package com.github.mwacha.wachafit.notification.event;

import java.util.UUID;

public record BookingCancelledEvent(
    UUID studentId,
    String className,
    String date,
    String time
) {}
