package com.github.mwacha.wachafit.groupclass.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public record UpdateGroupClassRequest(
    @NotBlank String name,
    String description,
    @Min(1) int capacity,
    Integer durationMinutes,
    UUID trainerId,
    @NotBlank String scheduleType,
    String startTime,
    String endTime,
    List<String> daysOfWeek
) {}
