package com.github.mwacha.wachafit.membership;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mwacha.wachafit.account.Account;
import com.github.mwacha.wachafit.auth.dto.LoginRequest;
import com.github.mwacha.wachafit.billing.PaymentChargeRepository;
import com.github.mwacha.wachafit.membership.dto.CreatePlanRequest;
import com.github.mwacha.wachafit.membership.dto.CreateSubscriptionRequest;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Testcontainers @ActiveProfiles("test")
class MembershipControllerIntegrationTest {

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
    @Autowired MembershipPlanRepository planRepo;
    @Autowired MemberSubscriptionRepository subscriptionRepo;
    @Autowired PaymentChargeRepository chargeRepo;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired com.github.mwacha.wachafit.account.AccountRepository accountRepository;
    @Autowired com.github.mwacha.wachafit.tenant.TenantRepository tenantRepository;

    private String adminToken;
    private UUID studentId;
    private UUID planId;

    @BeforeEach
    void setUp() throws Exception {
        chargeRepo.deleteAll();
        subscriptionRepo.deleteAll();
        planRepo.deleteAll();
        userRepo.deleteAll();
        accountRepository.deleteAll();

        var tenant = tenantRepository.findBySlug("personal-studio").orElseThrow();

        Account adminAccount = new Account();
        adminAccount.setName("Admin"); adminAccount.setEmail("admin@t.com");
        adminAccount.setPasswordHash(passwordEncoder.encode("pass"));
        accountRepository.save(adminAccount);
        User admin = new User();
        admin.setAccount(adminAccount);
        admin.setRole(Role.ADMIN); admin.setTenant(tenant); admin.setActive(true);
        userRepo.save(admin);

        Account studentAccount = new Account();
        studentAccount.setName("Student"); studentAccount.setEmail("student@t.com");
        studentAccount.setPasswordHash(passwordEncoder.encode("pass"));
        accountRepository.save(studentAccount);
        User student = new User();
        student.setAccount(studentAccount);
        student.setRole(Role.STUDENT); student.setTenant(tenant); student.setActive(true);
        userRepo.save(student);
        studentId = student.getId();

        var loginResult = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(new LoginRequest("admin@t.com", "pass")))).andReturn();
        adminToken = mapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();

        var planResult = mvc.perform(post("/api/membership-plans")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CreatePlanRequest("Plano Básico", null, 1, new BigDecimal("99.90"), null))))
            .andReturn();
        planId = UUID.fromString(mapper.readTree(planResult.getResponse().getContentAsString()).get("id").asText());
    }

    @Test
    void createSubscription_withAdminToken_shouldReturn201AndGenerateCharge() throws Exception {
        CreateSubscriptionRequest req = new CreateSubscriptionRequest(planId, LocalDate.of(2026, 7, 1));
        mvc.perform(post("/api/students/" + studentId + "/subscription")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.planName").value("Plano Básico"))
            .andExpect(jsonPath("$.expiresAt").value("2026-08-01"));

        assert chargeRepo.count() == 1;
    }

    @Test
    void createSubscription_whenAlreadyHasActive_shouldReturn409() throws Exception {
        CreateSubscriptionRequest req = new CreateSubscriptionRequest(planId, LocalDate.of(2026, 7, 1));
        mvc.perform(post("/api/students/" + studentId + "/subscription")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated());

        mvc.perform(post("/api/students/" + studentId + "/subscription")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isConflict());
    }

    @Test
    void cancelSubscription_shouldReturnNoContent() throws Exception {
        CreateSubscriptionRequest req = new CreateSubscriptionRequest(planId, LocalDate.of(2026, 7, 1));
        mvc.perform(post("/api/students/" + studentId + "/subscription")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated());

        mvc.perform(delete("/api/students/" + studentId + "/subscription")
                .header("Authorization", "Bearer " + adminToken)
                .param("reason", "Cancelamento teste"))
            .andExpect(status().isNoContent());
    }

    @Test
    void getSubscription_whenStudent_shouldOnlySeeOwn() throws Exception {
        CreateSubscriptionRequest req = new CreateSubscriptionRequest(planId, LocalDate.of(2026, 7, 1));
        mvc.perform(post("/api/students/" + studentId + "/subscription")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated());

        var studentLogin = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(new LoginRequest("student@t.com", "pass")))).andReturn();
        String studentToken = mapper.readTree(studentLogin.getResponse().getContentAsString()).get("token").asText();

        mvc.perform(get("/api/students/" + studentId + "/subscription")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
