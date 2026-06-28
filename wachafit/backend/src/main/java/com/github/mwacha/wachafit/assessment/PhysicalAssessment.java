package com.github.mwacha.wachafit.assessment;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "physical_assessments")
public class PhysicalAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "assessed_by", nullable = false)
    private UUID assessedBy;

    @Column(name = "assessed_at", nullable = false)
    private LocalDate assessedAt;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "height_cm", precision = 5, scale = 2)
    private BigDecimal heightCm;

    @Column(name = "body_fat_pct", precision = 4, scale = 1)
    private BigDecimal bodyFatPct;

    @Column(precision = 4, scale = 1)
    private BigDecimal bmi;

    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "assessment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<AssessmentMeasurement> measurements = new ArrayList<>();

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID studentId) { this.studentId = studentId; }
    public UUID getAssessedBy() { return assessedBy; }
    public void setAssessedBy(UUID assessedBy) { this.assessedBy = assessedBy; }
    public LocalDate getAssessedAt() { return assessedAt; }
    public void setAssessedAt(LocalDate assessedAt) { this.assessedAt = assessedAt; }
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    public BigDecimal getHeightCm() { return heightCm; }
    public void setHeightCm(BigDecimal heightCm) { this.heightCm = heightCm; }
    public BigDecimal getBodyFatPct() { return bodyFatPct; }
    public void setBodyFatPct(BigDecimal bodyFatPct) { this.bodyFatPct = bodyFatPct; }
    public BigDecimal getBmi() { return bmi; }
    public void setBmi(BigDecimal bmi) { this.bmi = bmi; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public List<AssessmentMeasurement> getMeasurements() { return measurements; }
}
