package com.github.mwacha.wachafit.membership;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MemberSubscriptionRepository extends JpaRepository<MemberSubscription, UUID> {

    boolean existsByStudentIdAndStatus(UUID studentId, String status);

    Optional<MemberSubscription> findByStudentIdAndStatus(UUID studentId, String status);
}
