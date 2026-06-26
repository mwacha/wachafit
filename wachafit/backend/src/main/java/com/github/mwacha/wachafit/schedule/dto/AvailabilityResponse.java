package com.github.mwacha.wachafit.schedule.dto;

import java.util.List;

public record AvailabilityResponse(List<SlotAvailability> slots) {
    public record SlotAvailability(int weekday, String startTime, String endTime) {}
}
