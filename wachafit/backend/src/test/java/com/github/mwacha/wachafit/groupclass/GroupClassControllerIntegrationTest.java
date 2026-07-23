package com.github.mwacha.wachafit.groupclass;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mwacha.wachafit.auth.dto.LoginRequest;
import com.github.mwacha.wachafit.auth.dto.RegisterRequest;
import com.github.mwacha.wachafit.groupclass.dto.CreateGroupClassRequest;
import com.github.mwacha.wachafit.groupclass.dto.UpdateGroupClassRequest;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class GroupClassControllerIntegrationTest {

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

    private String studentToken;
    private UUID trainerId;

    @BeforeEach
    void setup() throws Exception {
        // Register a trainer (will be STUDENT by default — promote manually in DB)
        var trainerEmail = "trainer-" + UUID.randomUUID() + "@test.com";
        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new RegisterRequest("Trainer User", trainerEmail, "password123"))))
            .andReturn();

        // Promote to TRAINER role directly via repository
        var trainerUser = userRepository.findByEmail(trainerEmail).orElseThrow();
        trainerUser.setRole(Role.TRAINER);
        userRepository.save(trainerUser);
        trainerId = trainerUser.getId();

        // Register a STUDENT and get token
        var studentEmail = "student-" + UUID.randomUUID() + "@test.com";
        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new RegisterRequest("Student User", studentEmail, "password123"))))
            .andReturn();

        var result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new LoginRequest(studentEmail, "password123"))))
            .andReturn();
        var body = objectMapper.readTree(result.getResponse().getContentAsString());
        studentToken = body.get("token").asText();
    }

    @Test
    void list_withoutToken_shouldReturn200() throws Exception {
        // GET /api/classes is public
        mockMvc.perform(get("/api/classes"))
            .andExpect(status().isOk());
    }

    @Test
    void list_withActiveFilter_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/classes?active=true"))
            .andExpect(status().isOk());
    }

    @Test
    void create_withStudentToken_shouldReturn403() throws Exception {
        var req = new CreateGroupClassRequest("Yoga", null, 10, 60, trainerId, "FLEX", null, null, null);
        mockMvc.perform(post("/api/classes")
            .header("Authorization", "Bearer " + studentToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());
    }

    @Test
    void create_withTrainerToken_shouldReturn201() throws Exception {
        // Login as trainer
        var trainerEmail = "trainer-" + UUID.randomUUID() + "@test.com";
        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new RegisterRequest("Trainer User", trainerEmail, "password123"))))
            .andReturn();

        var trainerUser = userRepository.findByEmail(trainerEmail).orElseThrow();
        trainerUser.setRole(Role.TRAINER);
        userRepository.save(trainerUser);

        var result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new LoginRequest(trainerEmail, "password123"))))
            .andReturn();
        var body = objectMapper.readTree(result.getResponse().getContentAsString());
        String trainerToken = body.get("token").asText();

        // Create class and assert 201
        var req = new CreateGroupClassRequest("Yoga", null, 10, 60, trainerUser.getId(), "FLEX", null, null, null);
        mockMvc.perform(post("/api/classes")
            .header("Authorization", "Bearer " + trainerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated());
    }

    @Test
    void create_withoutToken_shouldReturn401() throws Exception {
        var req = new CreateGroupClassRequest("Yoga", null, 10, 60, trainerId, "FLEX", null, null, null);
        mockMvc.perform(post("/api/classes")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deactivate_withStudentToken_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/classes/" + UUID.randomUUID())
            .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void update_withStudentToken_shouldReturn403() throws Exception {
        var req = new UpdateGroupClassRequest("Updated", null, 5, 30, null, "FLEX", null, null, null);
        mockMvc.perform(put("/api/classes/" + UUID.randomUUID())
            .header("Authorization", "Bearer " + studentToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());
    }
}
