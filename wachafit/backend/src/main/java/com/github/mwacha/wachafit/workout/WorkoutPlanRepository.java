package com.github.mwacha.wachafit.workout;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, UUID> {

    List<WorkoutPlan> findByStudentIdOrderByCreatedAtDesc(UUID studentId);

    Optional<WorkoutPlan> findByStudentIdAndActiveTrue(UUID studentId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE WorkoutPlan p SET p.active = false WHERE p.studentId = :studentId")
    void deactivateAllForStudent(@Param("studentId") UUID studentId);
}
