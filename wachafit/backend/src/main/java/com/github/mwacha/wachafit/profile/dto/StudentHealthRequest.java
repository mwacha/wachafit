package com.github.mwacha.wachafit.profile.dto;

import java.time.LocalDate;

public record StudentHealthRequest(
    boolean hasHeartCondition, boolean hasDiabetes, boolean hasHypertension,
    String medications, String physicalRestrictions, LocalDate parqSignedAt, String notes
) {}
