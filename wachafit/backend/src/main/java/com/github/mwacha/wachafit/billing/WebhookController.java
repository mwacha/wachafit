package com.github.mwacha.wachafit.billing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final PaymentGatewayService gatewayService;
    private final BillingService billingService;

    public WebhookController(PaymentGatewayService gatewayService, BillingService billingService) {
        this.gatewayService = gatewayService;
        this.billingService = billingService;
    }

    // Contrato de notificação do Mercado Pago: ?type=payment&data.id=<id>, headers x-signature/x-request-id.
    // O corpo não participa da validação de assinatura nem é confiável por si só -- o status real do
    // pagamento é sempre buscado na API do gateway (gatewayService.fetchPaymentUpdate).
    @PostMapping("/api/payments/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "data.id", required = false) String dataId,
            @RequestHeader(value = "x-signature", required = false, defaultValue = "") String signature,
            @RequestHeader(value = "x-request-id", required = false, defaultValue = "") String requestId) {
        if (!"payment".equals(type) || dataId == null) {
            return ResponseEntity.ok().build();
        }
        if (!gatewayService.validateWebhookSignature(new WebhookVerificationRequest(dataId, requestId, signature))) {
            log.warn("Assinatura de webhook inválida para payment {}", dataId);
            return ResponseEntity.badRequest().build();
        }
        gatewayService.fetchPaymentUpdate(dataId)
            .ifPresent(update -> billingService.processWebhookCharge(
                update.chargeId(), update.externalPaymentId(), update.status()));
        return ResponseEntity.ok().build();
    }
}
