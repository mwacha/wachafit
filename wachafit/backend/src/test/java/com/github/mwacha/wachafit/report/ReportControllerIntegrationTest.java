package com.github.mwacha.wachafit.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mwacha.wachafit.auth.dto.LoginRequest;
import com.github.mwacha.wachafit.billing.PaymentCharge;
import com.github.mwacha.wachafit.billing.PaymentChargeRepository;
import com.github.mwacha.wachafit.membership.MemberSubscription;
import com.github.mwacha.wachafit.membership.MemberSubscriptionRepository;
import com.github.mwacha.wachafit.membership.MembershipPlan;
import com.github.mwacha.wachafit.membership.MembershipPlanRepository;
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
import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Testcontainers @ActiveProfiles("test")
class ReportControllerIntegrationTest {

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
        r.add("app.payment.gateway", () -> "manual");
        r.add("app.payment.suspend-after-days", () -> "5");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository userRepo;
    @Autowired MembershipPlanRepository planRepo;
    @Autowired MemberSubscriptionRepository subscriptionRepo;
    @Autowired PaymentChargeRepository chargeRepo;
    @Autowired PasswordEncoder passwordEncoder;

    private String adminToken;
    private String cashierToken;

    @BeforeEach
    void setUp() throws Exception {
        chargeRepo.deleteAll();
        subscriptionRepo.deleteAll();
        planRepo.deleteAll();
        userRepo.deleteAll();

        User admin = new User();
        admin.setName("Admin"); admin.setEmail("admin@r.com");
        admin.setPasswordHash(passwordEncoder.encode("pass"));
        admin.setRole(Role.ADMIN); admin.setActive(true);
        userRepo.save(admin);

        User cashier = new User();
        cashier.setName("Caixa"); cashier.setEmail("cashier@r.com");
        cashier.setPasswordHash(passwordEncoder.encode("pass"));
        cashier.setRole(Role.CASHIER); cashier.setActive(true);
        userRepo.save(cashier);

        User student = new User();
        student.setName("Aluno"); student.setEmail("student@r.com");
        student.setPasswordHash(passwordEncoder.encode("pass"));
        student.setRole(Role.STUDENT); student.setActive(true);
        userRepo.save(student);

        MembershipPlan plan = new MembershipPlan();
        plan.setName("Plano"); plan.setDurationMonths(1);
        plan.setPrice(new BigDecimal("100.00")); plan.setActive(true);
        planRepo.save(plan);

        MemberSubscription sub = new MemberSubscription();
        sub.setStudentId(student.getId()); sub.setPlanId(plan.getId());
        sub.setStatus("ACTIVE"); sub.setStartedAt(LocalDate.now());
        sub.setExpiresAt(LocalDate.now().plusMonths(1));
        sub.setCreatedBy(admin.getId());
        subscriptionRepo.save(sub);

        PaymentCharge charge = new PaymentCharge();
        charge.setStudentId(student.getId()); charge.setSubscriptionId(sub.getId());
        charge.setAmount(new BigDecimal("100.00")); charge.setDueDate(LocalDate.now());
        charge.setStatus("PAID"); charge.setPaidAt(OffsetDateTime.now());
        chargeRepo.save(charge);

        adminToken = extractToken("admin@r.com");
        cashierToken = extractToken("cashier@r.com");
    }

    private String extractToken(String email) throws Exception {
        var result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(new LoginRequest(email, "pass")))).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void revenue_shouldReturn200WithResults() throws Exception {
        String from = LocalDate.now().getYear() + "-" + String.format("%02d", LocalDate.now().getMonthValue());
        mvc.perform(get("/api/reports/revenue")
                .param("from", from).param("to", from)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].total").value(100.00))
            .andExpect(jsonPath("$[0].chargesCount").value(1));
    }

    @Test
    void overdue_shouldReturn200WithEmptyList_whenNoOverdueCharges() throws Exception {
        mvc.perform(get("/api/reports/overdue")
                .header("Authorization", "Bearer " + cashierToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void subscriptions_shouldReturn200WithStats() throws Exception {
        mvc.perform(get("/api/reports/subscriptions")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").isNumber())
            .andExpect(jsonPath("$.suspended").isNumber())
            .andExpect(jsonPath("$.cancelled").isNumber())
            .andExpect(jsonPath("$.expired").isNumber());
    }

    @Test
    void trainerCommissions_shouldReturn200WithEmptyList_whenNoTrainers() throws Exception {
        mvc.perform(get("/api/reports/trainer-commissions")
                .param("from", LocalDate.now().minusMonths(1).toString())
                .param("to", LocalDate.now().toString())
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void cashFlow_shouldReturn200WithDailyAggregates() throws Exception {
        mvc.perform(get("/api/reports/cash-flow")
                .param("from", LocalDate.now().withDayOfMonth(1).toString())
                .param("to", LocalDate.now().toString())
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].received").value(100.00));
    }

    @Test
    void revenue_shouldReturn403_whenStudentAccesses() throws Exception {
        String studentToken = extractToken("student@r.com");
        String from = LocalDate.now().getYear() + "-" + String.format("%02d", LocalDate.now().getMonthValue());
        mvc.perform(get("/api/reports/revenue")
                .param("from", from).param("to", from)
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void subscriptions_shouldReturn403_whenCashierAccesses() throws Exception {
        mvc.perform(get("/api/reports/subscriptions")
                .header("Authorization", "Bearer " + cashierToken))
            .andExpect(status().isForbidden());
    }
}
