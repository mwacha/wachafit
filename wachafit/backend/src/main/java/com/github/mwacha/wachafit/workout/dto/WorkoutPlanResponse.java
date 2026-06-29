package com.github.mwacha.wachafit.workout.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkoutPlanResponse(
    UUID id,
    UUID studentId,
    UUID trainerId,
    String name,
    String description,
    boolean active,
    Instant createdAt,
    List<WorkoutPlanItemResponse> items) {}
