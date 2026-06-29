package com.github.mwacha.wachafit.exercise;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {

    @Query(value = """
        SELECT * FROM exercises
        WHERE active = true
        AND (:q IS NULL OR LOWER(name) LIKE LOWER(CONCAT('%', CAST(:q AS TEXT), '%')))
        AND (:muscleGroup IS NULL OR muscle_group = CAST(:muscleGroup AS TEXT))
        ORDER BY name
        """, nativeQuery = true)
    List<Exercise> search(@Param("q") String q, @Param("muscleGroup") String muscleGroup);
}
