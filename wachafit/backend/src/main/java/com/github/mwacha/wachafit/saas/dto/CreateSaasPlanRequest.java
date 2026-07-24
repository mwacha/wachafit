package com.github.mwacha.wachafit.saas.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateSaasPlanRequest(
    @NotBlank String name,
    String description,
    @NotNull @DecimalMin("0.01") BigDecimal price,
    @Min(1) int billingPeriodMonths,
    Integer maxUsers
) {}
