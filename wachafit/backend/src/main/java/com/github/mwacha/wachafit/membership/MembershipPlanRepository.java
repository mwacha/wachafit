package com.github.mwacha.wachafit.membership;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, UUID> {
}
