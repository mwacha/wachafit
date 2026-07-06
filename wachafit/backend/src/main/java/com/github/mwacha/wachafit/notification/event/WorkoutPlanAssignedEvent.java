package com.github.mwacha.wachafit.notification.event;

import java.util.UUID;

public record WorkoutPlanAssignedEvent(UUID studentId, UUID trainerId, String planName) {}
