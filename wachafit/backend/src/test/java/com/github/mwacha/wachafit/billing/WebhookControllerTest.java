package com.github.mwacha.wachafit.billing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookControllerTest {

    @Mock PaymentGatewayService gatewayService;
    @Mock BillingService billingService;
    @InjectMocks WebhookController controller;

    @Test
    void handleWebhook_ignoresNonPaymentNotifications() {
        ResponseEntity<Void> response = controller.handleWebhook("merchant_order", "123", "sig", "req-1");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verifyNoInteractions(gatewayService, billingService);
    }

    @Test
    void handleWebhook_rejectsInvalidSignature() {
        when(gatewayService.validateWebhookSignature(any())).thenReturn(false);

        ResponseEntity<Void> response = controller.handleWebhook("payment", "123", "bad-sig", "req-1");

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        verify(billingService, never()).processWebhookCharge(any(), any(), any());
    }

    @Test
    void handleWebhook_processesChargeUpdate_whenSignatureValidAndPaymentFound() {
        UUID chargeId = UUID.randomUUID();
        when(gatewayService.validateWebhookSignature(any())).thenReturn(true);
        when(gatewayService.fetchPaymentUpdate("123"))
            .thenReturn(Optional.of(new PaymentUpdate(chargeId, "123", "PAID")));

        ResponseEntity<Void> response = controller.handleWebhook("payment", "123", "good-sig", "req-1");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(billingService).processWebhookCharge(chargeId, "123", "PAID");
    }

    @Test
    void handleWebhook_returnsOk_whenPaymentUpdateNotFound() {
        when(gatewayService.validateWebhookSignature(any())).thenReturn(true);
        when(gatewayService.fetchPaymentUpdate("123")).thenReturn(Optional.empty());

        ResponseEntity<Void> response = controller.handleWebhook("payment", "123", "good-sig", "req-1");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(billingService, never()).processWebhookCharge(any(), any(), any());
    }

    @Test
    void handleWebhook_passesDataIdAndRequestIdToSignatureValidation() {
        when(gatewayService.validateWebhookSignature(
            eq(new WebhookVerificationRequest("123", "req-1", "good-sig")))).thenReturn(true);
        when(gatewayService.fetchPaymentUpdate("123")).thenReturn(Optional.empty());

        controller.handleWebhook("payment", "123", "good-sig", "req-1");

        verify(gatewayService).validateWebhookSignature(new WebhookVerificationRequest("123", "req-1", "good-sig"));
    }
}
