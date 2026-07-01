package com.github.mwacha.wachafit.report.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

public record RevenueReport(YearMonth month, BigDecimal total, int chargesCount) {}
