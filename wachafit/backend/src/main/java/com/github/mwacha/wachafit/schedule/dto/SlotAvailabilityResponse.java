package com.github.mwacha.wachafit.schedule.dto;

import java.util.UUID;

public record SlotAvailabilityResponse(UUID scheduleId, int remainingSlots) {}
