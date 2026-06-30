package com.github.mwacha.wachafit.profile.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTrainerProfileRequest(
    String cref, String specialties, String bio,
    String contractType, LocalDate admissionDate,
    String commissionType, BigDecimal commissionValue
) {}
