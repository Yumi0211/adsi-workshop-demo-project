package com.example.attendance.correction.service;

import com.example.attendance.attendance.entity.AttendanceRecord;
import com.example.attendance.attendance.repository.AttendanceRecordRepository;
import com.example.attendance.correction.dto.CorrectionCreateRequest;
import com.example.attendance.correction.entity.AttendanceCorrection;
import com.example.attendance.correction.entity.CorrectionStatus;
import com.example.attendance.correction.repository.AttendanceCorrectionRepository;
import com.example.attendance.department.entity.Department;
import com.example.attendance.employee.entity.Employee;
import com.example.attendance.employee.entity.Role;
import com.example.attendance.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorrectionServiceTest {

    @Mock
    private AttendanceCorrectionRepository correctionRepository;

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    private CorrectionServiceImpl service;

    private Department department;
    private Employee employee;
    private Employee manager;

    @BeforeEach
    void setUp() {
        service = new CorrectionServiceImpl(
                correctionRepository, attendanceRecordRepository, employeeRepository);

        department = Department.builder()
                .id(UUID.randomUUID())
                .name("Engineering")
                .build();

        employee = Employee.builder()
                .id(UUID.randomUUID())
                .name("田中太郎")
                .email("tanaka@example.com")
                .password("hashed")
                .department(department)
                .role(Role.EMPLOYEE)
                .isManager(false)
                .hireDate(LocalDate.of(2024, 4, 1))
                .build();

        manager = Employee.builder()
                .id(UUID.randomUUID())
                .name("佐藤次郎")
                .email("sato@example.com")
                .password("hashed")
                .department(department)
                .role(Role.EMPLOYEE)
                .isManager(true)
                .hireDate(LocalDate.of(2020, 4, 1))
                .build();
    }

    @Nested
    @DisplayName("修正申請作成")
    class Create {

        @Test
        @DisplayName("既存レコードの修正申請ができる")
        void create_existingRecord_createsCorrection() {
            // Arrange
            var recordId = UUID.randomUUID();
            var record = AttendanceRecord.builder()
                    .id(recordId)
                    .employee(employee)
                    .workDate(LocalDate.of(2025, 1, 15))
                    .clockIn(Instant.parse("2025-01-14T23:00:00Z"))
                    .clockOut(Instant.parse("2025-01-15T08:00:00Z"))
                    .build();
            var request = new CorrectionCreateRequest(
                    recordId,
                    LocalDate.of(2025, 1, 15),
                    Instant.parse("2025-01-14T23:30:00Z"),
                    Instant.parse("2025-01-15T09:00:00Z"),
                    "打刻時刻を間違えました",
                    null
            );

            when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
            when(attendanceRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
            when(correctionRepository.save(any(AttendanceCorrection.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            var result = service.create(employee.getId(), request);

            // Assert
            assertThat(result.status()).isEqualTo(CorrectionStatus.PENDING);
            assertThat(result.reason()).isEqualTo("打刻時刻を間違えました");
            assertThat(result.attendanceRecordId()).isEqualTo(recordId);

            var captor = ArgumentCaptor.forClass(AttendanceCorrection.class);
            verify(correctionRepository).save(captor.capture());
            assertThat(captor.getValue().getAttendanceRecord().getId()).isEqualTo(recordId);
        }

        @Test
        @DisplayName("打刻忘れの修正申請ができる（attendanceRecordId = null）")
        void create_missingPunch_createsCorrection() {
            // Arrange
            var request = new CorrectionCreateRequest(
                    null,
                    LocalDate.of(2025, 1, 15),
                    Instant.parse("2025-01-14T23:00:00Z"),
                    Instant.parse("2025-01-15T08:00:00Z"),
                    "打刻忘れ",
                    null
            );

            when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
            when(correctionRepository.save(any(AttendanceCorrection.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            var result = service.create(employee.getId(), request);

            // Assert
            assertThat(result.status()).isEqualTo(CorrectionStatus.PENDING);
            assertThat(result.attendanceRecordId()).isNull();
        }
    }

    @Nested
    @DisplayName("自分の申請一覧")
    class FindByRequester {

        @Test
        @DisplayName("ログインユーザーの申請のみ返す")
        void findByRequester_returnsOwnCorrections() {
            // Arrange
            var correction = AttendanceCorrection.builder()
                    .id(UUID.randomUUID())
                    .requester(employee)
                    .targetDate(LocalDate.of(2025, 1, 15))
                    .correctedClockIn(Instant.parse("2025-01-14T23:00:00Z"))
                    .correctedClockOut(Instant.parse("2025-01-15T08:00:00Z"))
                    .reason("修正理由")
                    .status(CorrectionStatus.PENDING)
                    .build();
            when(correctionRepository.findByRequesterIdOrderByCreatedAtDesc(employee.getId()))
                    .thenReturn(List.of(correction));

            // Act
            var results = service.findByRequester(employee.getId(), null);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.get(0).requesterId()).isEqualTo(employee.getId());
        }

        @Test
        @DisplayName("ステータスフィルタでPENDINGのみ返す")
        void findByRequester_withStatusFilter_returnsPendingOnly() {
            // Arrange
            var correction = AttendanceCorrection.builder()
                    .id(UUID.randomUUID())
                    .requester(employee)
                    .targetDate(LocalDate.of(2025, 1, 15))
                    .correctedClockIn(Instant.parse("2025-01-14T23:00:00Z"))
                    .correctedClockOut(Instant.parse("2025-01-15T08:00:00Z"))
                    .reason("修正理由")
                    .status(CorrectionStatus.PENDING)
                    .build();
            when(correctionRepository.findByRequesterIdAndStatusOrderByCreatedAtDesc(
                    employee.getId(), CorrectionStatus.PENDING))
                    .thenReturn(List.of(correction));

            // Act
            var results = service.findByRequester(employee.getId(), CorrectionStatus.PENDING);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.get(0).status()).isEqualTo(CorrectionStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("承認待ち一覧")
    class FindPending {

        @Test
        @DisplayName("自部署メンバーのPENDINGのみ返す")
        void findPending_returnsOwnDepartmentPending() {
            // Arrange
            var correction = AttendanceCorrection.builder()
                    .id(UUID.randomUUID())
                    .requester(employee)
                    .targetDate(LocalDate.of(2025, 1, 15))
                    .correctedClockIn(Instant.parse("2025-01-14T23:00:00Z"))
                    .correctedClockOut(Instant.parse("2025-01-15T08:00:00Z"))
                    .reason("修正理由")
                    .status(CorrectionStatus.PENDING)
                    .build();
            when(employeeRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
            when(correctionRepository.findByRequesterDepartmentIdAndStatusOrderByCreatedAtDesc(
                    department.getId(), CorrectionStatus.PENDING))
                    .thenReturn(List.of(correction));

            // Act
            var results = service.findPending(manager.getId());

            // Assert
            assertThat(results).hasSize(1);
        }
    }

    @Nested
    @DisplayName("承認")
    class Approve {

        @Test
        @DisplayName("承認すると既存レコードのclockIn/clockOutが更新されcorrected=trueになる")
        void approve_existingRecord_updatesAttendanceRecord() {
            // Arrange
            var record = AttendanceRecord.builder()
                    .id(UUID.randomUUID())
                    .employee(employee)
                    .workDate(LocalDate.of(2025, 1, 15))
                    .clockIn(Instant.parse("2025-01-14T23:00:00Z"))
                    .clockOut(Instant.parse("2025-01-15T08:00:00Z"))
                    .corrected(false)
                    .build();
            var correction = AttendanceCorrection.builder()
                    .id(UUID.randomUUID())
                    .attendanceRecord(record)
                    .requester(employee)
                    .targetDate(LocalDate.of(2025, 1, 15))
                    .correctedClockIn(Instant.parse("2025-01-14T23:30:00Z"))
                    .correctedClockOut(Instant.parse("2025-01-15T09:00:00Z"))
                    .reason("修正理由")
                    .status(CorrectionStatus.PENDING)
                    .version(0L)
                    .build();

            when(correctionRepository.findById(correction.getId()))
                    .thenReturn(Optional.of(correction));
            when(employeeRepository.findById(manager.getId()))
                    .thenReturn(Optional.of(manager));
            when(correctionRepository.save(any(AttendanceCorrection.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(attendanceRecordRepository.save(any(AttendanceRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            var result = service.approve(correction.getId(), manager.getId(), 0L);

            // Assert
            assertThat(result.status()).isEqualTo(CorrectionStatus.APPROVED);
            assertThat(record.getClockIn()).isEqualTo(Instant.parse("2025-01-14T23:30:00Z"));
            assertThat(record.getClockOut()).isEqualTo(Instant.parse("2025-01-15T09:00:00Z"));
            assertThat(record.isCorrected()).isTrue();
        }

        @Test
        @DisplayName("打刻忘れの承認時に新規AttendanceRecordが作成される")
        void approve_missingPunch_createsNewAttendanceRecord() {
            // Arrange
            var correction = AttendanceCorrection.builder()
                    .id(UUID.randomUUID())
                    .attendanceRecord(null)
                    .requester(employee)
                    .targetDate(LocalDate.of(2025, 1, 15))
                    .correctedClockIn(Instant.parse("2025-01-14T23:00:00Z"))
                    .correctedClockOut(Instant.parse("2025-01-15T08:00:00Z"))
                    .reason("打刻忘れ")
                    .status(CorrectionStatus.PENDING)
                    .version(0L)
                    .build();

            when(correctionRepository.findById(correction.getId()))
                    .thenReturn(Optional.of(correction));
            when(employeeRepository.findById(manager.getId()))
                    .thenReturn(Optional.of(manager));
            when(correctionRepository.save(any(AttendanceCorrection.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(attendanceRecordRepository.save(any(AttendanceRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            var result = service.approve(correction.getId(), manager.getId(), 0L);

            // Assert
            assertThat(result.status()).isEqualTo(CorrectionStatus.APPROVED);

            var captor = ArgumentCaptor.forClass(AttendanceRecord.class);
            verify(attendanceRecordRepository).save(captor.capture());
            var newRecord = captor.getValue();
            assertThat(newRecord.getWorkDate()).isEqualTo(LocalDate.of(2025, 1, 15));
            assertThat(newRecord.isCorrected()).isTrue();
        }

        @Test
        @DisplayName("承認者が部署の上長でない場合は403エラー")
        void approve_notManager_throwsForbidden() {
            // Arrange
            var otherDepartment = Department.builder()
                    .id(UUID.randomUUID())
                    .name("Sales")
                    .build();
            var otherManager = Employee.builder()
                    .id(UUID.randomUUID())
                    .name("鈴木三郎")
                    .email("suzuki@example.com")
                    .password("hashed")
                    .department(otherDepartment)
                    .role(Role.EMPLOYEE)
                    .isManager(true)
                    .hireDate(LocalDate.of(2020, 4, 1))
                    .build();
            var correction = AttendanceCorrection.builder()
                    .id(UUID.randomUUID())
                    .requester(employee)
                    .targetDate(LocalDate.of(2025, 1, 15))
                    .correctedClockIn(Instant.parse("2025-01-14T23:00:00Z"))
                    .correctedClockOut(Instant.parse("2025-01-15T08:00:00Z"))
                    .reason("修正理由")
                    .status(CorrectionStatus.PENDING)
                    .version(0L)
                    .build();

            when(correctionRepository.findById(correction.getId()))
                    .thenReturn(Optional.of(correction));
            when(employeeRepository.findById(otherManager.getId()))
                    .thenReturn(Optional.of(otherManager));

            // Act & Assert
            assertThatThrownBy(() ->
                    service.approve(correction.getId(), otherManager.getId(), 0L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("403");
        }

        @Test
        @DisplayName("上長の自己承認が正常に動作する")
        void approve_selfApproval_succeeds() {
            // Arrange
            var correction = AttendanceCorrection.builder()
                    .id(UUID.randomUUID())
                    .attendanceRecord(null)
                    .requester(manager)
                    .targetDate(LocalDate.of(2025, 1, 15))
                    .correctedClockIn(Instant.parse("2025-01-14T23:00:00Z"))
                    .correctedClockOut(Instant.parse("2025-01-15T08:00:00Z"))
                    .reason("上長自身の修正")
                    .status(CorrectionStatus.PENDING)
                    .version(0L)
                    .build();

            when(correctionRepository.findById(correction.getId()))
                    .thenReturn(Optional.of(correction));
            when(employeeRepository.findById(manager.getId()))
                    .thenReturn(Optional.of(manager));
            when(correctionRepository.save(any(AttendanceCorrection.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(attendanceRecordRepository.save(any(AttendanceRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            var result = service.approve(correction.getId(), manager.getId(), 0L);

            // Assert
            assertThat(result.status()).isEqualTo(CorrectionStatus.APPROVED);
            assertThat(result.approverId()).isEqualTo(manager.getId());
        }
    }

    @Nested
    @DisplayName("却下")
    class Reject {

        @Test
        @DisplayName("却下するとステータスがREJECTEDになり理由が保存される")
        void reject_setsStatusAndReason() {
            // Arrange
            var correction = AttendanceCorrection.builder()
                    .id(UUID.randomUUID())
                    .requester(employee)
                    .targetDate(LocalDate.of(2025, 1, 15))
                    .correctedClockIn(Instant.parse("2025-01-14T23:00:00Z"))
                    .correctedClockOut(Instant.parse("2025-01-15T08:00:00Z"))
                    .reason("修正理由")
                    .status(CorrectionStatus.PENDING)
                    .version(0L)
                    .build();

            when(correctionRepository.findById(correction.getId()))
                    .thenReturn(Optional.of(correction));
            when(employeeRepository.findById(manager.getId()))
                    .thenReturn(Optional.of(manager));
            when(correctionRepository.save(any(AttendanceCorrection.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            var result = service.reject(
                    correction.getId(), manager.getId(), "内容に不備があります", 0L);

            // Assert
            assertThat(result.status()).isEqualTo(CorrectionStatus.REJECTED);
            assertThat(result.rejectReason()).isEqualTo("内容に不備があります");
        }
    }

    @Nested
    @DisplayName("楽観ロック")
    class OptimisticLock {

        @Test
        @DisplayName("バージョン不一致時はコンフリクトエラー")
        void approve_versionMismatch_throwsConflict() {
            // Arrange
            var correction = AttendanceCorrection.builder()
                    .id(UUID.randomUUID())
                    .requester(employee)
                    .targetDate(LocalDate.of(2025, 1, 15))
                    .correctedClockIn(Instant.parse("2025-01-14T23:00:00Z"))
                    .correctedClockOut(Instant.parse("2025-01-15T08:00:00Z"))
                    .reason("修正理由")
                    .status(CorrectionStatus.PENDING)
                    .version(1L)
                    .build();

            when(correctionRepository.findById(correction.getId()))
                    .thenReturn(Optional.of(correction));
            when(employeeRepository.findById(manager.getId()))
                    .thenReturn(Optional.of(manager));

            // Act & Assert
            assertThatThrownBy(() ->
                    service.approve(correction.getId(), manager.getId(), 0L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("409");
        }
    }
}
