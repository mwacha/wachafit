package com.github.mwacha.wachafit.workout;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonalRecordRepository extends JpaRepository<PersonalRecord, UUID> {

    Optional<PersonalRecord> findByStudentIdAndExerciseId(UUID studentId, UUID exerciseId);

    List<PersonalRecord> findByStudentIdOrderByAchievedAtDesc(UUID studentId);
}
