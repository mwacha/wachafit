package com.github.mwacha.wachafit.groupclass.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record UpdateGroupClassRequest(
    @NotBlank String name,
    String description,
    @Min(1) int capacity,
    Integer durationMinutes,
    @NotBlank String scheduleType,
    String startTime,
    String endTime,
    List<String> daysOfWeek
) {}
