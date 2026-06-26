package com.github.mwacha.wachafit.schedule.dto;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record SlotRequest(@NotNull OffsetDateTime startsAt, @NotNull OffsetDateTime endsAt) {}
