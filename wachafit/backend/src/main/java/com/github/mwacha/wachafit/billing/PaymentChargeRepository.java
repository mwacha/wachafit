package com.github.mwacha.wachafit.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PaymentChargeRepository extends JpaRepository<PaymentCharge, UUID> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE PaymentCharge c SET c.status = 'CANCELLED' WHERE c.subscriptionId = :subscriptionId AND c.status = 'PENDING'")
    void cancelPendingBySubscriptionId(@Param("subscriptionId") UUID subscriptionId);
}
