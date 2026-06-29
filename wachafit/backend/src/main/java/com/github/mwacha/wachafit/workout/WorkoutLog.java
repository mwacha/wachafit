package com.github.mwacha.wachafit.workout;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "workout_logs")
public class WorkoutLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;

    @Column(name = "workout_plan_item_id")
    private UUID workoutPlanItemId;

    @Column(name = "performed_at", nullable = false)
    private LocalDate performedAt;

    private Integer sets;

    private Integer reps;

    @Column(name = "load_kg", precision = 6, scale = 2)
    private BigDecimal loadKg;

    @Column(length = 200)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID v) { this.studentId = v; }
    public UUID getExerciseId() { return exerciseId; }
    public void setExerciseId(UUID v) { this.exerciseId = v; }
    public UUID getWorkoutPlanItemId() { return workoutPlanItemId; }
    public void setWorkoutPlanItemId(UUID v) { this.workoutPlanItemId = v; }
    public LocalDate getPerformedAt() { return performedAt; }
    public void setPerformedAt(LocalDate v) { this.performedAt = v; }
    public Integer getSets() { return sets; }
    public void setSets(Integer v) { this.sets = v; }
    public Integer getReps() { return reps; }
    public void setReps(Integer v) { this.reps = v; }
    public BigDecimal getLoadKg() { return loadKg; }
    public void setLoadKg(BigDecimal v) { this.loadKg = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
    public Instant getCreatedAt() { return createdAt; }
}
