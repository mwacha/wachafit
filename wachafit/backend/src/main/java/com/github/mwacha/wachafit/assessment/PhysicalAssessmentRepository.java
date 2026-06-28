package com.github.mwacha.wachafit.assessment;

import com.github.mwacha.wachafit.assessment.dto.EvolutionPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PhysicalAssessmentRepository extends JpaRepository<PhysicalAssessment, UUID> {

    List<PhysicalAssessment> findByStudentIdOrderByAssessedAtAsc(UUID studentId);

    @Query("""
        SELECT new com.github.mwacha.wachafit.assessment.dto.EvolutionPoint(
            a.assessedAt, a.weightKg, a.bodyFatPct, a.bmi)
        FROM PhysicalAssessment a
        WHERE a.studentId = :studentId
        ORDER BY a.assessedAt ASC
    """)
    List<EvolutionPoint> findEvolutionByStudentId(@Param("studentId") UUID studentId);
}
