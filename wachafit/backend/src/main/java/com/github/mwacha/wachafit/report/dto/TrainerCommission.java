package com.github.mwacha.wachafit.report.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TrainerCommission(UUID trainerId, String name, String commissionType, BigDecimal commissionDue, int classesCount) {}
