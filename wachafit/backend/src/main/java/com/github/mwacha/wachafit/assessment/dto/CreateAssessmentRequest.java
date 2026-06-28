package com.github.mwacha.wachafit.assessment.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record CreateAssessmentRequest(
    LocalDate assessedAt,
    BigDecimal weightKg,
    BigDecimal heightCm,
    BigDecimal bodyFatPct,
    BigDecimal bmi,
    String notes,
    List<MeasurementRequest> measurements
) {}
