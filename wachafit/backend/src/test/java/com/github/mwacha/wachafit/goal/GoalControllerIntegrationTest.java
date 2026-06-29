package com.github.mwacha.wachafit.goal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mwacha.wachafit.auth.dto.LoginRequest;
import com.github.mwacha.wachafit.goal.dto.CreateGoalRequest;
import com.github.mwacha.wachafit.goal.dto.UpdateGoalStatusRequest;
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

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class GoalControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository userRepo;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired StudentGoalRepository goalRepo;

    private String trainerToken;
    private String studentToken;
    private UUID studentId;

    @BeforeEach
    void setUp() throws Exception {
        goalRepo.deleteAll();
        userRepo.deleteAll();

        User trainer = new User();
        trainer.setName("T");
        trainer.setEmail("t@t.com");
        trainer.setPasswordHash(passwordEncoder.encode("pass"));
        trainer.setRole(Role.TRAINER);
        trainer.setActive(true);
        userRepo.save(trainer);

        User student = new User();
        student.setName("S");
        student.setEmail("s@t.com");
        student.setPasswordHash(passwordEncoder.encode("pass"));
        student.setRole(Role.STUDENT);
        student.setActive(true);
        userRepo.save(student);
        studentId = student.getId();

        var r = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new LoginRequest("t@t.com", "pass"))))
            .andReturn();
        trainerToken = mapper.readTree(r.getResponse().getContentAsString()).get("token").asText();

        var sr = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new LoginRequest("s@t.com", "pass"))))
            .andReturn();
        studentToken = mapper.readTree(sr.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void create_shouldReturn201() throws Exception {
        mvc.perform(post("/api/students/" + studentId + "/goals")
                .header("Authorization", "Bearer " + trainerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(
                    new CreateGoalRequest("Lose 5kg", "weight", null, null))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void list_withoutToken_shouldReturn401() throws Exception {
        mvc.perform(get("/api/students/" + studentId + "/goals"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void list_withTrainerToken_shouldReturn200() throws Exception {
        // seed a goal first
        mvc.perform(post("/api/students/" + studentId + "/goals")
                .header("Authorization", "Bearer " + trainerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(
                    new CreateGoalRequest("Run 5km", "distance", null, null))))
            .andExpect(status().isCreated());

        mvc.perform(get("/api/students/" + studentId + "/goals")
                .header("Authorization", "Bearer " + trainerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].description").value("Run 5km"));
    }

    @Test
    void updateStatus_withTrainerToken_shouldReturn200() throws Exception {
        // create goal
        var createResult = mvc.perform(post("/api/students/" + studentId + "/goals")
                .header("Authorization", "Bearer " + trainerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(
                    new CreateGoalRequest("Lose 5kg", "weight", null, null))))
            .andExpect(status().isCreated())
            .andReturn();

        UUID goalId = UUID.fromString(
            mapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(patch("/api/goals/" + goalId + "/status")
                .header("Authorization", "Bearer " + trainerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new UpdateGoalStatusRequest("ACHIEVED"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACHIEVED"));
    }

    @Test
    void updateStatus_withStudentToken_shouldReturn403() throws Exception {
        // create goal first
        var createResult = mvc.perform(post("/api/students/" + studentId + "/goals")
                .header("Authorization", "Bearer " + trainerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(
                    new CreateGoalRequest("Lose 5kg", "weight", null, null))))
            .andExpect(status().isCreated())
            .andReturn();

        UUID goalId = UUID.fromString(
            mapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(patch("/api/goals/" + goalId + "/status")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new UpdateGoalStatusRequest("ACHIEVED"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void delete_withTrainerToken_shouldReturn204() throws Exception {
        var createResult = mvc.perform(post("/api/students/" + studentId + "/goals")
                .header("Authorization", "Bearer " + trainerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(
                    new CreateGoalRequest("Lose 5kg", "weight", null, null))))
            .andExpect(status().isCreated())
            .andReturn();

        UUID goalId = UUID.fromString(
            mapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(delete("/api/goals/" + goalId)
                .header("Authorization", "Bearer " + trainerToken))
            .andExpect(status().isNoContent());
    }

    @Test
    void create_withNonExistentStudent_shouldReturn404() throws Exception {
        mvc.perform(post("/api/students/" + UUID.randomUUID() + "/goals")
                .header("Authorization", "Bearer " + trainerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(
                    new CreateGoalRequest("Lose 5kg", "weight", null, null))))
            .andExpect(status().isNotFound());
    }
}
