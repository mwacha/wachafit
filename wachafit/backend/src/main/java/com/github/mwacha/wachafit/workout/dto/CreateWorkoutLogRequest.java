package com.github.mwacha.wachafit.workout.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateWorkoutLogRequest(
    UUID exerciseId,
    UUID workoutPlanItemId,
    LocalDate performedAt,
    Integer sets,
    Integer reps,
    BigDecimal loadKg,
    String notes) {}
