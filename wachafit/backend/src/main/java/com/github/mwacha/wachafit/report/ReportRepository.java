package com.github.mwacha.wachafit.report;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class ReportRepository {

    @PersistenceContext
    private EntityManager em;

    @SuppressWarnings("unchecked")
    public List<Object[]> getRevenueRows(LocalDate from, LocalDate to) {
        String sql = """
            SELECT DATE_TRUNC('month', paid_at AT TIME ZONE 'UTC')::date AS month,
                   SUM(amount)  AS total,
                   COUNT(*)     AS charges_count
            FROM payment_charges
            WHERE status = 'PAID'
              AND paid_at >= :from
              AND paid_at <  :to
            GROUP BY 1
            ORDER BY 1
            """;
        return em.createNativeQuery(sql)
            .setParameter("from", from.atStartOfDay())
            .setParameter("to", to.plusDays(1).atStartOfDay())
            .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getOverdueStudentRows() {
        String sql = """
            SELECT c.student_id::text,
                   u.name,
                   SUM(c.amount)  AS total_due,
                   MIN(c.due_date) AS oldest_due
            FROM payment_charges c
            JOIN users u ON u.id = c.student_id
            WHERE c.status = 'OVERDUE'
            GROUP BY c.student_id, u.name
            ORDER BY MIN(c.due_date) ASC
            """;
        return em.createNativeQuery(sql).getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getSubscriptionStatusCounts() {
        return em.createQuery(
            "SELECT s.status, COUNT(s) FROM MemberSubscription s GROUP BY s.status"
        ).getResultList();
    }

    public int countExpiredActive() {
        Long count = (Long) em.createQuery(
            "SELECT COUNT(s) FROM MemberSubscription s WHERE s.status = 'ACTIVE' AND s.expiresAt < :today"
        ).setParameter("today", LocalDate.now()).getSingleResult();
        return count.intValue();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getTrainerCommissionRows(LocalDate from, LocalDate to) {
        String sql = """
            SELECT u.id::text,
                   u.name,
                   tp.commission_type,
                   tp.commission_value,
                   COUNT(b.id) AS classes_count
            FROM users u
            JOIN trainer_profiles tp ON tp.user_id = u.id
            LEFT JOIN schedules s ON s.trainer_id = u.id
                AND s.starts_at::date BETWEEN :from AND :to
            LEFT JOIN bookings b ON b.schedule_id = s.id AND b.status = 'CONFIRMED'
            WHERE u.role = 'TRAINER'
            GROUP BY u.id, u.name, tp.commission_type, tp.commission_value
            ORDER BY u.name
            """;
        return em.createNativeQuery(sql)
            .setParameter("from", from)
            .setParameter("to", to)
            .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getCashFlowRows(LocalDate from, LocalDate to) {
        String sql = """
            SELECT due_date,
                   SUM(CASE WHEN status = 'PAID'    THEN amount ELSE 0 END) AS received,
                   SUM(CASE WHEN status = 'PENDING' THEN amount ELSE 0 END) AS pending,
                   SUM(CASE WHEN status = 'OVERDUE' THEN amount ELSE 0 END) AS overdue
            FROM payment_charges
            WHERE due_date BETWEEN :from AND :to
            GROUP BY due_date
            ORDER BY due_date
            """;
        return em.createNativeQuery(sql)
            .setParameter("from", from)
            .setParameter("to", to)
            .getResultList();
    }
}
