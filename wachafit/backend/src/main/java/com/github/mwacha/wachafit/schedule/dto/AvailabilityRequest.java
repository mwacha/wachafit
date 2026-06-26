package com.github.mwacha.wachafit.schedule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AvailabilityRequest(@NotNull List<SlotAvailability> slots) {
    public record SlotAvailability(
        @Min(1) @Max(7) int weekday,
        @NotNull String startTime,
        @NotNull String endTime
    ) {}
}
