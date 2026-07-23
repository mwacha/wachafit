package com.github.mwacha.wachafit.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailAndTenantId(String email, UUID tenantId);
    boolean existsByEmailAndTenantId(String email, UUID tenantId);
    boolean existsByIdAndTenantId(UUID id, UUID tenantId);
    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);

    // Mantidos para uso do SUPER_ADMIN (sem filtro de tenant)
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
