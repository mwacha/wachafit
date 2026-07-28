package com.github.mwacha.wachafit.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mwacha.wachafit.account.Account;
import com.github.mwacha.wachafit.auth.dto.LoginRequest;
import com.github.mwacha.wachafit.billing.dto.CreateChargeRequest;
import com.github.mwacha.wachafit.billing.dto.ManualPaymentRequest;
import com.github.mwacha.wachafit.membership.MemberSubscription;
import com.github.mwacha.wachafit.membership.MemberSubscriptionRepository;
import com.github.mwacha.wachafit.membership.MembershipPlan;
import com.github.mwacha.wachafit.membership.MembershipPlanRepository;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class BillingControllerIntegrationTest {

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
    @Autowired PaymentChargeRepository chargeRepo;
    @Autowired MemberSubscriptionRepository subscriptionRepo;
    @Autowired MembershipPlanRepository planRepo;
    @Autowired UserRepository userRepo;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired com.github.mwacha.wachafit.account.AccountRepository accountRepository;
    @Autowired com.github.mwacha.wachafit.tenant.TenantRepository tenantRepository;

    private String adminToken;
    private String studentToken;
    private UUID studentId;
    private UUID chargeId;

    @BeforeEach
    void setUp() throws Exception {
        chargeRepo.deleteAll();
        subscriptionRepo.deleteAll();
        planRepo.deleteAll();
        userRepo.deleteAll();
        accountRepository.deleteAll();

        var tenant = tenantRepository.findBySlug("personal-studio").orElseThrow();
        // Testes que persistem entidades TenantAware diretamente (fora de uma requisição HTTP
        // autenticada, que é quem normalmente define isso via JwtFilter) precisam setar o
        // TenantContext manualmente antes de qualquer save() abaixo.
        com.github.mwacha.wachafit.tenant.TenantContext.set(tenant.getId());

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

        var loginResult = mvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(new LoginRequest("admin@t.com", "pass")))).andReturn();
        adminToken = mapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();

        var studentLoginResult = mvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(new LoginRequest("student@t.com", "pass")))).andReturn();
        studentToken = mapper.readTree(studentLoginResult.getResponse().getContentAsString()).get("token").asText();
        // JwtFilter limpa o TenantContext incondicionalmente ao fim de QUALQUER requisição
        // (autenticada ou não) — as chamadas de login acima já apagaram o valor setado no início
        // deste método, então precisa ser redefinido antes dos saves diretos abaixo.
        com.github.mwacha.wachafit.tenant.TenantContext.set(tenant.getId());

        MembershipPlan plan = new MembershipPlan();
        plan.setName("Plano Teste"); plan.setDurationMonths(1);
        plan.setPrice(new BigDecimal("99.90")); plan.setActive(true);
        planRepo.save(plan);

        MemberSubscription sub = new MemberSubscription();
        sub.setStudentId(studentId); sub.setPlanId(plan.getId());
        sub.setStatus("ACTIVE"); sub.setStartedAt(LocalDate.now());
        sub.setExpiresAt(LocalDate.now().plusMonths(1));
        sub.setCreatedBy(admin.getId());
        subscriptionRepo.save(sub);

        PaymentCharge charge = new PaymentCharge();
        charge.setStudentId(studentId); charge.setSubscriptionId(sub.getId());
        charge.setAmount(new BigDecimal("99.90")); charge.setDueDate(LocalDate.now());
        charge.setStatus("PENDING");
        chargeRepo.save(charge);
        chargeId = charge.getId();
    }

    @AfterEach
    void tearDown() {
        com.github.mwacha.wachafit.tenant.TenantContext.clear();
    }

    @Test
    void listCharges_shouldReturn200WithCharges() throws Exception {
        mvc.perform(get("/api/students/" + studentId + "/charges")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("PENDING"))
            .andExpect(jsonPath("$[0].amount").value(99.90));
    }

    @Test
    void createManualCharge_shouldReturn201() throws Exception {
        CreateChargeRequest req = new CreateChargeRequest(new BigDecimal("50.00"), LocalDate.now().plusDays(30));
        mvc.perform(post("/api/students/" + studentId + "/charges")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.amount").value(50.00));
    }

    @Test
    void payCharge_shouldReturn200WithPaidStatus() throws Exception {
        ManualPaymentRequest req = new ManualPaymentRequest("CASH");
        mvc.perform(patch("/api/charges/" + chargeId + "/pay")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PAID"))
            .andExpect(jsonPath("$.paymentMethod").value("CASH"));
    }

    @Test
    void payCharge_withStudentTokenAndPix_shouldReturn200() throws Exception {
        ManualPaymentRequest req = new ManualPaymentRequest("PIX");
        mvc.perform(patch("/api/charges/" + chargeId + "/pay")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PAID"))
            .andExpect(jsonPath("$.paymentMethod").value("PIX"));
    }

    @Test
    void payCharge_withStudentTokenAndCash_shouldReturn409() throws Exception {
        ManualPaymentRequest req = new ManualPaymentRequest("CASH");
        mvc.perform(patch("/api/charges/" + chargeId + "/pay")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isConflict());
    }

    @Test
    void cancelCharge_shouldReturn204() throws Exception {
        mvc.perform(patch("/api/charges/" + chargeId + "/cancel")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNoContent());
    }

    @Test
    void payCharge_alreadyPaid_shouldReturn409() throws Exception {
        ManualPaymentRequest req = new ManualPaymentRequest("PIX");
        mvc.perform(patch("/api/charges/" + chargeId + "/pay")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isOk());

        mvc.perform(patch("/api/charges/" + chargeId + "/pay")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isConflict());
    }

    @Test
    void webhook_withManualGateway_shouldReturn200() throws Exception {
        String payload = "{\"externalChargeId\":\"ext-123\",\"status\":\"PAID\"}";
        mvc.perform(post("/api/payments/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .header("x-signature", "any-value"))
            .andExpect(status().isOk());
    }
}
