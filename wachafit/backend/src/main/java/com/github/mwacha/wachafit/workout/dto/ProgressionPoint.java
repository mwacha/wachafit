package com.github.mwacha.wachafit.workout.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProgressionPoint(
    LocalDate performedAt,
    BigDecimal loadKg,
    Integer reps) {}
