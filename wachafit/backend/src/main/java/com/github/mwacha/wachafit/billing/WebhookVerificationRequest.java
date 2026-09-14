package com.github.mwacha.wachafit.billing;

public record WebhookVerificationRequest(String dataId, String requestId, String signatureHeader) {}
