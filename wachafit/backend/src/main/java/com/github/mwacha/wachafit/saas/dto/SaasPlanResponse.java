package com.github.mwacha.wachafit.saas.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SaasPlanResponse(
    UUID id, String name, String description, BigDecimal price,
    int billingPeriodMonths, Integer maxUsers, boolean active, Instant createdAt
) {}
