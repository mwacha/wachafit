package com.github.mwacha.wachafit.schedule.dto;

public record ScheduleResponse(
    String id, String groupClassId, String groupClassName,
    String trainerId, String type, String status,
    String startsAt, String endsAt, String createdAt
) {}
