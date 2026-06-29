package com.github.mwacha.wachafit.goal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateGoalRequest(
        String description,
        String metric,
        BigDecimal targetValue,
        LocalDate targetDate
) {}
