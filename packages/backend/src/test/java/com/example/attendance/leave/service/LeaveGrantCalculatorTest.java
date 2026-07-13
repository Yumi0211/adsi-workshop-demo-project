package com.example.attendance.leave.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class LeaveGrantCalculatorTest {

    private final LeaveGrantCalculator calculator = new LeaveGrantCalculator();

    @Test
    @DisplayName("勤続0.5年（6ヶ月）で10日付与される")
    void calculate_halfYear_returns10() {
        var hireDate = LocalDate.of(2025, 10, 1);
        var fiscalYear = 2026;

        var result = calculator.calculate(hireDate, fiscalYear);

        assertThat(result).isEqualByComparingTo(BigDecimal.TEN);
    }

    @Test
    @DisplayName("勤続1.5年で11日付与される")
    void calculate_oneAndHalfYears_returns11() {
        var hireDate = LocalDate.of(2024, 10, 1);
        var fiscalYear = 2026;

        var result = calculator.calculate(hireDate, fiscalYear);

        assertThat(result).isEqualByComparingTo(new BigDecimal("11"));
    }

    @Test
    @DisplayName("勤続2.5年で12日付与される")
    void calculate_twoAndHalfYears_returns12() {
        var hireDate = LocalDate.of(2023, 10, 1);
        var fiscalYear = 2026;

        var result = calculator.calculate(hireDate, fiscalYear);

        assertThat(result).isEqualByComparingTo(new BigDecimal("12"));
    }

    @Test
    @DisplayName("勤続3.5年で14日付与される")
    void calculate_threeAndHalfYears_returns14() {
        var hireDate = LocalDate.of(2022, 10, 1);
        var fiscalYear = 2026;

        var result = calculator.calculate(hireDate, fiscalYear);

        assertThat(result).isEqualByComparingTo(new BigDecimal("14"));
    }

    @Test
    @DisplayName("勤続6.5年以上で20日付与される")
    void calculate_sixAndHalfYearsOrMore_returns20() {
        var hireDate = LocalDate.of(2019, 10, 1);
        var fiscalYear = 2026;

        var result = calculator.calculate(hireDate, fiscalYear);

        assertThat(result).isEqualByComparingTo(new BigDecimal("20"));
    }

    @Test
    @DisplayName("入社6ヶ月未満（年度末までに6ヶ月到達しない）は0日")
    void calculate_lessThanHalfYear_returnsZero() {
        var hireDate = LocalDate.of(2026, 11, 1);
        var fiscalYear = 2026;

        var result = calculator.calculate(hireDate, fiscalYear);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("初年度按分: 4月入社→10月に6ヶ月到達→残6ヶ月→5日")
    void calculate_firstYearProrated_april() {
        var hireDate = LocalDate.of(2026, 4, 1);
        var fiscalYear = 2026;

        var result = calculator.calculate(hireDate, fiscalYear);

        assertThat(result).isEqualByComparingTo(new BigDecimal("5"));
    }

    @Test
    @DisplayName("初年度按分: 7月入社→1月に6ヶ月到達→残3ヶ月→3日（端数切り上げ）")
    void calculate_firstYearProrated_july() {
        var hireDate = LocalDate.of(2026, 7, 1);
        var fiscalYear = 2026;

        var result = calculator.calculate(hireDate, fiscalYear);

        assertThat(result).isEqualByComparingTo(new BigDecimal("3"));
    }

    @Test
    @DisplayName("10月入社→翌年4月に6ヶ月到達→当年度は0日、翌年度から満額")
    void calculate_octoberHire_noGrantInCurrentFiscalYear() {
        var hireDate = LocalDate.of(2025, 10, 1);
        var fiscalYear = 2025;

        var result = calculator.calculate(hireDate, fiscalYear);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("10月入社→翌年度（4月〜）に満額10日")
    void calculate_octoberHire_fullGrantNextFiscalYear() {
        var hireDate = LocalDate.of(2025, 10, 1);
        var fiscalYear = 2026;

        var result = calculator.calculate(hireDate, fiscalYear);

        assertThat(result).isEqualByComparingTo(BigDecimal.TEN);
    }
}
