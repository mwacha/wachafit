package com.github.mwacha.wachafit.billing;

import com.github.mwacha.wachafit.membership.MemberSubscriptionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
public class OverdueJobService {

    private final PaymentChargeRepository chargeRepo;
    private final MemberSubscriptionRepository subscriptionRepo;
    private final PaymentProperties paymentProperties;

    public OverdueJobService(PaymentChargeRepository chargeRepo,
                             MemberSubscriptionRepository subscriptionRepo,
                             PaymentProperties paymentProperties) {
        this.chargeRepo = chargeRepo;
        this.subscriptionRepo = subscriptionRepo;
        this.paymentProperties = paymentProperties;
    }

    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void markOverdueAndSuspend() {
        LocalDate today = LocalDate.now();
        chargeRepo.markOverdue(today);

        LocalDate cutoff = today.minusDays(paymentProperties.getSuspendAfterDays());
        List<UUID> subscriptionIds = chargeRepo.findSubscriptionIdsWithOverdueChargesOlderThan(cutoff);
        for (UUID subId : subscriptionIds) {
            subscriptionRepo.findById(subId).ifPresent(sub -> {
                if ("ACTIVE".equals(sub.getStatus())) {
                    sub.setStatus("SUSPENDED");
                    subscriptionRepo.save(sub);
                }
            });
        }
    }
}
