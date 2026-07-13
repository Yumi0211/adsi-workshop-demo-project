package com.example.attendance.leave.dto;

import com.example.attendance.leave.entity.LeaveRequest;
import com.example.attendance.leave.entity.LeaveStatus;
import com.example.attendance.leave.entity.LeaveType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PendingLeaveResponse(
    UUID id,
    UUID requesterId,
    String requesterName,
    LocalDate leaveDate,
    LeaveType leaveType,
    String reason,
    LeaveStatus status,
    Long version,
    Instant createdAt
) {
    public static PendingLeaveResponse from(LeaveRequest request) {
        return new PendingLeaveResponse(
            request.getId(),
            request.getRequester().getId(),
            request.getRequester().getName(),
            request.getLeaveDate(),
            request.getLeaveType(),
            request.getReason(),
            request.getStatus(),
            request.getVersion(),
            request.getCreatedAt()
        );
    }
}
