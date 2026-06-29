package com.github.mwacha.wachafit.workout;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkoutPlanItemRepository extends JpaRepository<WorkoutPlanItem, UUID> {
}
