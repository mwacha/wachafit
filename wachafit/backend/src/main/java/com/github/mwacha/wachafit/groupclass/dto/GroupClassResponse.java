package com.github.mwacha.wachafit.groupclass.dto;

import java.util.List;

public record GroupClassResponse(
    String id,
    String name,
    String description,
    int capacity,
    int durationMinutes,
    String trainerId,
    String trainerName,
    boolean active,
    String createdAt,
    String scheduleType,
    String startTime,
    String endTime,
    List<String> daysOfWeek,
    int enrolledCount
) {}
