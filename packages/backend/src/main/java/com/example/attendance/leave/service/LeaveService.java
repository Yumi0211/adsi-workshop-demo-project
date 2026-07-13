package com.example.attendance.leave.service;

import com.example.attendance.leave.dto.LeaveBalanceResponse;
import com.example.attendance.leave.dto.LeaveCreateRequest;
import com.example.attendance.leave.dto.LeaveResponse;
import com.example.attendance.leave.dto.LeaveSummaryResponse;
import com.example.attendance.leave.dto.PendingLeaveResponse;
import com.example.attendance.leave.entity.LeaveStatus;

import java.util.List;
import java.util.UUID;

public interface LeaveService {

    LeaveResponse create(UUID requesterId, LeaveCreateRequest request);

    List<LeaveResponse> findByRequester(UUID requesterId, LeaveStatus status, Integer fiscalYear);

    LeaveBalanceResponse getBalance(UUID employeeId, Integer fiscalYear);

    List<PendingLeaveResponse> findPending(UUID managerId);

    LeaveResponse approve(UUID leaveId, UUID approverId, Long version);

    LeaveResponse reject(UUID leaveId, UUID approverId, String rejectReason, Long version);

    List<LeaveSummaryResponse> getSummary(Integer fiscalYear, UUID departmentId);
}
