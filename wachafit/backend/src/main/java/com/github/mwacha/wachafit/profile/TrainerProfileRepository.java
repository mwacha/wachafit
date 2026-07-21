package com.github.mwacha.wachafit.profile;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainerProfileRepository extends JpaRepository<TrainerProfile, UUID> {
    Optional<TrainerProfile> findByUserId(UUID userId);
    List<TrainerProfile> findAllByOrderByCreatedAtDesc();
}
