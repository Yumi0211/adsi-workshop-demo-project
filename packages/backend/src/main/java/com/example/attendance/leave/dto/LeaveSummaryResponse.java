package com.example.attendance.leave.dto;

import com.example.attendance.leave.entity.LeaveBalance;

import java.math.BigDecimal;
import java.util.UUID;

public record LeaveSummaryResponse(
    UUID employeeId,
    String employeeName,
    String departmentName,
    BigDecimal grantedDays,
    BigDecimal usedDays,
    BigDecimal remainingDays
) {
    public static LeaveSummaryResponse from(LeaveBalance balance) {
        return new LeaveSummaryResponse(
            balance.getEmployee().getId(),
            balance.getEmployee().getName(),
            balance.getEmployee().getDepartment().getName(),
            balance.getGrantedDays(),
            balance.getUsedDays(),
            balance.getRemainingDays()
        );
    }
}
