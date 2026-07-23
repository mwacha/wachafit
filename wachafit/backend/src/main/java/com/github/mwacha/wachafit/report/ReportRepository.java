package com.github.mwacha.wachafit.report;

import com.github.mwacha.wachafit.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public class ReportRepository {

    @PersistenceContext
    private EntityManager em;

    private UUID tenantId() { return TenantContext.get(); }

    @SuppressWarnings("unchecked")
    public List<Object[]> getRevenueRows(LocalDate from, LocalDate to) {
        return em.createNativeQuery("""
            SELECT DATE_TRUNC('month', paid_at AT TIME ZONE 'UTC')::date AS month,
                   SUM(amount) AS total, COUNT(*) AS charges_count
            FROM payment_charges
            WHERE status = 'PAID'
              AND tenant_id = :tenantId
              AND paid_at >= :from AND paid_at < :to
            GROUP BY 1 ORDER BY 1
            """)
            .setParameter("tenantId", tenantId())
            .setParameter("from", from.atStartOfDay())
            .setParameter("to", to.plusDays(1).atStartOfDay())
            .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getOverdueStudentRows() {
        return em.createNativeQuery("""
            SELECT c.student_id::text, u.name,
                   SUM(c.amount) AS total_due, MIN(c.due_date) AS oldest_due
            FROM payment_charges c
            JOIN users u ON u.id = c.student_id
            WHERE c.status = 'OVERDUE'
              AND c.tenant_id = :tenantId
            GROUP BY c.student_id, u.name
            ORDER BY MIN(c.due_date) ASC
            """)
            .setParameter("tenantId", tenantId())
            .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getSubscriptionStatusCounts() {
        return em.createQuery("""
            SELECT s.status, COUNT(s)
            FROM MemberSubscription s
            WHERE s.tenantId = :tenantId
            GROUP BY s.status
            """)
            .setParameter("tenantId", tenantId())
            .getResultList();
    }

    public int countExpiredActive() {
        Long count = (Long) em.createQuery("""
            SELECT COUNT(s) FROM MemberSubscription s
            WHERE s.status = 'ACTIVE' AND s.expiresAt < :today AND s.tenantId = :tenantId
            """)
            .setParameter("today", LocalDate.now())
            .setParameter("tenantId", tenantId())
            .getSingleResult();
        return count.intValue();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getTrainerCommissionRows(LocalDate from, LocalDate to) {
        return em.createNativeQuery("""
            SELECT u.id::text, u.name, tp.commission_type, tp.commission_value,
                   COUNT(b.id) AS classes_count
            FROM users u
            JOIN trainer_profiles tp ON tp.user_id = u.id AND tp.tenant_id = :tenantId
            LEFT JOIN schedules s ON s.trainer_id = u.id AND s.tenant_id = :tenantId
                AND s.starts_at::date BETWEEN :from AND :to
            LEFT JOIN bookings b ON b.schedule_id = s.id AND b.status = 'CONFIRMED'
                AND b.tenant_id = :tenantId
            WHERE u.role IN ('TRAINER','PROFESSOR') AND u.tenant_id = :tenantId
            GROUP BY u.id, u.name, tp.commission_type, tp.commission_value
            ORDER BY u.name
            """)
            .setParameter("tenantId", tenantId())
            .setParameter("from", from)
            .setParameter("to", to)
            .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getEnrollmentTrendRows(LocalDate from) {
        return em.createNativeQuery("""
            SELECT TO_CHAR(DATE_TRUNC('month', started_at), 'YYYY-MM') AS month,
                   COUNT(*) AS cnt
            FROM member_subscriptions
            WHERE started_at >= :from AND tenant_id = :tenantId
            GROUP BY 1 ORDER BY 1
            """)
            .setParameter("from", from)
            .setParameter("tenantId", tenantId())
            .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getAttendanceRankingRows(LocalDate from, int limit) {
        return em.createNativeQuery("""
            SELECT u.name, COUNT(b.id) AS cnt
            FROM bookings b
            JOIN users u ON u.id = b.student_id
            WHERE b.status = 'CONFIRMED'
              AND b.tenant_id = :tenantId
              AND b.booked_at >= :from
            GROUP BY u.id, u.name
            ORDER BY cnt DESC
            LIMIT :limit
            """)
            .setParameter("tenantId", tenantId())
            .setParameter("from", from.atStartOfDay())
            .setParameter("limit", limit)
            .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getCashFlowRows(LocalDate from, LocalDate to) {
        return em.createNativeQuery("""
            SELECT due_date,
                   SUM(CASE WHEN status = 'PAID'    THEN amount ELSE 0 END) AS received,
                   SUM(CASE WHEN status = 'PENDING' THEN amount ELSE 0 END) AS pending,
                   SUM(CASE WHEN status = 'OVERDUE' THEN amount ELSE 0 END) AS overdue
            FROM payment_charges
            WHERE due_date BETWEEN :from AND :to
              AND tenant_id = :tenantId
            GROUP BY due_date ORDER BY due_date
            """)
            .setParameter("tenantId", tenantId())
            .setParameter("from", from)
            .setParameter("to", to)
            .getResultList();
    }
}
