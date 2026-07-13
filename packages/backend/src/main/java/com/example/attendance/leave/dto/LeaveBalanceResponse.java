package com.example.attendance.leave.dto;

import com.example.attendance.leave.entity.LeaveBalance;

import java.math.BigDecimal;

public record LeaveBalanceResponse(
    int fiscalYear,
    BigDecimal grantedDays,
    BigDecimal usedDays,
    BigDecimal remainingDays
) {
    public static LeaveBalanceResponse from(LeaveBalance balance) {
        return new LeaveBalanceResponse(
            balance.getFiscalYear(),
            balance.getGrantedDays(),
            balance.getUsedDays(),
            balance.getRemainingDays()
        );
    }
}
