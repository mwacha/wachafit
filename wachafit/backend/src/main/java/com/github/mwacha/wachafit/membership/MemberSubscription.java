package com.github.mwacha.wachafit.membership;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "member_subscriptions")
public class MemberSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "started_at", nullable = false)
    private LocalDate startedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDate expiresAt;

    @Column(name = "cancelled_at")
    private LocalDate cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID v) { this.studentId = v; }
    public UUID getPlanId() { return planId; }
    public void setPlanId(UUID v) { this.planId = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public LocalDate getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDate v) { this.startedAt = v; }
    public LocalDate getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDate v) { this.expiresAt = v; }
    public LocalDate getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDate v) { this.cancelledAt = v; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String v) { this.cancellationReason = v; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID v) { this.createdBy = v; }
    public Instant getCreatedAt() { return createdAt; }
}
