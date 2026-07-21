package com.github.mwacha.wachafit.billing;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_charges")
public class PaymentCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @Column(length = 20)
    private String gateway;

    @Column(name = "external_charge_id", length = 255)
    private String externalChargeId;

    @Column(name = "external_payment_url", columnDefinition = "TEXT")
    private String externalPaymentUrl;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public UUID getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(UUID v) { this.subscriptionId = v; }
    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID v) { this.studentId = v; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { this.amount = v; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate v) { this.dueDate = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public OffsetDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(OffsetDateTime v) { this.paidAt = v; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String v) { this.paymentMethod = v; }
    public String getGateway() { return gateway; }
    public void setGateway(String v) { this.gateway = v; }
    public String getExternalChargeId() { return externalChargeId; }
    public void setExternalChargeId(String v) { this.externalChargeId = v; }
    public String getExternalPaymentUrl() { return externalPaymentUrl; }
    public void setExternalPaymentUrl(String v) { this.externalPaymentUrl = v; }
    public Instant getCreatedAt() { return createdAt; }
}
