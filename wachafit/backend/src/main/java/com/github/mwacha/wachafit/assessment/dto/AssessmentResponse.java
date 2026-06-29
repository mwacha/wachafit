package com.github.mwacha.wachafit.assessment.dto;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
public record AssessmentResponse(
    UUID id, UUID studentId, UUID assessedBy, LocalDate assessedAt,
    BigDecimal weightKg, BigDecimal heightCm, BigDecimal bodyFatPct, BigDecimal bmi,
    String notes, List<MeasurementResponse> measurements, Instant createdAt
) {}
