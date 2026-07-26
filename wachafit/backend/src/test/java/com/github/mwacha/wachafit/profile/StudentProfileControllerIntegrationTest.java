package com.github.mwacha.wachafit.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mwacha.wachafit.account.Account;
import com.github.mwacha.wachafit.auth.dto.LoginRequest;
import com.github.mwacha.wachafit.profile.dto.CreateStudentProfileRequest;
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
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Testcontainers @ActiveProfiles("test")
class StudentProfileControllerIntegrationTest {

    @Container static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16");

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
    @Autowired StudentProfileRepository profileRepo;
    @Autowired StudentHealthRepository healthRepo;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired com.github.mwacha.wachafit.account.AccountRepository accountRepository;
    @Autowired com.github.mwacha.wachafit.tenant.TenantRepository tenantRepository;

    private String adminToken;
    private UUID studentId;

    @BeforeEach
    void setUp() throws Exception {
        healthRepo.deleteAll();
        profileRepo.deleteAll();
        userRepo.deleteAll();
        accountRepository.deleteAll();
        var tenant = tenantRepository.findBySlug("personal-studio").orElseThrow();
        Account adminAccount = new Account();
        adminAccount.setName("Admin"); adminAccount.setEmail("admin@t.com");
        adminAccount.setPasswordHash(passwordEncoder.encode("pass"));
        accountRepository.save(adminAccount);
        User admin = new User();
        admin.setAccount(adminAccount); admin.setRole(Role.ADMIN);
        admin.setTenant(tenant); admin.setActive(true);
        userRepo.save(admin);
        Account studentAccount = new Account();
        studentAccount.setName("Student"); studentAccount.setEmail("student@t.com");
        studentAccount.setPasswordHash(passwordEncoder.encode("pass"));
        accountRepository.save(studentAccount);
        User student = new User();
        student.setAccount(studentAccount); student.setRole(Role.STUDENT);
        student.setTenant(tenant); student.setActive(true);
        userRepo.save(student);
        studentId = student.getId();
        var r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(new LoginRequest("admin@t.com", "pass", "personal-studio")))).andReturn();
        adminToken = mapper.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void createProfile_withAdminToken_shouldReturn201() throws Exception {
        CreateStudentProfileRequest req = new CreateStudentProfileRequest(
            "123.456.789-00", null, null, null, null, null, "11999999999",
            null, null, null, null, null, null, null, null, null, null);
        mvc.perform(post("/api/students/" + studentId + "/profile")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.cpf").value("123.456.789-00"));
    }

    @Test
    void createProfile_withoutToken_shouldReturn401() throws Exception {
        mvc.perform(post("/api/students/" + UUID.randomUUID() + "/profile")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getProfile_withAdminToken_shouldReturn200() throws Exception {
        mvc.perform(get("/api/students/" + studentId + "/profile")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());
    }
}
