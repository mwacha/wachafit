package com.github.mwacha.wachafit.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByAccountIdAndTenantId(UUID accountId, UUID tenantId);
    boolean existsByAccountIdAndTenantId(UUID accountId, UUID tenantId);
    List<User> findByAccountIdAndActiveTrue(UUID accountId);

    boolean existsByIdAndTenantId(UUID id, UUID tenantId);
    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);
    List<User> findByTenantId(UUID tenantId);
}
