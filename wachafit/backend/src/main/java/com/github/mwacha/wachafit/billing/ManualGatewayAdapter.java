package com.github.mwacha.wachafit.billing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.payment.gateway", havingValue = "manual", matchIfMissing = true)
public class ManualGatewayAdapter implements PaymentGatewayService {

    @Override
    public boolean validateWebhookSignature(String payload, String signature) {
        return true;
    }
}
