package com.github.mwacha.wachafit.workout.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CreateWorkoutPlanRequest(
    @NotBlank String name,
    String description,
    List<WorkoutPlanItemRequest> items) {}
