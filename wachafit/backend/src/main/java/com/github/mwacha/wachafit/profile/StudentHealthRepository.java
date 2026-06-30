package com.github.mwacha.wachafit.profile;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface StudentHealthRepository extends JpaRepository<StudentHealth, UUID> {
    Optional<StudentHealth> findByUserId(UUID userId);
}
