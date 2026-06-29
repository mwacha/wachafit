package com.github.mwacha.wachafit.booking.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateBookingRequest(@NotNull UUID scheduleId) {}
