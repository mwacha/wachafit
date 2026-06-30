package com.github.mwacha.wachafit.billing.dto;

import jakarta.validation.constraints.NotBlank;

public record ManualPaymentRequest(
    @NotBlank String paymentMethod
) {}
