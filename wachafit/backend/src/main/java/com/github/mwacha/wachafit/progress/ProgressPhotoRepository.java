package com.github.mwacha.wachafit.progress;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ProgressPhotoRepository extends JpaRepository<ProgressPhoto, UUID> {
    List<ProgressPhoto> findByStudentIdOrderByTakenAtDesc(UUID studentId);
}
