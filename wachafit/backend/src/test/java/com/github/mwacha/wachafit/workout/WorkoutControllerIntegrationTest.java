package com.github.mwacha.wachafit.workout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mwacha.wachafit.auth.dto.LoginRequest;
import com.github.mwacha.wachafit.exercise.Exercise;
import com.github.mwacha.wachafit.exercise.ExerciseRepository;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import com.github.mwacha.wachafit.workout.dto.*;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class WorkoutControllerIntegrationTest {

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
    @Autowired ExerciseRepository exerciseRepo;
    @Autowired WorkoutLogRepository logRepo;
    @Autowired PersonalRecordRepository prRepo;
    @Autowired WorkoutPlanRepository planRepo;
    @Autowired PasswordEncoder passwordEncoder;

    private UUID studentId;
    private UUID exerciseId;
    private String trainerToken;
    private String studentToken;

    @BeforeEach
    void setUp() throws Exception {
        // Delete in FK-safe order: children before parents
        prRepo.deleteAll();
        logRepo.deleteAll();
        planRepo.deleteAll();
        exerciseRepo.deleteAll();
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

        Exercise ex = new Exercise();
        ex.setName("Squat");
        ex.setMuscleGroup("legs");
        exerciseRepo.save(ex);
        exerciseId = ex.getId();

        var r = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new LoginRequest("t@t.com", "pass"))))
            .andReturn();
        trainerToken = mapper.readTree(r.getResponse().getContentAsString()).get("token").asText();

        var r2 = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new LoginRequest("s@t.com", "pass"))))
            .andReturn();
        studentToken = mapper.readTree(r2.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void createPlan_withTrainerToken_shouldReturn201() throws Exception {
        CreateWorkoutPlanRequest req = new CreateWorkoutPlanRequest("Plan A", null,
            List.of(new WorkoutPlanItemRequest(exerciseId, "A", 3, "12", null, 60, 1, null)));
        mvc.perform(post("/api/students/" + studentId + "/workout-plans")
                .header("Authorization", "Bearer " + trainerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Plan A"))
            .andExpect(jsonPath("$.items[0].sets").value(3));
    }

    @Test
    void createPlan_withStudentToken_shouldReturn403() throws Exception {
        CreateWorkoutPlanRequest req = new CreateWorkoutPlanRequest("Plan B", null, List.of());
        mvc.perform(post("/api/students/" + studentId + "/workout-plans")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());
    }

    @Test
    void createLog_withStudentToken_shouldReturn201() throws Exception {
        CreateWorkoutLogRequest req = new CreateWorkoutLogRequest(
            exerciseId, null, LocalDate.now(), 3, 12, null, null);
        mvc.perform(post("/api/students/" + studentId + "/workout-logs")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated());
    }

    @Test
    void createLog_withStudentToken_forOtherStudent_shouldReturn403() throws Exception {
        UUID otherId = UUID.randomUUID();
        CreateWorkoutLogRequest req = new CreateWorkoutLogRequest(
            exerciseId, null, LocalDate.now(), 3, 12, null, null);
        mvc.perform(post("/api/students/" + otherId + "/workout-logs")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());
    }

    @Test
    void listRecords_withStudentToken_shouldReturn200() throws Exception {
        mvc.perform(get("/api/students/" + studentId + "/records")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk());
    }

    @Test
    void createLog_withLoad_shouldCreatePersonalRecord() throws Exception {
        CreateWorkoutLogRequest req = new CreateWorkoutLogRequest(
            exerciseId, null, LocalDate.now(), 3, 12, java.math.BigDecimal.valueOf(100), null);
        mvc.perform(post("/api/students/" + studentId + "/workout-logs")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated());

        mvc.perform(get("/api/students/" + studentId + "/records")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].recordLoadKg").value(100));
    }

    @Test
    void activatePlan_shouldDeactivateOthersAndReturn200() throws Exception {
        // Create two plans
        CreateWorkoutPlanRequest req1 = new CreateWorkoutPlanRequest("Plan 1", null, List.of());
        var r1 = mvc.perform(post("/api/students/" + studentId + "/workout-plans")
                .header("Authorization", "Bearer " + trainerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req1)))
            .andExpect(status().isCreated())
            .andReturn();
        UUID plan1Id = UUID.fromString(
            mapper.readTree(r1.getResponse().getContentAsString()).get("id").asText());

        CreateWorkoutPlanRequest req2 = new CreateWorkoutPlanRequest("Plan 2", null, List.of());
        var r2 = mvc.perform(post("/api/students/" + studentId + "/workout-plans")
                .header("Authorization", "Bearer " + trainerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req2)))
            .andExpect(status().isCreated())
            .andReturn();
        UUID plan2Id = UUID.fromString(
            mapper.readTree(r2.getResponse().getContentAsString()).get("id").asText());

        // Activate plan2 — plan1 should become inactive
        mvc.perform(patch("/api/workout-plans/" + plan2Id + "/activate")
                .header("Authorization", "Bearer " + trainerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(true));

        // Active plan for student should be plan2
        mvc.perform(get("/api/students/" + studentId + "/workout-plans/active")
                .header("Authorization", "Bearer " + trainerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(plan2Id.toString()));
    }
}
