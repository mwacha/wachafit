package com.github.mwacha.wachafit.assessment.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
public record EvolutionPoint(LocalDate assessedAt, BigDecimal weightKg, BigDecimal bodyFatPct, BigDecimal bmi) {}
