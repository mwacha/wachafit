package com.github.mwacha.wachafit.assessment.dto;
import java.math.BigDecimal;
public record MeasurementRequest(String bodyPart, BigDecimal valueCm) {}
