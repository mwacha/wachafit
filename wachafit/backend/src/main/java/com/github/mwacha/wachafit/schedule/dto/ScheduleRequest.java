package com.github.mwacha.wachafit.schedule.dto;

import com.github.mwacha.wachafit.schedule.ScheduleType;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ScheduleRequest(
    UUID groupClassId,
    @NotNull UUID trainerId,
    @NotNull ScheduleType type,
    @NotNull OffsetDateTime startsAt,
    @NotNull OffsetDateTime endsAt
) {}
