package com.github.mwacha.wachafit.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TrainerAvailabilityRepository extends JpaRepository<TrainerAvailability, UUID> {
    List<TrainerAvailability> findByTrainerId(UUID trainerId);

    @Modifying
    @Query("DELETE FROM TrainerAvailability a WHERE a.trainerId = :trainerId")
    void deleteByTrainerId(@Param("trainerId") UUID trainerId);
}
