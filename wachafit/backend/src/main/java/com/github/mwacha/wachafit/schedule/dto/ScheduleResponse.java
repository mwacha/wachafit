package com.github.mwacha.wachafit.schedule.dto;

import java.util.List;

public record ScheduleResponse(
    String id, String groupClassId, String groupClassName,
    String trainerId, String type, String status,
    String startsAt, String endsAt, String createdAt,
    List<BookedStudentSummary> bookedStudents
) {}
