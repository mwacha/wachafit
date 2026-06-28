package com.github.mwacha.wachafit.assessment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mwacha.wachafit.assessment.dto.CreateAssessmentRequest;
import com.github.mwacha.wachafit.assessment.dto.MeasurementRequest;
import com.github.mwacha.wachafit.auth.dto.LoginRequest;
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
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AssessmentControllerIntegrationTest {

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
    @Autowired PhysicalAssessmentRepository assessmentRepo;
    @Autowired PasswordEncoder passwordEncoder;

    private String trainerToken;
    private UUID studentId;

    @BeforeEach
    void setUp() throws Exception {
        assessmentRepo.deleteAll();
        userRepo.deleteAll();

        User trainer = new User();
        trainer.setName("Trainer");
        trainer.setEmail("trainer@test.com");
        trainer.setPasswordHash(passwordEncoder.encode("pass123"));
        trainer.setRole(Role.TRAINER);
        trainer.setActive(true);
        userRepo.save(trainer);

        User student = new User();
        student.setName("Student");
        student.setEmail("student@test.com");
        student.setPasswordHash(passwordEncoder.encode("pass123"));
        student.setRole(Role.STUDENT);
        student.setActive(true);
        userRepo.save(student);
        studentId = student.getId();

        var loginRes = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new LoginRequest("trainer@test.com", "pass123"))))
            .andReturn().getResponse().getContentAsString();
        trainerToken = mapper.readTree(loginRes).get("token").asText();
    }

    @Test
    void create_withTrainerToken_shouldReturn201() throws Exception {
        CreateAssessmentRequest req = new CreateAssessmentRequest(
            LocalDate.now(), new BigDecimal("75.0"), new BigDecimal("175.0"),
            new BigDecimal("18.0"), new BigDecimal("24.5"), null,
            List.of(new MeasurementRequest("waist", new BigDecimal("80.0")))
        );

        mvc.perform(post("/api/students/" + studentId + "/assessments")
                .header("Authorization", "Bearer " + trainerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.studentId").value(studentId.toString()))
            .andExpect(jsonPath("$.measurements[0].bodyPart").value("waist"));
    }

    @Test
    void create_withoutToken_shouldReturn401() throws Exception {
        mvc.perform(post("/api/students/" + UUID.randomUUID() + "/assessments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void list_withTrainerToken_shouldReturn200() throws Exception {
        mvc.perform(get("/api/students/" + studentId + "/assessments")
                .header("Authorization", "Bearer " + trainerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }
}
