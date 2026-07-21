package com.github.mwacha.wachafit.billing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.payment.gateway", havingValue = "manual", matchIfMissing = true)
public class ManualGatewayAdapter implements PaymentGatewayService {

    // Validação desabilitada intencionalmente. app.payment.webhook-secret só é lido por adapters reais (MercadoPago, PagSeguro).
    @Override
    public boolean validateWebhookSignature(String payload, String signature) {
        return true;
    }
}
