package com.github.mwacha.wachafit.workout;

import com.github.mwacha.wachafit.workout.dto.ProgressionPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface WorkoutLogRepository extends JpaRepository<WorkoutLog, UUID> {

    List<WorkoutLog> findByStudentIdOrderByPerformedAtDesc(UUID studentId);

    @Query("""
        SELECT new com.github.mwacha.wachafit.workout.dto.ProgressionPoint(
            l.performedAt, l.loadKg, l.reps)
        FROM WorkoutLog l
        WHERE l.studentId = :studentId AND l.exerciseId = :exerciseId
        ORDER BY l.performedAt ASC
    """)
    List<ProgressionPoint> findProgressionByStudentAndExercise(
        @Param("studentId") UUID studentId,
        @Param("exerciseId") UUID exerciseId);
}
