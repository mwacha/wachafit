package com.github.mwacha.wachafit.groupclass.dto;

import java.util.List;

public record EnrolledClassResponse(
    String classId,
    String className,
    String trainerName,
    String startTime,
    String endTime,
    List<String> daysOfWeek
) {}
