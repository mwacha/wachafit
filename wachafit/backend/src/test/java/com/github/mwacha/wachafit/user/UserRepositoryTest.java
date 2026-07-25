package com.github.mwacha.wachafit.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    UserRepository userRepository;

    @Autowired
    com.github.mwacha.wachafit.tenant.TenantRepository tenantRepository;

    @Autowired
    com.github.mwacha.wachafit.account.AccountRepository accountRepository;

    @Test
    void shouldSaveAndFindUserByEmail() {
        User user = new User();
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed");
        user.setRole(Role.STUDENT);

        User saved = userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("test@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getRole()).isEqualTo(Role.STUDENT);
        assertThat(found.get().isActive()).isTrue();
    }

    @Test
    void existsByEmail_shouldReturnTrueIfExists() {
        User user = new User();
        user.setName("Another");
        user.setEmail("another@example.com");
        user.setPasswordHash("hashed");
        user.setRole(Role.TRAINER);
        userRepository.save(user);

        assertThat(userRepository.existsByEmail("another@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nobody@example.com")).isFalse();
    }

    @Test
    void findByAccountIdAndTenantId_returnsMembership() {
        var tenant = new com.github.mwacha.wachafit.tenant.Tenant();
        tenant.setName("Academia Teste");
        tenant.setSlug("academia-teste-" + java.util.UUID.randomUUID());
        tenant = tenantRepository.save(tenant);

        var account = new com.github.mwacha.wachafit.account.Account();
        account.setName("Pessoa Teste");
        account.setEmail("pessoa" + java.util.UUID.randomUUID() + "@teste.com");
        account.setPasswordHash("hash");
        account = accountRepository.save(account);

        User user = new User();
        user.setAccount(account);
        user.setRole(Role.STUDENT);
        user.setTenant(tenant);
        User saved = userRepository.save(user);

        var found = userRepository.findByAccountIdAndTenantId(account.getId(), tenant.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getEmail()).isEqualTo(account.getEmail());

        assertThat(userRepository.existsByAccountIdAndTenantId(account.getId(), tenant.getId())).isTrue();
        assertThat(userRepository.findByAccountIdAndActiveTrue(account.getId())).hasSize(1);
    }
}
