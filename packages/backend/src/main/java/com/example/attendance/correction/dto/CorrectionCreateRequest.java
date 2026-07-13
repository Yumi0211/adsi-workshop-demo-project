package com.example.attendance.correction.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CorrectionCreateRequest(
    UUID attendanceRecordId,
    @NotNull LocalDate targetDate,
    @NotNull Instant correctedClockIn,
    @NotNull Instant correctedClockOut,
    @NotNull @Size(min = 1, max = 500) String reason,
    @Size(max = 50) String correctedMemo
) {}
