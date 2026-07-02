package com.github.mwacha.wachafit.notification.event;

import java.util.UUID;

public record BookingConfirmedEvent(
    UUID studentId,
    UUID trainerId,
    String className,
    String date,
    String time
) {}
