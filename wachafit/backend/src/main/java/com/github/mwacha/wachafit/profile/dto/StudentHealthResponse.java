package com.github.mwacha.wachafit.profile.dto;

import java.time.LocalDate;
import java.util.UUID;

public record StudentHealthResponse(
    UUID id, UUID userId,
    boolean hasHeartCondition, boolean hasDiabetes, boolean hasHypertension,
    boolean hasRespiratoryCondition, boolean hasOrthopedicCondition,
    boolean hadSurgery, String surgeryDescription,
    boolean hasChronicPain, String chronicPainLocation,
    String medications, String physicalRestrictions,
    boolean smokes, boolean drinksAlcohol, String alcoholFrequency,
    Integer sleepHours, Integer stressLevel, String activityLevel,
    String fitnessGoal, String fitnessLevel, String exerciseHistory,
    boolean parqHeartProblem, boolean parqChestPainExercise, boolean parqChestPainRest,
    boolean parqDizziness, boolean parqBoneJoint, boolean parqBloodPressureMeds,
    boolean parqOtherReason, String parqOtherReasonDetail,
    LocalDate parqSignedAt, String notes
) {}
