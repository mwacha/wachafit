package com.github.mwacha.wachafit.saas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TenantSubscriptionRepositoryTest {

    @Autowired TenantSubscriptionRepository subscriptionRepo;
    @Autowired TenantChargeRepository chargeRepo;

    @Test
    void savesSubscriptionAndCharge() {
        UUID tenantId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        TenantSubscription sub = new TenantSubscription();
        sub.setTenantId(tenantId);
        sub.setSaasPlanId(planId);
        sub.setStatus("TRIALING");
        sub.setTrialEndsAt(Instant.now().plus(14, ChronoUnit.DAYS));
        TenantSubscription savedSub = subscriptionRepo.save(sub);

        TenantCharge charge = new TenantCharge();
        charge.setTenantId(tenantId);
        charge.setSubscriptionId(savedSub.getId());
        charge.setAmount(new BigDecimal("299.90"));
        charge.setDueDate(LocalDate.now().plusDays(14));
        charge.setStatus("PENDING");
        charge.setPaymentMethod("PIX");
        TenantCharge savedCharge = chargeRepo.save(charge);

        var foundSub = subscriptionRepo.findById(savedSub.getId());
        assertThat(foundSub).isPresent();
        assertThat(foundSub.get().getStatus()).isEqualTo("TRIALING");

        var foundCharge = chargeRepo.findById(savedCharge.getId());
        assertThat(foundCharge).isPresent();
        assertThat(foundCharge.get().getSubscriptionId()).isEqualTo(savedSub.getId());
        assertThat(foundCharge.get().getPaymentMethod()).isEqualTo("PIX");
    }
}
