package com.github.mwacha.wachafit.groupclass.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record GroupClassRequest(
    @NotBlank String name,
    String description,
    @Min(1) int capacity,
    @Min(1) int durationMinutes,
    @NotNull UUID trainerId
) {}
