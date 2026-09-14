package com.github.mwacha.wachafit.billing;

import java.util.Optional;

public interface PaymentGatewayService {

    boolean validateWebhookSignature(WebhookVerificationRequest request);

    CheckoutResult createCheckout(PaymentCharge charge);

    Optional<PaymentUpdate> fetchPaymentUpdate(String externalPaymentId);
}
