package com.github.mwacha.wachafit.billing;

import java.util.UUID;

public record PaymentUpdate(UUID chargeId, String externalPaymentId, String status) {}
