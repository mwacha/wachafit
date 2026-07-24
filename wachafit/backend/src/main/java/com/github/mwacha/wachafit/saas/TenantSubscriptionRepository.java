package com.github.mwacha.wachafit.saas;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface TenantSubscriptionRepository extends JpaRepository<TenantSubscription, UUID> {
}
