package com.github.mwacha.wachafit.membership.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreatePlanRequest(
    @NotBlank String name,
    String description,
    @Positive int durationMonths,
    @DecimalMin("0.01") BigDecimal price,
    Integer maxClassesPerWeek
) {}
