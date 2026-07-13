package com.example.attendance.attendance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MemoUpdateRequest(
    @NotNull @Size(max = 50) String memo
) {
}
