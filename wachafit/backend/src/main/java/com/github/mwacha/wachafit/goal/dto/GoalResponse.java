package com.github.mwacha.wachafit.goal.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record GoalResponse(
        UUID id,
        UUID studentId,
        UUID createdById,
        String description,
        String metric,
        BigDecimal targetValue,
        LocalDate targetDate,
        String status,
        Instant createdAt
) {}
