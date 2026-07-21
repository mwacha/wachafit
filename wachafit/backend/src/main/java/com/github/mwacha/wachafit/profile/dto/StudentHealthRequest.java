package com.github.mwacha.wachafit.profile.dto;

import java.time.LocalDate;

public record StudentHealthRequest(
    // Condições médicas
    boolean hasHeartCondition,
    boolean hasDiabetes,
    boolean hasHypertension,
    boolean hasRespiratoryCondition,
    boolean hasOrthopedicCondition,
    boolean hadSurgery,
    String surgeryDescription,
    boolean hasChronicPain,
    String chronicPainLocation,
    String medications,
    String physicalRestrictions,

    // Hábitos de vida
    boolean smokes,
    boolean drinksAlcohol,
    String alcoholFrequency,
    Integer sleepHours,
    Integer stressLevel,
    String activityLevel,

    // Objetivos
    String fitnessGoal,
    String fitnessLevel,
    String exerciseHistory,

    // PAR-Q
    boolean parqHeartProblem,
    boolean parqChestPainExercise,
    boolean parqChestPainRest,
    boolean parqDizziness,
    boolean parqBoneJoint,
    boolean parqBloodPressureMeds,
    boolean parqOtherReason,
    String parqOtherReasonDetail,
    LocalDate parqSignedAt,

    String notes
) {}
