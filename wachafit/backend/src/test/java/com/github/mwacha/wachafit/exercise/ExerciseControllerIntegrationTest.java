package com.github.mwacha.wachafit.exercise;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mwacha.wachafit.account.Account;
import com.github.mwacha.wachafit.auth.dto.LoginRequest;
import com.github.mwacha.wachafit.exercise.dto.CreateExerciseRequest;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class ExerciseControllerIntegrationTest {

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

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository userRepo;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired com.github.mwacha.wachafit.account.AccountRepository accountRepository;
    @Autowired com.github.mwacha.wachafit.tenant.TenantRepository tenantRepository;

    private String trainerToken;

    @BeforeEach
    void setUp() throws Exception {
        userRepo.deleteAll();
        accountRepository.deleteAll();
        var tenant = tenantRepository.findBySlug("personal-studio").orElseThrow();
        Account trainerAccount = new Account();
        trainerAccount.setName("T");
        trainerAccount.setEmail("t@t.com");
        trainerAccount.setPasswordHash(passwordEncoder.encode("pass"));
        accountRepository.save(trainerAccount);
        User trainer = new User();
        trainer.setAccount(trainerAccount);
        trainer.setRole(Role.TRAINER);
        trainer.setTenant(tenant);
        trainer.setActive(true);
        userRepo.save(trainer);

        var result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new LoginRequest("t@t.com", "pass"))))
                .andReturn();
        trainerToken = mapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    @Test
    void create_shouldReturn201() throws Exception {
        mvc.perform(post("/api/exercises")
                        .header("Authorization", "Bearer " + trainerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                new CreateExerciseRequest("Squat", "legs", null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Squat"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void search_withToken_shouldReturn200() throws Exception {
        mvc.perform(get("/api/exercises")
                        .header("Authorization", "Bearer " + trainerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void search_withoutToken_shouldReturn401() throws Exception {
        mvc.perform(get("/api/exercises"))
                .andExpect(status().isUnauthorized());
    }

    // Regressão: search() usa query SQL nativa, que o @Filter automático de tenant do Hibernate
    // NÃO cobre -- um exercício criado no tenant A não podia aparecer para o tenant B. Bug real
    // reportado pelo usuário: exercícios de outra academia apareciam numa academia recém-criada
    // sem nenhum exercício próprio.
    @Test
    void search_doesNotReturnExercisesFromOtherTenant() throws Exception {
        mvc.perform(post("/api/exercises")
                        .header("Authorization", "Bearer " + trainerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                new CreateExerciseRequest("Deadlift", "back", null, null))))
                .andExpect(status().isCreated());

        var tenantB = new com.github.mwacha.wachafit.tenant.Tenant();
        tenantB.setName("Academia B");
        tenantB.setSlug("isolation-exercise-" + java.util.UUID.randomUUID());
        tenantB.setActive(true);
        tenantB = tenantRepository.save(tenantB);

        Account tenantBAccount = new Account();
        tenantBAccount.setName("Trainer B");
        tenantBAccount.setEmail("trainer-b-" + java.util.UUID.randomUUID() + "@test.com");
        tenantBAccount.setPasswordHash(passwordEncoder.encode("pass"));
        accountRepository.save(tenantBAccount);
        User trainerB = new User();
        trainerB.setAccount(tenantBAccount);
        trainerB.setRole(Role.TRAINER);
        trainerB.setTenant(tenantB);
        trainerB.setActive(true);
        userRepo.save(trainerB);

        var loginResult = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new LoginRequest(tenantBAccount.getEmail(), "pass"))))
                .andReturn();
        String trainerBToken = mapper.readTree(loginResult.getResponse().getContentAsString())
                .get("token").asText();

        mvc.perform(get("/api/exercises")
                        .header("Authorization", "Bearer " + trainerBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
