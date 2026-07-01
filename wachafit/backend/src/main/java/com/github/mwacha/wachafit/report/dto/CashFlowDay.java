package com.github.mwacha.wachafit.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CashFlowDay(LocalDate date, BigDecimal received, BigDecimal pending, BigDecimal overdue) {}
