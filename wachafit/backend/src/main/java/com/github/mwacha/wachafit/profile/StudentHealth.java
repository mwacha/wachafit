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

    // Condições médicas existentes
    @Column(name = "has_heart_condition", nullable = false) private boolean hasHeartCondition;
    @Column(name = "has_diabetes", nullable = false) private boolean hasDiabetes;
    @Column(name = "has_hypertension", nullable = false) private boolean hasHypertension;
    @Column(name = "has_respiratory_condition", nullable = false) private boolean hasRespiratoryCondition;
    @Column(name = "has_orthopedic_condition", nullable = false) private boolean hasOrthopedicCondition;
    @Column(name = "had_surgery", nullable = false) private boolean hadSurgery;
    @Column(name = "surgery_description") private String surgeryDescription;
    @Column(name = "has_chronic_pain", nullable = false) private boolean hasChronicPain;
    @Column(name = "chronic_pain_location", length = 200) private String chronicPainLocation;
    private String medications;
    @Column(name = "physical_restrictions") private String physicalRestrictions;

    // Hábitos de vida
    @Column(name = "smokes", nullable = false) private boolean smokes;
    @Column(name = "drinks_alcohol", nullable = false) private boolean drinksAlcohol;
    @Column(name = "alcohol_frequency", length = 30) private String alcoholFrequency;
    @Column(name = "sleep_hours") private Integer sleepHours;
    @Column(name = "stress_level") private Integer stressLevel;
    @Column(name = "activity_level", length = 30) private String activityLevel;

    // Objetivos e histórico de treino
    @Column(name = "fitness_goal", length = 50) private String fitnessGoal;
    @Column(name = "fitness_level", length = 20) private String fitnessLevel;
    @Column(name = "exercise_history") private String exerciseHistory;

    // PAR-Q
    @Column(name = "parq_heart_problem", nullable = false) private boolean parqHeartProblem;
    @Column(name = "parq_chest_pain_exercise", nullable = false) private boolean parqChestPainExercise;
    @Column(name = "parq_chest_pain_rest", nullable = false) private boolean parqChestPainRest;
    @Column(name = "parq_dizziness", nullable = false) private boolean parqDizziness;
    @Column(name = "parq_bone_joint", nullable = false) private boolean parqBoneJoint;
    @Column(name = "parq_blood_pressure_meds", nullable = false) private boolean parqBloodPressureMeds;
    @Column(name = "parq_other_reason", nullable = false) private boolean parqOtherReason;
    @Column(name = "parq_other_reason_detail") private String parqOtherReasonDetail;

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
    public boolean isHasRespiratoryCondition() { return hasRespiratoryCondition; }
    public void setHasRespiratoryCondition(boolean v) { this.hasRespiratoryCondition = v; }
    public boolean isHasOrthopedicCondition() { return hasOrthopedicCondition; }
    public void setHasOrthopedicCondition(boolean v) { this.hasOrthopedicCondition = v; }
    public boolean isHadSurgery() { return hadSurgery; }
    public void setHadSurgery(boolean v) { this.hadSurgery = v; }
    public String getSurgeryDescription() { return surgeryDescription; }
    public void setSurgeryDescription(String v) { this.surgeryDescription = v; }
    public boolean isHasChronicPain() { return hasChronicPain; }
    public void setHasChronicPain(boolean v) { this.hasChronicPain = v; }
    public String getChronicPainLocation() { return chronicPainLocation; }
    public void setChronicPainLocation(String v) { this.chronicPainLocation = v; }
    public String getMedications() { return medications; }
    public void setMedications(String v) { this.medications = v; }
    public String getPhysicalRestrictions() { return physicalRestrictions; }
    public void setPhysicalRestrictions(String v) { this.physicalRestrictions = v; }
    public boolean isSmokes() { return smokes; }
    public void setSmokes(boolean v) { this.smokes = v; }
    public boolean isDrinksAlcohol() { return drinksAlcohol; }
    public void setDrinksAlcohol(boolean v) { this.drinksAlcohol = v; }
    public String getAlcoholFrequency() { return alcoholFrequency; }
    public void setAlcoholFrequency(String v) { this.alcoholFrequency = v; }
    public Integer getSleepHours() { return sleepHours; }
    public void setSleepHours(Integer v) { this.sleepHours = v; }
    public Integer getStressLevel() { return stressLevel; }
    public void setStressLevel(Integer v) { this.stressLevel = v; }
    public String getActivityLevel() { return activityLevel; }
    public void setActivityLevel(String v) { this.activityLevel = v; }
    public String getFitnessGoal() { return fitnessGoal; }
    public void setFitnessGoal(String v) { this.fitnessGoal = v; }
    public String getFitnessLevel() { return fitnessLevel; }
    public void setFitnessLevel(String v) { this.fitnessLevel = v; }
    public String getExerciseHistory() { return exerciseHistory; }
    public void setExerciseHistory(String v) { this.exerciseHistory = v; }
    public boolean isParqHeartProblem() { return parqHeartProblem; }
    public void setParqHeartProblem(boolean v) { this.parqHeartProblem = v; }
    public boolean isParqChestPainExercise() { return parqChestPainExercise; }
    public void setParqChestPainExercise(boolean v) { this.parqChestPainExercise = v; }
    public boolean isParqChestPainRest() { return parqChestPainRest; }
    public void setParqChestPainRest(boolean v) { this.parqChestPainRest = v; }
    public boolean isParqDizziness() { return parqDizziness; }
    public void setParqDizziness(boolean v) { this.parqDizziness = v; }
    public boolean isParqBoneJoint() { return parqBoneJoint; }
    public void setParqBoneJoint(boolean v) { this.parqBoneJoint = v; }
    public boolean isParqBloodPressureMeds() { return parqBloodPressureMeds; }
    public void setParqBloodPressureMeds(boolean v) { this.parqBloodPressureMeds = v; }
    public boolean isParqOtherReason() { return parqOtherReason; }
    public void setParqOtherReason(boolean v) { this.parqOtherReason = v; }
    public String getParqOtherReasonDetail() { return parqOtherReasonDetail; }
    public void setParqOtherReasonDetail(String v) { this.parqOtherReasonDetail = v; }
    public LocalDate getParqSignedAt() { return parqSignedAt; }
    public void setParqSignedAt(LocalDate v) { this.parqSignedAt = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
}
