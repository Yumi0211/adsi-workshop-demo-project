package com.example.attendance.leave.dto;

import com.example.attendance.leave.entity.LeaveRequest;
import com.example.attendance.leave.entity.LeaveStatus;
import com.example.attendance.leave.entity.LeaveType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LeaveResponse(
    UUID id,
    LocalDate leaveDate,
    LeaveType leaveType,
    String reason,
    LeaveStatus status,
    String approverName,
    String rejectReason,
    Long version,
    Instant createdAt
) {
    public static LeaveResponse from(LeaveRequest request) {
        return new LeaveResponse(
            request.getId(),
            request.getLeaveDate(),
            request.getLeaveType(),
            request.getReason(),
            request.getStatus(),
            request.getApprover() != null ? request.getApprover().getName() : null,
            request.getRejectReason(),
            request.getVersion(),
            request.getCreatedAt()
        );
    }
}
