package com.github.mwacha.wachafit.membership.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PlanResponse(
    UUID id,
    String name,
    String description,
    int durationMonths,
    BigDecimal price,
    Integer maxClassesPerWeek,
    boolean active,
    Instant createdAt
) {}
