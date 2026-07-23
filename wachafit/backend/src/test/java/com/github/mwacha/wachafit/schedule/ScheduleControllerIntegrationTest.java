package com.github.mwacha.wachafit.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mwacha.wachafit.auth.dto.LoginRequest;
import com.github.mwacha.wachafit.auth.dto.RegisterRequest;
import com.github.mwacha.wachafit.schedule.dto.ScheduleRequest;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class ScheduleControllerIntegrationTest {

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

    private String trainerToken;
    private String studentToken;
    private UUID trainerId;

    @BeforeEach
    void setup() throws Exception {
        // Register and promote a TRAINER
        var trainerEmail = "sched-trainer-" + UUID.randomUUID() + "@test.com";
        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new RegisterRequest("Trainer", trainerEmail, "password123", "personal-studio"))))
            .andReturn();

        var trainerUser = userRepository.findByEmail(trainerEmail).orElseThrow();
        trainerUser.setRole(Role.TRAINER);
        userRepository.save(trainerUser);
        trainerId = trainerUser.getId();

        var loginResult = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new LoginRequest(trainerEmail, "password123", "personal-studio"))))
            .andReturn();
        var loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        trainerToken = loginBody.get("token").asText();

        // Register a STUDENT and get token
        var studentEmail = "sched-student-" + UUID.randomUUID() + "@test.com";
        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new RegisterRequest("Student", studentEmail, "password123", "personal-studio"))))
            .andReturn();

        var studentResult = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new LoginRequest(studentEmail, "password123", "personal-studio"))))
            .andReturn();
        var studentBody = objectMapper.readTree(studentResult.getResponse().getContentAsString());
        studentToken = studentBody.get("token").asText();
    }

    @Test
    void list_withoutToken_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/schedules"))
            .andExpect(status().isOk());
    }

    @Test
    void create_withStudentToken_shouldReturn403() throws Exception {
        OffsetDateTime start = OffsetDateTime.of(2026, 8, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        var req = new ScheduleRequest(null, trainerId, ScheduleType.PERSONAL, start, start.plusHours(1));

        mockMvc.perform(post("/api/schedules")
            .header("Authorization", "Bearer " + studentToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());
    }

    @Test
    void create_withoutToken_shouldReturn401() throws Exception {
        OffsetDateTime start = OffsetDateTime.of(2026, 8, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        var req = new ScheduleRequest(null, trainerId, ScheduleType.PERSONAL, start, start.plusHours(1));

        mockMvc.perform(post("/api/schedules")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void create_withTrainerToken_shouldReturn201() throws Exception {
        OffsetDateTime start = OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        var req = new ScheduleRequest(null, trainerId, ScheduleType.PERSONAL, start, start.plusHours(1));

        mockMvc.perform(post("/api/schedules")
            .header("Authorization", "Bearer " + trainerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.status").value("OPEN"))
            .andExpect(jsonPath("$.type").value("PERSONAL"));
    }

    @Test
    void cancel_withTrainerToken_shouldReturn204() throws Exception {
        // First create a schedule
        OffsetDateTime start = OffsetDateTime.of(2026, 10, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        var req = new ScheduleRequest(null, trainerId, ScheduleType.PERSONAL, start, start.plusHours(1));

        var createResult = mockMvc.perform(post("/api/schedules")
            .header("Authorization", "Bearer " + trainerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andReturn();

        var body = objectMapper.readTree(createResult.getResponse().getContentAsString());
        var scheduleId = body.get("id").asText();

        // Cancel it via DELETE
        mockMvc.perform(delete("/api/schedules/" + scheduleId)
            .header("Authorization", "Bearer " + trainerToken))
            .andExpect(status().isNoContent());
    }

    @Test
    void cancel_withStudentToken_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/schedules/" + UUID.randomUUID())
            .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isForbidden());
    }
}
