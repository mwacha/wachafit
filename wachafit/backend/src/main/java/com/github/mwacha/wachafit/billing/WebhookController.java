package com.github.mwacha.wachafit.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mwacha.wachafit.billing.dto.WebhookPayload;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class WebhookController {

    private final PaymentGatewayService gatewayService;
    private final BillingService billingService;
    private final ObjectMapper objectMapper;

    public WebhookController(PaymentGatewayService gatewayService,
                             BillingService billingService,
                             ObjectMapper objectMapper) {
        this.gatewayService = gatewayService;
        this.billingService = billingService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/api/payments/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "x-signature", required = false, defaultValue = "") String signature) {
        if (!gatewayService.validateWebhookSignature(payload, signature)) {
            return ResponseEntity.badRequest().build();
        }
        try {
            WebhookPayload parsed = objectMapper.readValue(payload, WebhookPayload.class);
            if (parsed.externalChargeId() != null && parsed.status() != null) {
                billingService.processWebhookCharge(parsed.externalChargeId(), parsed.status());
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }
}
