package com.github.mwacha.wachafit.exercise.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateExerciseRequest(@NotBlank String name, @NotBlank String muscleGroup, String description, String videoUrl) {}
