package com.github.mwacha.wachafit.profile;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, UUID> {
    Optional<StudentProfile> findByUserId(UUID userId);
    boolean existsByCpfAndUserIdNot(String cpf, UUID userId);
}
