package com.example.attendance.leave.service;

import com.example.attendance.department.entity.Department;
import com.example.attendance.employee.entity.Employee;
import com.example.attendance.employee.entity.Role;
import com.example.attendance.employee.repository.EmployeeRepository;
import com.example.attendance.leave.dto.LeaveCreateRequest;
import com.example.attendance.leave.entity.LeaveBalance;
import com.example.attendance.leave.entity.LeaveRequest;
import com.example.attendance.leave.entity.LeaveStatus;
import com.example.attendance.leave.entity.LeaveType;
import com.example.attendance.leave.repository.LeaveBalanceRepository;
import com.example.attendance.leave.repository.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private LeaveGrantCalculator leaveGrantCalculator;

    private LeaveServiceImpl service;

    private Department department;
    private Employee employee;
    private Employee manager;
    private LeaveBalance balance;

    @BeforeEach
    void setUp() {
        service = new LeaveServiceImpl(
                leaveRequestRepository, leaveBalanceRepository,
                employeeRepository, leaveGrantCalculator);

        department = Department.builder()
                .id(UUID.randomUUID())
                .name("開発部")
                .build();

        employee = Employee.builder()
                .id(UUID.randomUUID())
                .name("田中太郎")
                .email("tanaka@example.com")
                .department(department)
                .role(Role.EMPLOYEE)
                .isManager(false)
                .hireDate(LocalDate.of(2024, 4, 1))
                .build();

        manager = Employee.builder()
                .id(UUID.randomUUID())
                .name("鈴木一郎")
                .email("suzuki@example.com")
                .department(department)
                .role(Role.EMPLOYEE)
                .isManager(true)
                .hireDate(LocalDate.of(2020, 4, 1))
                .build();

        balance = LeaveBalance.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .fiscalYear(2026)
                .grantedDays(new BigDecimal("10.0"))
                .usedDays(new BigDecimal("2.0"))
                .version(0L)
                .build();
    }

    @Nested
    @DisplayName("申請")
    class Create {

        @Test
        @DisplayName("残日数が十分なとき全日休を申請できる")
        void create_sufficientBalance_success() {
            var request = new LeaveCreateRequest(
                    LocalDate.of(2026, 7, 20), LeaveType.FULL, "私用");

            when(employeeRepository.findById(employee.getId()))
                    .thenReturn(Optional.of(employee));
            when(leaveBalanceRepository.findByEmployeeIdAndFiscalYear(employee.getId(), 2026))
                    .thenReturn(Optional.of(balance));
            when(leaveRequestRepository.existsByRequesterIdAndLeaveDateAndLeaveTypeAndStatusIn(
                    any(), any(), any(), any())).thenReturn(false);
            when(leaveRequestRepository.save(any(LeaveRequest.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            var result = service.create(employee.getId(), request);

            assertThat(result.leaveDate()).isEqualTo(LocalDate.of(2026, 7, 20));
            assertThat(result.leaveType()).isEqualTo(LeaveType.FULL);
            assertThat(result.status()).isEqualTo(LeaveStatus.PENDING);
        }

        @Test
        @DisplayName("残日数0で申請するとエラー")
        void create_zeroBalance_throwsError() {
            balance.setUsedDays(new BigDecimal("10.0"));
            var request = new LeaveCreateRequest(
                    LocalDate.of(2026, 7, 20), LeaveType.FULL, null);

            when(employeeRepository.findById(employee.getId()))
                    .thenReturn(Optional.of(employee));
            when(leaveBalanceRepository.findByEmployeeIdAndFiscalYear(employee.getId(), 2026))
                    .thenReturn(Optional.of(balance));

            assertThatThrownBy(() -> service.create(employee.getId(), request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("残日数");
        }

        @Test
        @DisplayName("残0.5日で全日休を申請するとエラー")
        void create_insufficientBalanceForFull_throwsError() {
            balance.setUsedDays(new BigDecimal("9.5"));
            var request = new LeaveCreateRequest(
                    LocalDate.of(2026, 7, 20), LeaveType.FULL, null);

            when(employeeRepository.findById(employee.getId()))
                    .thenReturn(Optional.of(employee));
            when(leaveBalanceRepository.findByEmployeeIdAndFiscalYear(employee.getId(), 2026))
                    .thenReturn(Optional.of(balance));

            assertThatThrownBy(() -> service.create(employee.getId(), request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("残日数");
        }

        @Test
        @DisplayName("残0.5日で半日休を申請できる")
        void create_halfDayWithHalfBalance_success() {
            balance.setUsedDays(new BigDecimal("9.5"));
            var request = new LeaveCreateRequest(
                    LocalDate.of(2026, 7, 20), LeaveType.AM, null);

            when(employeeRepository.findById(employee.getId()))
                    .thenReturn(Optional.of(employee));
            when(leaveBalanceRepository.findByEmployeeIdAndFiscalYear(employee.getId(), 2026))
                    .thenReturn(Optional.of(balance));
            when(leaveRequestRepository.existsByRequesterIdAndLeaveDateAndLeaveTypeAndStatusIn(
                    any(), any(), any(), any())).thenReturn(false);
            when(leaveRequestRepository.save(any(LeaveRequest.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            var result = service.create(employee.getId(), request);

            assertThat(result.leaveType()).isEqualTo(LeaveType.AM);
            assertThat(result.status()).isEqualTo(LeaveStatus.PENDING);
        }

        @Test
        @DisplayName("同日同種別の重複申請はエラー")
        void create_duplicate_throwsError() {
            var request = new LeaveCreateRequest(
                    LocalDate.of(2026, 7, 20), LeaveType.FULL, null);

            when(employeeRepository.findById(employee.getId()))
                    .thenReturn(Optional.of(employee));
            when(leaveBalanceRepository.findByEmployeeIdAndFiscalYear(employee.getId(), 2026))
                    .thenReturn(Optional.of(balance));
            when(leaveRequestRepository.existsByRequesterIdAndLeaveDateAndLeaveTypeAndStatusIn(
                    eq(employee.getId()), eq(LocalDate.of(2026, 7, 20)), eq(LeaveType.FULL), any()))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.create(employee.getId(), request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("重複");
        }
    }

    @Nested
    @DisplayName("承認")
    class Approve {

        @Test
        @DisplayName("承認するとusedDaysが加算される（全日: +1.0）")
        void approve_fullDay_incrementsUsedDays() {
            var leaveRequest = LeaveRequest.builder()
                    .id(UUID.randomUUID())
                    .requester(employee)
                    .leaveDate(LocalDate.of(2026, 7, 20))
                    .leaveType(LeaveType.FULL)
                    .status(LeaveStatus.PENDING)
                    .version(0L)
                    .build();

            when(leaveRequestRepository.findById(leaveRequest.getId()))
                    .thenReturn(Optional.of(leaveRequest));
            when(employeeRepository.findById(manager.getId()))
                    .thenReturn(Optional.of(manager));
            when(leaveBalanceRepository.findByEmployeeIdAndFiscalYear(employee.getId(), 2026))
                    .thenReturn(Optional.of(balance));
            when(leaveRequestRepository.save(any(LeaveRequest.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.approve(leaveRequest.getId(), manager.getId(), 0L);

            assertThat(balance.getUsedDays()).isEqualByComparingTo(new BigDecimal("3.0"));
        }

        @Test
        @DisplayName("承認するとusedDaysが加算される（半日: +0.5）")
        void approve_halfDay_incrementsUsedDaysBy05() {
            var leaveRequest = LeaveRequest.builder()
                    .id(UUID.randomUUID())
                    .requester(employee)
                    .leaveDate(LocalDate.of(2026, 7, 20))
                    .leaveType(LeaveType.AM)
                    .status(LeaveStatus.PENDING)
                    .version(0L)
                    .build();

            when(leaveRequestRepository.findById(leaveRequest.getId()))
                    .thenReturn(Optional.of(leaveRequest));
            when(employeeRepository.findById(manager.getId()))
                    .thenReturn(Optional.of(manager));
            when(leaveBalanceRepository.findByEmployeeIdAndFiscalYear(employee.getId(), 2026))
                    .thenReturn(Optional.of(balance));
            when(leaveRequestRepository.save(any(LeaveRequest.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.approve(leaveRequest.getId(), manager.getId(), 0L);

            assertThat(balance.getUsedDays()).isEqualByComparingTo(new BigDecimal("2.5"));
        }
    }

    @Nested
    @DisplayName("却下")
    class Reject {

        @Test
        @DisplayName("却下してもusedDaysは変動しない")
        void reject_doesNotChangeUsedDays() {
            var leaveRequest = LeaveRequest.builder()
                    .id(UUID.randomUUID())
                    .requester(employee)
                    .leaveDate(LocalDate.of(2026, 7, 20))
                    .leaveType(LeaveType.FULL)
                    .status(LeaveStatus.PENDING)
                    .version(0L)
                    .build();

            when(leaveRequestRepository.findById(leaveRequest.getId()))
                    .thenReturn(Optional.of(leaveRequest));
            when(employeeRepository.findById(manager.getId()))
                    .thenReturn(Optional.of(manager));
            when(leaveRequestRepository.save(any(LeaveRequest.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.reject(leaveRequest.getId(), manager.getId(), "業務都合", 0L);

            assertThat(balance.getUsedDays()).isEqualByComparingTo(new BigDecimal("2.0"));
        }
    }
}
