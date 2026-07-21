package com.github.mwacha.wachafit.notification.event;

import java.util.UUID;

public record PersonalSessionRequestedEvent(
    UUID studentId,
    UUID trainerId,
    String date,
    String time
) {}
