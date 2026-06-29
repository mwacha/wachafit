package com.github.mwacha.wachafit.workout.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WorkoutPlanItemResponse(
    UUID id,
    UUID exerciseId,
    String division,
    int sets,
    String reps,
    BigDecimal suggestedLoadKg,
    Integer restSeconds,
    int orderIndex,
    String notes) {}
