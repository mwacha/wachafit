package com.github.mwacha.wachafit.profile.dto;

import java.time.LocalDate;
import java.util.UUID;

public record StudentHealthResponse(
    UUID id, UUID userId, boolean hasHeartCondition, boolean hasDiabetes,
    boolean hasHypertension, String medications, String physicalRestrictions,
    LocalDate parqSignedAt, String notes
) {}
