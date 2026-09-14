package com.github.mwacha.wachafit.billing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@ConditionalOnProperty(name = "app.payment.gateway", havingValue = "manual", matchIfMissing = true)
public class ManualGatewayAdapter implements PaymentGatewayService {

    // Gateway manual não recebe webhooks reais; validação sempre permissiva.
    @Override
    public boolean validateWebhookSignature(WebhookVerificationRequest request) {
        return true;
    }

    // Sem checkout externo: staff registra o pagamento diretamente via payCharge().
    @Override
    public CheckoutResult createCheckout(PaymentCharge charge) {
        return CheckoutResult.none();
    }

    @Override
    public Optional<PaymentUpdate> fetchPaymentUpdate(String externalPaymentId) {
        return Optional.empty();
    }
}
