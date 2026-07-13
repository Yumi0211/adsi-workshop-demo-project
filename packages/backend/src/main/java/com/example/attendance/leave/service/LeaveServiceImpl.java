package com.example.attendance.leave.service;

import com.example.attendance.employee.entity.Employee;
import com.example.attendance.employee.repository.EmployeeRepository;
import com.example.attendance.leave.dto.LeaveBalanceResponse;
import com.example.attendance.leave.dto.LeaveCreateRequest;
import com.example.attendance.leave.dto.LeaveResponse;
import com.example.attendance.leave.dto.LeaveSummaryResponse;
import com.example.attendance.leave.dto.PendingLeaveResponse;
import com.example.attendance.leave.entity.LeaveBalance;
import com.example.attendance.leave.entity.LeaveRequest;
import com.example.attendance.leave.entity.LeaveStatus;
import com.example.attendance.leave.entity.LeaveType;
import com.example.attendance.leave.repository.LeaveBalanceRepository;
import com.example.attendance.leave.repository.LeaveRequestRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
public class LeaveServiceImpl implements LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveGrantCalculator leaveGrantCalculator;

    public LeaveServiceImpl(
            LeaveRequestRepository leaveRequestRepository,
            LeaveBalanceRepository leaveBalanceRepository,
            EmployeeRepository employeeRepository,
            LeaveGrantCalculator leaveGrantCalculator) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.employeeRepository = employeeRepository;
        this.leaveGrantCalculator = leaveGrantCalculator;
    }

    @Override
    @Transactional
    public LeaveResponse create(UUID requesterId, LeaveCreateRequest request) {
        var requester = findEmployeeOrThrow(requesterId);
        var fiscalYear = toFiscalYear(request.leaveDate());
        var balance = getOrCreateBalance(requester, fiscalYear);

        var requestedDays = request.leaveType().getDays();
        if (balance.getRemainingDays().compareTo(requestedDays) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "残日数が不足しています（残: " + balance.getRemainingDays() + "日）");
        }

        var duplicate = leaveRequestRepository.existsByRequesterIdAndLeaveDateAndLeaveTypeAndStatusIn(
                requesterId, request.leaveDate(), request.leaveType(),
                List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED));
        if (duplicate) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "同日・同種別の申請が既に存在します（重複）");
        }

        var leaveRequest = LeaveRequest.builder()
                .id(UuidCreator.getTimeOrderedEpoch())
                .requester(requester)
                .leaveDate(request.leaveDate())
                .leaveType(request.leaveType())
                .reason(request.reason())
                .status(LeaveStatus.PENDING)
                .build();

        var saved = leaveRequestRepository.save(leaveRequest);
        log.info("有給休暇申請: employeeId={}, date={}, type={}",
                requesterId, request.leaveDate(), request.leaveType());
        return LeaveResponse.from(saved);
    }

    @Override
    public List<LeaveResponse> findByRequester(UUID requesterId, LeaveStatus status, Integer fiscalYear) {
        List<LeaveRequest> requests;
        if (status != null) {
            requests = leaveRequestRepository.findByRequesterIdAndStatusOrderByCreatedAtDesc(requesterId, status);
        } else {
            requests = leaveRequestRepository.findByRequesterIdOrderByCreatedAtDesc(requesterId);
        }

        if (fiscalYear != null) {
            var start = LocalDate.of(fiscalYear, 4, 1);
            var end = LocalDate.of(fiscalYear + 1, 3, 31);
            requests = requests.stream()
                    .filter(r -> !r.getLeaveDate().isBefore(start) && !r.getLeaveDate().isAfter(end))
                    .toList();
        }

        return requests.stream().map(LeaveResponse::from).toList();
    }

    @Override
    public LeaveBalanceResponse getBalance(UUID employeeId, Integer fiscalYear) {
        var employee = findEmployeeOrThrow(employeeId);
        var year = fiscalYear != null ? fiscalYear : currentFiscalYear();
        var balance = getOrCreateBalance(employee, year);
        return LeaveBalanceResponse.from(balance);
    }

    @Override
    public List<PendingLeaveResponse> findPending(UUID managerId) {
        var mgr = findEmployeeOrThrow(managerId);
        var requests = leaveRequestRepository
                .findByRequesterDepartmentIdAndStatusOrderByCreatedAtDesc(
                        mgr.getDepartment().getId(), LeaveStatus.PENDING);
        return requests.stream()
                .filter(r -> !r.getRequester().getId().equals(managerId))
                .map(PendingLeaveResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public LeaveResponse approve(UUID leaveId, UUID approverId) {
        var leaveRequest = findLeaveRequestOrThrow(leaveId);
        var approver = findEmployeeOrThrow(approverId);

        validateApprover(approver, leaveRequest.getRequester());

        leaveRequest.setStatus(LeaveStatus.APPROVED);
        leaveRequest.setApprover(approver);

        var fiscalYear = toFiscalYear(leaveRequest.getLeaveDate());
        var balance = getOrCreateBalance(leaveRequest.getRequester(), fiscalYear);
        balance.setUsedDays(balance.getUsedDays().add(leaveRequest.getLeaveType().getDays()));
        leaveBalanceRepository.save(balance);

        var saved = leaveRequestRepository.save(leaveRequest);
        log.info("有給休暇承認: leaveId={}, approverId={}", leaveId, approverId);
        return LeaveResponse.from(saved);
    }

    @Override
    @Transactional
    public LeaveResponse reject(UUID leaveId, UUID approverId, String rejectReason, Long version) {
        var leaveRequest = findLeaveRequestOrThrow(leaveId);
        var approver = findEmployeeOrThrow(approverId);

        validateApprover(approver, leaveRequest.getRequester());
        validateVersion(leaveRequest, version);

        leaveRequest.setStatus(LeaveStatus.REJECTED);
        leaveRequest.setApprover(approver);
        leaveRequest.setRejectReason(rejectReason);

        var saved = leaveRequestRepository.save(leaveRequest);
        log.info("有給休暇却下: leaveId={}, approverId={}", leaveId, approverId);
        return LeaveResponse.from(saved);
    }

    @Override
    public List<LeaveSummaryResponse> getSummary(Integer fiscalYear, UUID departmentId) {
        var year = fiscalYear != null ? fiscalYear : currentFiscalYear();
        List<LeaveBalance> balances;
        if (departmentId != null) {
            balances = leaveBalanceRepository.findByEmployeeDepartmentIdAndFiscalYear(departmentId, year);
        } else {
            balances = leaveBalanceRepository.findByFiscalYear(year);
        }
        return balances.stream().map(LeaveSummaryResponse::from).toList();
    }

    private Employee findEmployeeOrThrow(UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "社員が見つかりません: " + employeeId));
    }

    private LeaveRequest findLeaveRequestOrThrow(UUID leaveId) {
        return leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "有給申請が見つかりません: " + leaveId));
    }

    private void validateApprover(Employee approver, Employee requester) {
        if (!approver.getDepartment().getId().equals(requester.getDepartment().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "他部署の申請は承認できません");
        }
        if (!approver.isManager()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "上長のみ承認できます");
        }
    }

    private void validateVersion(LeaveRequest leaveRequest, Long version) {
        if (!leaveRequest.getVersion().equals(version)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "データが更新されています。再読み込みしてください");
        }
    }

    private LeaveBalance getOrCreateBalance(Employee employee, int fiscalYear) {
        return leaveBalanceRepository.findByEmployeeIdAndFiscalYear(employee.getId(), fiscalYear)
                .orElseGet(() -> {
                    var granted = leaveGrantCalculator.calculate(employee.getHireDate(), fiscalYear);
                    var balance = LeaveBalance.builder()
                            .id(UuidCreator.getTimeOrderedEpoch())
                            .employee(employee)
                            .fiscalYear(fiscalYear)
                            .grantedDays(granted)
                            .usedDays(BigDecimal.ZERO)
                            .build();
                    return leaveBalanceRepository.save(balance);
                });
    }

    private int toFiscalYear(LocalDate date) {
        return date.getMonthValue() >= 4 ? date.getYear() : date.getYear() - 1;
    }

    private int currentFiscalYear() {
        return toFiscalYear(LocalDate.now());
    }
}
