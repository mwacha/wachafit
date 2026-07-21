package com.github.mwacha.wachafit.groupclass;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, UUID> {

    Optional<ClassEnrollment> findByGroupClassIdAndStudentId(UUID classId, UUID studentId);

    List<ClassEnrollment> findByGroupClassIdAndStatus(UUID classId, String status);

    List<ClassEnrollment> findByStudentIdAndStatus(UUID studentId, String status);

    long countByGroupClassIdAndStatus(UUID classId, String status);

    long countByStudentIdAndStatus(UUID studentId, String status);
}
