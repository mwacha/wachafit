package com.github.mwacha.wachafit.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentChargeRepository extends JpaRepository<PaymentCharge, UUID> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE PaymentCharge c SET c.status = 'CANCELLED' WHERE c.subscriptionId = :subscriptionId AND c.status = 'PENDING'")
    void cancelPendingBySubscriptionId(@Param("subscriptionId") UUID subscriptionId);

    List<PaymentCharge> findByStudentIdOrderByCreatedAtDesc(UUID studentId);

    Optional<PaymentCharge> findByExternalChargeId(String externalChargeId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE PaymentCharge c SET c.status = 'OVERDUE' WHERE c.status = 'PENDING' AND c.dueDate < :today")
    void markOverdue(@Param("today") LocalDate today);

    @Query("SELECT DISTINCT c.subscriptionId FROM PaymentCharge c WHERE c.status = 'OVERDUE' AND c.dueDate < :cutoffDate")
    List<UUID> findSubscriptionIdsWithOverdueChargesOlderThan(@Param("cutoffDate") LocalDate cutoffDate);

    @Query("SELECT COUNT(c) > 0 FROM PaymentCharge c WHERE c.studentId = :studentId AND (c.status = 'OVERDUE' OR (c.status = 'PENDING' AND c.dueDate < :today))")
    boolean existsUnpaidOverdueByStudentId(@Param("studentId") UUID studentId, @Param("today") LocalDate today);
}
