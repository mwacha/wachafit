package com.github.mwacha.wachafit.goal.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateGoalRequest(
        @NotBlank String description,
        String metric,
        BigDecimal targetValue,
        LocalDate targetDate
) {}
