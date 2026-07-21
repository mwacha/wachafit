package com.github.mwacha.wachafit.billing;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.payment")
public class PaymentProperties {

    private String gateway = "manual";
    private String accessToken;
    private String webhookSecret;
    private int suspendAfterDays = 5;

    public String getGateway() { return gateway; }
    public void setGateway(String v) { this.gateway = v; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String v) { this.accessToken = v; }
    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String v) { this.webhookSecret = v; }
    public int getSuspendAfterDays() { return suspendAfterDays; }
    public void setSuspendAfterDays(int v) { this.suspendAfterDays = v; }
}
