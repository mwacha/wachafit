package com.github.mwacha.wachafit.schedule.dto;

import com.github.mwacha.wachafit.schedule.ScheduleType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record BatchScheduleRequest(
    UUID groupClassId,
    @NotNull UUID trainerId,
    @NotNull ScheduleType type,
    @NotEmpty List<SlotRequest> slots
) {}
