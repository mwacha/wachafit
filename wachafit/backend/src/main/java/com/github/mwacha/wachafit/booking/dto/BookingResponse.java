package com.github.mwacha.wachafit.booking.dto;

public record BookingResponse(
    String id,
    String scheduleId,
    String startsAt,
    String endsAt,
    String type,
    String status,
    String groupClassName,
    String trainerName,
    String bookedAt
) {}
