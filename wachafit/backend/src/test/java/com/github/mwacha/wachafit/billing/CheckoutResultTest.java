package com.github.mwacha.wachafit.billing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CheckoutResultTest {

    @Test
    void none_hasNoCheckout() {
        CheckoutResult result = CheckoutResult.none();
        assertThat(result.hasCheckout()).isFalse();
    }

    @Test
    void withCheckoutUrl_hasCheckoutTrue() {
        CheckoutResult result = new CheckoutResult("MERCADOPAGO", "pref-123", "https://mp.example/pay/pref-123");
        assertThat(result.hasCheckout()).isTrue();
        assertThat(result.gateway()).isEqualTo("MERCADOPAGO");
        assertThat(result.externalId()).isEqualTo("pref-123");
    }
}
