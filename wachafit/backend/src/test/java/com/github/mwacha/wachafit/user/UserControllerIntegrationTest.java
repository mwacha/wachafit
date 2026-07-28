package com.github.mwacha.wachafit.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mwacha.wachafit.account.Account;
import com.github.mwacha.wachafit.account.AccountRepository;
import com.github.mwacha.wachafit.auth.dto.LoginRequest;
import com.github.mwacha.wachafit.auth.dto.RegisterRequest;
import com.github.mwacha.wachafit.tenant.Tenant;
import com.github.mwacha.wachafit.tenant.TenantContext;
import com.github.mwacha.wachafit.tenant.TenantRepository;
import com.github.mwacha.wachafit.user.dto.CreateUserRequest;
import com.github.mwacha.wachafit.user.dto.UpdateUserRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        r.add("jwt.secret", () -> "integration-test-secret-32-chars-ok");
        r.add("jwt.expiration", () -> "3600");
        r.add("app.frontend-url", () -> "http://localhost:5173");
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired TenantRepository tenantRepository;
    @Autowired PasswordEncoder passwordEncoder;
    private String studentToken;

    @BeforeEach
    void setup() throws Exception {
        // Register a STUDENT user (register endpoint creates STUDENT by default)
        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new RegisterRequest("Test User", "user@test.com", "password123", "personal-studio"))))
            .andReturn();

        var result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new LoginRequest("user@test.com", "password123"))))
            .andReturn();
        var body = objectMapper.readTree(result.getResponse().getContentAsString());
        studentToken = body.get("token").asText();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private String createAdminAndLogin(Tenant tenant, String email) throws Exception {
        Account account = new Account();
        account.setName("Admin " + email); account.setEmail(email);
        account.setPasswordHash(passwordEncoder.encode("pass12345"));
        accountRepository.save(account);
        User admin = new User();
        admin.setAccount(account); admin.setRole(Role.ADMIN); admin.setTenant(tenant); admin.setActive(true);
        userRepository.save(admin);

        var result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new LoginRequest(email, "pass12345"))))
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    // Regressão: User não é TenantAwareEntity (sem @Filter automático do Hibernate) -- é
    // proposital, para o fluxo de login buscar vínculos de uma Account em vários tenants. Isso
    // deixou UserService.listUsers()/findOrThrow() sem escopo de tenant algum (usavam findAll()/
    // findById() cru), vazando/permitindo editar usuários de QUALQUER academia. Bug real
    // reportado pelo usuário em produção: ao entrar numa academia nova, via lista de
    // usuários/alunos de outra academia.
    @Test
    void listUsers_scopesToOwnTenant_notOtherTenants() throws Exception {
        Tenant tenantA = tenantRepository.findBySlug("personal-studio").orElseThrow();
        Tenant tenantB = new Tenant();
        tenantB.setName("Academia B"); tenantB.setSlug("isolation-users-" + UUID.randomUUID());
        tenantB.setActive(true);
        tenantB = tenantRepository.save(tenantB);

        String tenantBAdminToken = createAdminAndLogin(tenantB, "admin-b-" + UUID.randomUUID() + "@test.com");

        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + tenantBAdminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].role").value("ADMIN"));
    }

    @Test
    void activate_withUserFromOtherTenant_shouldReturn404() throws Exception {
        Tenant tenantA = tenantRepository.findBySlug("personal-studio").orElseThrow();
        Tenant tenantB = new Tenant();
        tenantB.setName("Academia C"); tenantB.setSlug("isolation-activate-" + UUID.randomUUID());
        tenantB.setActive(true);
        tenantB = tenantRepository.save(tenantB);

        String tenantBAdminToken = createAdminAndLogin(tenantB, "admin-c-" + UUID.randomUUID() + "@test.com");

        // usuário do teste (tenantA / personal-studio) criado no @BeforeEach via /api/auth/register
        Account tenantAAccount = accountRepository.findByEmail("user@test.com").orElseThrow();
        User tenantAUser = userRepository.findByAccountIdAndTenantId(tenantAAccount.getId(), tenantA.getId()).orElseThrow();

        mockMvc.perform(patch("/api/admin/users/" + tenantAUser.getId() + "/activate")
                .header("Authorization", "Bearer " + tenantBAdminToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateUser_withUserFromOtherTenant_shouldReturn404() throws Exception {
        Tenant tenantA = tenantRepository.findBySlug("personal-studio").orElseThrow();
        Tenant tenantB = new Tenant();
        tenantB.setName("Academia D"); tenantB.setSlug("isolation-update-" + UUID.randomUUID());
        tenantB.setActive(true);
        tenantB = tenantRepository.save(tenantB);

        String tenantBAdminToken = createAdminAndLogin(tenantB, "admin-d-" + UUID.randomUUID() + "@test.com");

        Account tenantAAccount = accountRepository.findByEmail("user@test.com").orElseThrow();
        User tenantAUser = userRepository.findByAccountIdAndTenantId(tenantAAccount.getId(), tenantA.getId()).orElseThrow();

        mockMvc.perform(patch("/api/admin/users/" + tenantAUser.getId())
                .header("Authorization", "Bearer " + tenantBAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateUserRequest("Nome Hackeado", Role.ADMIN))))
            .andExpect(status().isNotFound());
    }

    @Test
    void listUsers_withStudentToken_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/admin/users")
            .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void listUsers_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void createUser_withStudentToken_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/admin/users")
            .header("Authorization", "Bearer " + studentToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new CreateUserRequest("Trainer", "t@test.com", "password123", Role.TRAINER))))
            .andExpect(status().isForbidden());
    }
}
