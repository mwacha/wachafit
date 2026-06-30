package com.github.mwacha.wachafit.profile.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TrainerProfileResponse(
    UUID id, UUID userId, String cref, String specialties, String bio,
    String contractType, LocalDate admissionDate,
    String commissionType, BigDecimal commissionValue, Instant createdAt
) {}
