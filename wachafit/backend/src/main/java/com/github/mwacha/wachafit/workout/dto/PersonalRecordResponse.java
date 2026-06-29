package com.github.mwacha.wachafit.workout.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PersonalRecordResponse(
    UUID id,
    UUID studentId,
    UUID exerciseId,
    BigDecimal recordLoadKg,
    LocalDate achievedAt) {}
