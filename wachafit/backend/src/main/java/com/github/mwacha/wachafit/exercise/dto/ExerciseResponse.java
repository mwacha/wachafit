package com.github.mwacha.wachafit.exercise.dto;

import java.util.UUID;

public record ExerciseResponse(UUID id, String name, String muscleGroup, String description, String videoUrl, boolean active) {}
