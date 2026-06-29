package com.github.mwacha.wachafit.groupclass.dto;

public record GroupClassResponse(
    String id,
    String name,
    String description,
    int capacity,
    int durationMinutes,
    String trainerId,
    String trainerName,
    boolean active,
    String createdAt
) {}
