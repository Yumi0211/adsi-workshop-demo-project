package com.example.attendance.leave.dto;

import com.example.attendance.leave.entity.LeaveType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record LeaveCreateRequest(
    @NotNull LocalDate leaveDate,
    @NotNull LeaveType leaveType,
    @Size(max = 500) String reason
) {}
