package com.github.mwacha.wachafit.billing.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ChargeResponse(
    UUID id,
    UUID studentId,
    UUID subscriptionId,
    BigDecimal amount,
    LocalDate dueDate,
    String status,
    OffsetDateTime paidAt,
    String paymentMethod,
    String externalPaymentUrl,
    Instant createdAt
) {}
