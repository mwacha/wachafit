package com.github.mwacha.wachafit.workout;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "personal_records")
public class PersonalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;

    @Column(name = "record_load_kg", nullable = false, precision = 6, scale = 2)
    private BigDecimal recordLoadKg;

    @Column(name = "achieved_at", nullable = false)
    private LocalDate achievedAt;

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID v) { this.studentId = v; }
    public UUID getExerciseId() { return exerciseId; }
    public void setExerciseId(UUID v) { this.exerciseId = v; }
    public BigDecimal getRecordLoadKg() { return recordLoadKg; }
    public void setRecordLoadKg(BigDecimal v) { this.recordLoadKg = v; }
    public LocalDate getAchievedAt() { return achievedAt; }
    public void setAchievedAt(LocalDate v) { this.achievedAt = v; }
}
