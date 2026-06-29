package com.github.mwacha.wachafit.workout;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "workout_plan_items")
public class WorkoutPlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_plan_id", nullable = false)
    private WorkoutPlan workoutPlan;

    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;

    @Column(length = 10)
    private String division;

    @Column(nullable = false)
    private int sets;

    @Column(nullable = false, length = 20)
    private String reps;

    @Column(name = "suggested_load_kg", precision = 6, scale = 2)
    private BigDecimal suggestedLoadKg;

    @Column(name = "rest_seconds")
    private Integer restSeconds;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(length = 200)
    private String notes;

    public UUID getId() { return id; }
    public WorkoutPlan getWorkoutPlan() { return workoutPlan; }
    public void setWorkoutPlan(WorkoutPlan v) { this.workoutPlan = v; }
    public UUID getExerciseId() { return exerciseId; }
    public void setExerciseId(UUID v) { this.exerciseId = v; }
    public String getDivision() { return division; }
    public void setDivision(String v) { this.division = v; }
    public int getSets() { return sets; }
    public void setSets(int v) { this.sets = v; }
    public String getReps() { return reps; }
    public void setReps(String v) { this.reps = v; }
    public BigDecimal getSuggestedLoadKg() { return suggestedLoadKg; }
    public void setSuggestedLoadKg(BigDecimal v) { this.suggestedLoadKg = v; }
    public Integer getRestSeconds() { return restSeconds; }
    public void setRestSeconds(Integer v) { this.restSeconds = v; }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int v) { this.orderIndex = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
}
