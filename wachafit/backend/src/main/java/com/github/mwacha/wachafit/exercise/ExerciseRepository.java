package com.github.mwacha.wachafit.exercise;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {

    // nativeQuery = true: o @Filter automático de tenant do Hibernate só se aplica a consultas
    // HQL/Criteria contra a entidade mapeada -- queries SQL nativas o ignoram completamente, então
    // o tenant precisa ser filtrado explicitamente aqui (achado como bug real: usuário via
    // exercícios de OUTRA academia numa academia recém-criada, sem nenhum exercício próprio).
    @Query(value = """
        SELECT * FROM exercises
        WHERE active = true
        AND tenant_id = CAST(:tenantId AS uuid)
        AND (:q IS NULL OR LOWER(name) LIKE LOWER(CONCAT('%', CAST(:q AS TEXT), '%')))
        AND (:muscleGroup IS NULL OR muscle_group = CAST(:muscleGroup AS TEXT))
        ORDER BY name
        """, nativeQuery = true)
    List<Exercise> search(@Param("tenantId") UUID tenantId, @Param("q") String q, @Param("muscleGroup") String muscleGroup);
}
