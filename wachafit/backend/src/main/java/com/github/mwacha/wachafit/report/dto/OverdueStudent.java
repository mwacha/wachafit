package com.github.mwacha.wachafit.report.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OverdueStudent(UUID studentId, String name, BigDecimal totalDue, int daysOverdue) {}
