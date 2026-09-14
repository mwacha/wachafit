package com.github.mwacha.wachafit.billing;

public record CheckoutResult(String gateway, String externalId, String checkoutUrl) {

    public static CheckoutResult none() {
        return new CheckoutResult(null, null, null);
    }

    public boolean hasCheckout() {
        return checkoutUrl != null;
    }
}
