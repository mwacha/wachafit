package com.github.mwacha.wachafit.workout.dto;

import java.util.List;

public record CreateWorkoutPlanRequest(
    String name,
    String description,
    List<WorkoutPlanItemRequest> items) {}
