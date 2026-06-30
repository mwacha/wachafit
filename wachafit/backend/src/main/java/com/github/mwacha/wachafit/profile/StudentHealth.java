package com.github.mwacha.wachafit.profile;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "student_health")
public class StudentHealth {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id", nullable = false, unique = true) private UUID userId;
    @Column(name = "has_heart_condition", nullable = false) private boolean hasHeartCondition;
    @Column(name = "has_diabetes", nullable = false) private boolean hasDiabetes;
    @Column(name = "has_hypertension", nullable = false) private boolean hasHypertension;
    private String medications;
    @Column(name = "physical_restrictions") private String physicalRestrictions;
    @Column(name = "parq_signed_at") private LocalDate parqSignedAt;
    private String notes;
    @Column(name = "updated_at", nullable = false, updatable = false, insertable = false) private Instant updatedAt;

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID v) { this.userId = v; }
    public boolean isHasHeartCondition() { return hasHeartCondition; }
    public void setHasHeartCondition(boolean v) { this.hasHeartCondition = v; }
    public boolean isHasDiabetes() { return hasDiabetes; }
    public void setHasDiabetes(boolean v) { this.hasDiabetes = v; }
    public boolean isHasHypertension() { return hasHypertension; }
    public void setHasHypertension(boolean v) { this.hasHypertension = v; }
    public String getMedications() { return medications; }
    public void setMedications(String v) { this.medications = v; }
    public String getPhysicalRestrictions() { return physicalRestrictions; }
    public void setPhysicalRestrictions(String v) { this.physicalRestrictions = v; }
    public LocalDate getParqSignedAt() { return parqSignedAt; }
    public void setParqSignedAt(LocalDate v) { this.parqSignedAt = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
}
