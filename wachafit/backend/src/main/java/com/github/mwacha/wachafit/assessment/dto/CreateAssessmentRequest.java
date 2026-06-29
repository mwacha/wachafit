package com.github.mwacha.wachafit.assessment.dto;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record CreateAssessmentRequest(
    @NotNull LocalDate assessedAt,
    BigDecimal weightKg,
    BigDecimal heightCm,
    BigDecimal bodyFatPct,
    BigDecimal bmi,
    String notes,
    List<MeasurementRequest> measurements
) {}
