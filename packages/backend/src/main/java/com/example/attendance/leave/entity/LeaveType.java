package com.example.attendance.leave.entity;

import java.math.BigDecimal;

public enum LeaveType {
    FULL(BigDecimal.ONE),
    AM(new BigDecimal("0.5")),
    PM(new BigDecimal("0.5"));

    private final BigDecimal days;

    LeaveType(BigDecimal days) {
        this.days = days;
    }

    public BigDecimal getDays() {
        return days;
    }
}
