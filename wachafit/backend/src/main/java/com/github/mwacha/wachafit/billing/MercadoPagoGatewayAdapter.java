package com.github.mwacha.wachafit.billing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.payment.gateway", havingValue = "mercadopago")
public class MercadoPagoGatewayAdapter implements PaymentGatewayService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoGatewayAdapter.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final RestClient restClient;
    private final PaymentProperties paymentProperties;
    private final String frontendUrl;

    public MercadoPagoGatewayAdapter(RestClient.Builder builder,
                                     PaymentProperties paymentProperties,
                                     @Value("${app.frontend-url}") String frontendUrl) {
        this.paymentProperties = paymentProperties;
        this.frontendUrl = frontendUrl;
        this.restClient = builder
            .baseUrl("https://api.mercadopago.com")
            .defaultHeader("Authorization", "Bearer " + paymentProperties.getAccessToken())
            .build();
    }

    @Override
    public boolean validateWebhookSignature(WebhookVerificationRequest request) {
        if (request.signatureHeader() == null || request.signatureHeader().isBlank()
                || request.dataId() == null || request.requestId() == null) {
            return false;
        }
        Map<String, String> parts = parseSignatureHeader(request.signatureHeader());
        String ts = parts.get("ts");
        String receivedV1 = parts.get("v1");
        if (ts == null || receivedV1 == null) {
            return false;
        }
        String manifest = "id:" + request.dataId().toLowerCase() + ";request-id:" + request.requestId() + ";ts:" + ts + ";";
        String computedV1 = hmacSha256Hex(manifest, paymentProperties.getWebhookSecret());
        return MessageDigest.isEqual(
            computedV1.getBytes(StandardCharsets.UTF_8),
            receivedV1.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public CheckoutResult createCheckout(PaymentCharge charge) {
        var request = new PreferenceRequest(
            java.util.List.of(new PreferenceRequest.Item("Mensalidade", 1, charge.getAmount(), "BRL")),
            charge.getId().toString(),
            paymentProperties.getNotificationUrl(),
            new PreferenceRequest.BackUrls(
                frontendUrl + "/student/charges",
                frontendUrl + "/student/charges",
                frontendUrl + "/student/charges")
        );

        PreferenceResponse response = restClient.post()
            .uri("/checkout/preferences")
            .body(request)
            .retrieve()
            .body(PreferenceResponse.class);

        if (response == null || response.initPoint() == null) {
            return CheckoutResult.none();
        }
        return new CheckoutResult("MERCADOPAGO", response.id(), response.initPoint());
    }

    @Override
    public Optional<PaymentUpdate> fetchPaymentUpdate(String externalPaymentId) {
        PaymentResponse response = restClient.get()
            .uri("/v1/payments/{id}", externalPaymentId)
            .retrieve()
            .body(PaymentResponse.class);

        if (response == null || response.externalReference() == null) {
            return Optional.empty();
        }
        UUID chargeId;
        try {
            chargeId = UUID.fromString(response.externalReference());
        } catch (IllegalArgumentException e) {
            log.warn("Webhook MP com external_reference inválido: {}", response.externalReference());
            return Optional.empty();
        }
        return Optional.of(new PaymentUpdate(chargeId, response.id(), mapStatus(response.status())));
    }

    private String mapStatus(String mpStatus) {
        return switch (mpStatus) {
            case "approved" -> "PAID";
            case "cancelled", "rejected" -> "CANCELLED";
            default -> "PENDING";
        };
    }

    private Map<String, String> parseSignatureHeader(String header) {
        Map<String, String> parts = new HashMap<>();
        for (String segment : header.split(",")) {
            String[] kv = segment.split("=", 2);
            if (kv.length == 2) {
                parts.put(kv[0].trim(), kv[1].trim());
            }
        }
        return parts;
    }

    private String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao calcular HMAC do webhook Mercado Pago", e);
        }
    }

    private record PreferenceRequest(
        java.util.List<Item> items,
        @JsonProperty("external_reference") String externalReference,
        @JsonProperty("notification_url") String notificationUrl,
        @JsonProperty("back_urls") BackUrls backUrls
    ) {
        private record Item(String title, Integer quantity,
                            @JsonProperty("unit_price") java.math.BigDecimal unitPrice,
                            @JsonProperty("currency_id") String currencyId) {}

        private record BackUrls(String success, String failure, String pending) {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PreferenceResponse(String id, @JsonProperty("init_point") String initPoint) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PaymentResponse(String id, String status,
                                   @JsonProperty("external_reference") String externalReference) {}
}
