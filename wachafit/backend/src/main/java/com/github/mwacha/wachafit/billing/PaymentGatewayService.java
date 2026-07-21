package com.github.mwacha.wachafit.billing;

public interface PaymentGatewayService {
    boolean validateWebhookSignature(String payload, String signature);
}
