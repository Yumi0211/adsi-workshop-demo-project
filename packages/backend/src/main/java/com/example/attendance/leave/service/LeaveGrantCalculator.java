package com.example.attendance.leave.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.TreeMap;

@Component
public class LeaveGrantCalculator {

    private static final TreeMap<Long, BigDecimal> GRANT_TABLE = new TreeMap<>(Map.of(
        6L,  new BigDecimal("10"),
        18L, new BigDecimal("11"),
        30L, new BigDecimal("12"),
        42L, new BigDecimal("14"),
        54L, new BigDecimal("16"),
        66L, new BigDecimal("18"),
        78L, new BigDecimal("20")
    ));

    private static final BigDecimal TWELVE = new BigDecimal("12");

    public BigDecimal calculate(LocalDate hireDate, int fiscalYear) {
        var fiscalStart = LocalDate.of(fiscalYear, 4, 1);
        var fiscalEnd = LocalDate.of(fiscalYear + 1, 3, 31);

        var sixMonthsAfterHire = hireDate.plusMonths(6);

        if (sixMonthsAfterHire.isAfter(fiscalEnd)) {
            return BigDecimal.ZERO;
        }

        var monthsOfService = ChronoUnit.MONTHS.between(hireDate, fiscalStart);

        if (monthsOfService >= 6) {
            var entry = GRANT_TABLE.floorEntry(monthsOfService);
            return entry != null ? entry.getValue() : BigDecimal.ZERO;
        }

        var remainingMonths = ChronoUnit.MONTHS.between(sixMonthsAfterHire, fiscalEnd.plusDays(1));
        if (remainingMonths <= 0) {
            return BigDecimal.ZERO;
        }

        var fullGrant = new BigDecimal("10");
        return fullGrant
            .multiply(new BigDecimal(remainingMonths))
            .divide(TWELVE, 0, RoundingMode.CEILING);
    }
}
