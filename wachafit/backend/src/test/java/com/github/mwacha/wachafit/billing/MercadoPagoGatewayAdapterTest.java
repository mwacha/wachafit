package com.github.mwacha.wachafit.billing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MercadoPagoGatewayAdapterTest {

    private PaymentProperties paymentProperties;
    private RestClient.Builder builder;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        paymentProperties = new PaymentProperties();
        paymentProperties.setAccessToken("test-access-token");
        paymentProperties.setWebhookSecret("test-secret");
        paymentProperties.setNotificationUrl("https://backend.test/api/payments/webhook");

        builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
    }

    private MercadoPagoGatewayAdapter newAdapter() {
        return new MercadoPagoGatewayAdapter(builder, paymentProperties, "https://app.test");
    }

    // --- validateWebhookSignature ---

    @Test
    void validateWebhookSignature_acceptsValidSignature() {
        // manifest = "id:123456;request-id:req-abc;ts:1700000000;" HMAC-SHA256 with secret "test-secret",
        // computed independently via `openssl dgst -sha256 -hmac test-secret`.
        String validV1 = "32fe22ad82298409906f07dd55dc753a98ef0ca8b5b3fc6358adaca4a1aa316d";
        var request = new WebhookVerificationRequest("123456", "req-abc", "ts=1700000000,v1=" + validV1);

        assertThat(newAdapter().validateWebhookSignature(request)).isTrue();
    }

    @Test
    void validateWebhookSignature_rejectsTamperedSignature() {
        var request = new WebhookVerificationRequest("123456", "req-abc", "ts=1700000000,v1=deadbeef");
        assertThat(newAdapter().validateWebhookSignature(request)).isFalse();
    }

    @Test
    void validateWebhookSignature_rejectsMissingSignatureHeader() {
        var request = new WebhookVerificationRequest("123456", "req-abc", "");
        assertThat(newAdapter().validateWebhookSignature(request)).isFalse();
    }

    // --- createCheckout ---

    @Test
    void createCheckout_returnsCheckoutUrlFromPreferenceResponse() {
        UUID chargeId = UUID.randomUUID();
        PaymentCharge charge = new PaymentCharge();
        setId(charge, chargeId);
        charge.setAmount(new BigDecimal("99.90"));
        charge.setDueDate(LocalDate.now());

        mockServer.expect(requestTo("https://api.mercadopago.com/checkout/preferences"))
            .andExpect(method(org.springframework.http.HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer test-access-token"))
            .andExpect(jsonPath("$.external_reference").value(chargeId.toString()))
            .andExpect(jsonPath("$.notification_url").value("https://backend.test/api/payments/webhook"))
            .andRespond(withSuccess("""
                {"id":"pref-123","init_point":"https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=pref-123"}
                """, MediaType.APPLICATION_JSON));

        CheckoutResult result = newAdapter().createCheckout(charge);

        assertThat(result.hasCheckout()).isTrue();
        assertThat(result.gateway()).isEqualTo("MERCADOPAGO");
        assertThat(result.externalId()).isEqualTo("pref-123");
        assertThat(result.checkoutUrl()).isEqualTo("https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=pref-123");
        mockServer.verify();
    }

    // --- fetchPaymentUpdate ---

    @Test
    void fetchPaymentUpdate_mapsApprovedToPaid() {
        UUID chargeId = UUID.randomUUID();
        mockServer.expect(requestTo("https://api.mercadopago.com/v1/payments/987654"))
            .andExpect(header("Authorization", "Bearer test-access-token"))
            .andRespond(withSuccess("""
                {"id":987654,"status":"approved","external_reference":"%s"}
                """.formatted(chargeId), MediaType.APPLICATION_JSON));

        Optional<PaymentUpdate> update = newAdapter().fetchPaymentUpdate("987654");

        assertThat(update).isPresent();
        assertThat(update.get().chargeId()).isEqualTo(chargeId);
        assertThat(update.get().externalPaymentId()).isEqualTo("987654");
        assertThat(update.get().status()).isEqualTo("PAID");
    }

    @Test
    void fetchPaymentUpdate_mapsRejectedToCancelled() {
        UUID chargeId = UUID.randomUUID();
        mockServer.expect(requestTo("https://api.mercadopago.com/v1/payments/111"))
            .andRespond(withSuccess("""
                {"id":111,"status":"rejected","external_reference":"%s"}
                """.formatted(chargeId), MediaType.APPLICATION_JSON));

        Optional<PaymentUpdate> update = newAdapter().fetchPaymentUpdate("111");

        assertThat(update).isPresent();
        assertThat(update.get().status()).isEqualTo("CANCELLED");
    }

    @Test
    void fetchPaymentUpdate_returnsEmpty_whenExternalReferenceIsNotAValidUuid() {
        mockServer.expect(requestTo("https://api.mercadopago.com/v1/payments/222"))
            .andRespond(withSuccess("""
                {"id":222,"status":"approved","external_reference":"not-a-uuid"}
                """, MediaType.APPLICATION_JSON));

        Optional<PaymentUpdate> update = newAdapter().fetchPaymentUpdate("222");

        assertThat(update).isEmpty();
    }

    private static void setId(PaymentCharge charge, UUID id) {
        try {
            var f = PaymentCharge.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(charge, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
