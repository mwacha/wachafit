package com.github.mwacha.wachafit.goal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StudentGoalRepository extends JpaRepository<StudentGoal, UUID> {
    List<StudentGoal> findByStudentIdOrderByCreatedAtDesc(UUID studentId);
}
