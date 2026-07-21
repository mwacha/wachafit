package com.github.mwacha.wachafit.membership.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionResponse(
    UUID id,
    UUID studentId,
    UUID planId,
    String planName,
    String status,
    LocalDate startedAt,
    LocalDate expiresAt,
    Instant createdAt
) {}
