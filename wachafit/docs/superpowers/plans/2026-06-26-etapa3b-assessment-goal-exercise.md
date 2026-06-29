# WachaFit Etapa 3B — Assessment + Goal + Exercise Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement backend modules for physical assessments, student goals, and the exercise library (T-15).

**Architecture:** Three independent package-by-feature modules. Each follows the established pattern: entity → repository → service (TDD) → controller → integration test. RN-11 visibility enforced in every service via `assertCanAccess(studentId, requestingUser)`.

**Tech Stack:** Java 21, Spring Boot 3, Spring Security, JPA/Hibernate, PostgreSQL 16, Flyway (V7–V10 already applied), Mockito, Testcontainers, JUnit 5

## Global Constraints

- Package root: `com.github.mwacha.wachafit`
- Worktree: `/Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit/`
- Git: `git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit`
- Maven: `cd backend && mvn ...`
- No Lombok — manual getters/setters
- Tests: `@ActiveProfiles("test")` on integration tests; Testcontainers Postgres
- TDD: commit failing tests first, then implementation
- RN-08: assessment and goal write endpoints restricted to TRAINER/ADMIN
- RN-11: student data visible only to owner + TRAINER/ADMIN; validated in service
- Existing shared exceptions: `NotFoundException`, `BusinessException`, `ForbiddenException`
- `@AuthenticationPrincipal com.github.mwacha.wachafit.user.User currentUser` in controllers

---

## Task 1: Assessment module (T-15)

**Files:**
- Create: `backend/src/main/java/com/github/mwacha/wachafit/assessment/AssessmentMeasurement.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/assessment/PhysicalAssessment.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/assessment/PhysicalAssessmentRepository.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/assessment/AssessmentService.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/assessment/AssessmentController.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/assessment/dto/MeasurementRequest.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/assessment/dto/MeasurementResponse.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/assessment/dto/CreateAssessmentRequest.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/assessment/dto/AssessmentResponse.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/assessment/dto/EvolutionPoint.java`
- Create: `backend/src/test/java/com/github/mwacha/wachafit/assessment/AssessmentServiceTest.java`
- Create: `backend/src/test/java/com/github/mwacha/wachafit/assessment/AssessmentControllerIntegrationTest.java`

**Interfaces:**
- Consumes: `User` entity (id, role), `UserRepository`, `NotFoundException`, `ForbiddenException`
- Produces:
  - `AssessmentService.create(UUID studentId, CreateAssessmentRequest, UUID assessedById) → AssessmentResponse`
  - `AssessmentService.list(UUID studentId, User requestingUser) → List<AssessmentResponse>`
  - `AssessmentService.evolution(UUID studentId, User requestingUser) → List<EvolutionPoint>`
  - `AssessmentService.update(UUID id, CreateAssessmentRequest, User requestingUser) → AssessmentResponse`

- [ ] **Step 1: Write failing unit tests**

```java
// backend/src/test/java/com/github/mwacha/wachafit/assessment/AssessmentServiceTest.java
package com.github.mwacha.wachafit.assessment;

import com.github.mwacha.wachafit.assessment.dto.*;
import com.github.mwacha.wachafit.shared.exception.ForbiddenException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceTest {

    @Mock PhysicalAssessmentRepository repo;
    @Mock UserRepository userRepo;
    @InjectMocks AssessmentService service;

    private User trainer;
    private User student;
    private UUID studentId;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        student = new User();
        student.setRole(Role.STUDENT);
        try { var f = User.class.getDeclaredField("id"); f.setAccessible(true); f.set(student, studentId); }
        catch (Exception e) { throw new RuntimeException(e); }

        trainer = new User();
        trainer.setRole(Role.TRAINER);
        try { var f = User.class.getDeclaredField("id"); f.setAccessible(true); f.set(trainer, UUID.randomUUID()); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void create_shouldPersistAssessmentWithMeasurements() {
        when(userRepo.findById(studentId)).thenReturn(Optional.of(student));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateAssessmentRequest req = new CreateAssessmentRequest(
            LocalDate.now(), new BigDecimal("75.5"), new BigDecimal("175.0"),
            new BigDecimal("18.5"), new BigDecimal("24.7"), "notes",
            List.of(new MeasurementRequest("waist", new BigDecimal("80.0")))
        );

        AssessmentResponse res = service.create(studentId, req, trainer.getId());
        assertThat(res.studentId()).isEqualTo(studentId);
        assertThat(res.measurements()).hasSize(1);
        assertThat(res.measurements().get(0).bodyPart()).isEqualTo("waist");
    }

    @Test
    void create_shouldThrowNotFound_whenStudentMissing() {
        when(userRepo.findById(studentId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(studentId,
            new CreateAssessmentRequest(LocalDate.now(), null, null, null, null, null, List.of()),
            trainer.getId()))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void list_shouldThrowForbidden_whenStudentAccessesOtherStudentData() {
        User otherStudent = new User();
        otherStudent.setRole(Role.STUDENT);
        try { var f = User.class.getDeclaredField("id"); f.setAccessible(true); f.set(otherStudent, UUID.randomUUID()); }
        catch (Exception e) { throw new RuntimeException(e); }

        assertThatThrownBy(() -> service.list(studentId, otherStudent))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void list_shouldSucceed_whenStudentAccessesOwnData() {
        when(repo.findByStudentIdOrderByAssessedAtAsc(studentId)).thenReturn(List.of());
        assertThatNoException().isThrownBy(() -> service.list(studentId, student));
    }

    @Test
    void list_shouldSucceed_whenTrainerAccessesStudentData() {
        when(repo.findByStudentIdOrderByAssessedAtAsc(studentId)).thenReturn(List.of());
        assertThatNoException().isThrownBy(() -> service.list(studentId, trainer));
    }
}
```

- [ ] **Step 2: Run tests — confirm they fail**

```bash
cd /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit/backend
mvn test -Dtest=AssessmentServiceTest 2>&1 | tail -5
```

Expected: compilation error.

- [ ] **Step 3: Commit failing tests**

```bash
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit add backend/src/test/java/com/github/mwacha/wachafit/assessment/AssessmentServiceTest.java
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit commit -m "test: Assessment unit tests (failing)"
```

- [ ] **Step 4: Create DTOs**

```java
// dto/MeasurementRequest.java
package com.github.mwacha.wachafit.assessment.dto;
import java.math.BigDecimal;
public record MeasurementRequest(String bodyPart, BigDecimal valueCm) {}

// dto/MeasurementResponse.java
package com.github.mwacha.wachafit.assessment.dto;
import java.math.BigDecimal;
public record MeasurementResponse(String bodyPart, BigDecimal valueCm) {}

// dto/CreateAssessmentRequest.java
package com.github.mwacha.wachafit.assessment.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record CreateAssessmentRequest(
    LocalDate assessedAt,
    BigDecimal weightKg,
    BigDecimal heightCm,
    BigDecimal bodyFatPct,
    BigDecimal bmi,
    String notes,
    List<MeasurementRequest> measurements
) {}

// dto/AssessmentResponse.java
package com.github.mwacha.wachafit.assessment.dto;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
public record AssessmentResponse(
    UUID id, UUID studentId, UUID assessedBy, LocalDate assessedAt,
    BigDecimal weightKg, BigDecimal heightCm, BigDecimal bodyFatPct, BigDecimal bmi,
    String notes, List<MeasurementResponse> measurements, Instant createdAt
) {}

// dto/EvolutionPoint.java
package com.github.mwacha.wachafit.assessment.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
public record EvolutionPoint(LocalDate assessedAt, BigDecimal weightKg, BigDecimal bodyFatPct, BigDecimal bmi) {}
```

- [ ] **Step 5: Create AssessmentMeasurement entity**

```java
// AssessmentMeasurement.java
package com.github.mwacha.wachafit.assessment;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "assessment_measurements")
public class AssessmentMeasurement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false)
    private PhysicalAssessment assessment;

    @Column(name = "body_part", nullable = false, length = 40)
    private String bodyPart;

    @Column(name = "value_cm", nullable = false, precision = 5, scale = 2)
    private BigDecimal valueCm;

    public UUID getId() { return id; }
    public PhysicalAssessment getAssessment() { return assessment; }
    public void setAssessment(PhysicalAssessment assessment) { this.assessment = assessment; }
    public String getBodyPart() { return bodyPart; }
    public void setBodyPart(String bodyPart) { this.bodyPart = bodyPart; }
    public BigDecimal getValueCm() { return valueCm; }
    public void setValueCm(BigDecimal valueCm) { this.valueCm = valueCm; }
}
```

- [ ] **Step 6: Create PhysicalAssessment entity**

```java
// PhysicalAssessment.java
package com.github.mwacha.wachafit.assessment;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "physical_assessments")
public class PhysicalAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "assessed_by", nullable = false)
    private UUID assessedBy;

    @Column(name = "assessed_at", nullable = false)
    private LocalDate assessedAt;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "height_cm", precision = 5, scale = 2)
    private BigDecimal heightCm;

    @Column(name = "body_fat_pct", precision = 4, scale = 1)
    private BigDecimal bodyFatPct;

    @Column(precision = 4, scale = 1)
    private BigDecimal bmi;

    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "assessment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<AssessmentMeasurement> measurements = new ArrayList<>();

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID studentId) { this.studentId = studentId; }
    public UUID getAssessedBy() { return assessedBy; }
    public void setAssessedBy(UUID assessedBy) { this.assessedBy = assessedBy; }
    public LocalDate getAssessedAt() { return assessedAt; }
    public void setAssessedAt(LocalDate assessedAt) { this.assessedAt = assessedAt; }
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    public BigDecimal getHeightCm() { return heightCm; }
    public void setHeightCm(BigDecimal heightCm) { this.heightCm = heightCm; }
    public BigDecimal getBodyFatPct() { return bodyFatPct; }
    public void setBodyFatPct(BigDecimal bodyFatPct) { this.bodyFatPct = bodyFatPct; }
    public BigDecimal getBmi() { return bmi; }
    public void setBmi(BigDecimal bmi) { this.bmi = bmi; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public List<AssessmentMeasurement> getMeasurements() { return measurements; }
}
```

- [ ] **Step 7: Create PhysicalAssessmentRepository**

```java
// PhysicalAssessmentRepository.java
package com.github.mwacha.wachafit.assessment;

import com.github.mwacha.wachafit.assessment.dto.EvolutionPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PhysicalAssessmentRepository extends JpaRepository<PhysicalAssessment, UUID> {

    List<PhysicalAssessment> findByStudentIdOrderByAssessedAtAsc(UUID studentId);

    @Query("""
        SELECT new com.github.mwacha.wachafit.assessment.dto.EvolutionPoint(
            a.assessedAt, a.weightKg, a.bodyFatPct, a.bmi)
        FROM PhysicalAssessment a
        WHERE a.studentId = :studentId
        ORDER BY a.assessedAt ASC
    """)
    List<EvolutionPoint> findEvolutionByStudentId(@Param("studentId") UUID studentId);
}
```

- [ ] **Step 8: Create AssessmentService**

```java
// AssessmentService.java
package com.github.mwacha.wachafit.assessment;

import com.github.mwacha.wachafit.assessment.dto.*;
import com.github.mwacha.wachafit.shared.exception.ForbiddenException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AssessmentService {

    private final PhysicalAssessmentRepository repo;
    private final UserRepository userRepo;

    public AssessmentService(PhysicalAssessmentRepository repo, UserRepository userRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
    }

    public AssessmentResponse create(UUID studentId, CreateAssessmentRequest req, UUID assessedById) {
        userRepo.findById(studentId).orElseThrow(() -> new NotFoundException("Student not found"));
        PhysicalAssessment a = new PhysicalAssessment();
        a.setStudentId(studentId);
        a.setAssessedBy(assessedById);
        a.setAssessedAt(req.assessedAt());
        a.setWeightKg(req.weightKg());
        a.setHeightCm(req.heightCm());
        a.setBodyFatPct(req.bodyFatPct());
        a.setBmi(req.bmi());
        a.setNotes(req.notes());
        if (req.measurements() != null) {
            for (MeasurementRequest m : req.measurements()) {
                AssessmentMeasurement measurement = new AssessmentMeasurement();
                measurement.setBodyPart(m.bodyPart());
                measurement.setValueCm(m.valueCm());
                measurement.setAssessment(a);
                a.getMeasurements().add(measurement);
            }
        }
        return toResponse(repo.save(a));
    }

    @Transactional(readOnly = true)
    public List<AssessmentResponse> list(UUID studentId, User requestingUser) {
        assertCanAccess(studentId, requestingUser);
        return repo.findByStudentIdOrderByAssessedAtAsc(studentId).stream()
            .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EvolutionPoint> evolution(UUID studentId, User requestingUser) {
        assertCanAccess(studentId, requestingUser);
        return repo.findEvolutionByStudentId(studentId);
    }

    public AssessmentResponse update(UUID id, CreateAssessmentRequest req, User requestingUser) {
        PhysicalAssessment a = repo.findById(id)
            .orElseThrow(() -> new NotFoundException("Assessment not found"));
        assertCanAccess(a.getStudentId(), requestingUser);
        a.setAssessedAt(req.assessedAt());
        a.setWeightKg(req.weightKg());
        a.setHeightCm(req.heightCm());
        a.setBodyFatPct(req.bodyFatPct());
        a.setBmi(req.bmi());
        a.setNotes(req.notes());
        a.getMeasurements().clear();
        if (req.measurements() != null) {
            for (MeasurementRequest m : req.measurements()) {
                AssessmentMeasurement measurement = new AssessmentMeasurement();
                measurement.setBodyPart(m.bodyPart());
                measurement.setValueCm(m.valueCm());
                measurement.setAssessment(a);
                a.getMeasurements().add(measurement);
            }
        }
        return toResponse(repo.save(a));
    }

    private void assertCanAccess(UUID studentId, User requestingUser) {
        if (requestingUser.getRole() == Role.STUDENT && !studentId.equals(requestingUser.getId())) {
            throw new ForbiddenException("Access denied");
        }
    }

    private AssessmentResponse toResponse(PhysicalAssessment a) {
        List<MeasurementResponse> measurements = a.getMeasurements().stream()
            .map(m -> new MeasurementResponse(m.getBodyPart(), m.getValueCm()))
            .toList();
        return new AssessmentResponse(
            a.getId(), a.getStudentId(), a.getAssessedBy(), a.getAssessedAt(),
            a.getWeightKg(), a.getHeightCm(), a.getBodyFatPct(), a.getBmi(),
            a.getNotes(), measurements, a.getCreatedAt()
        );
    }
}
```

- [ ] **Step 9: Create AssessmentController**

```java
// AssessmentController.java
package com.github.mwacha.wachafit.assessment;

import com.github.mwacha.wachafit.assessment.dto.*;
import com.github.mwacha.wachafit.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class AssessmentController {

    private final AssessmentService service;

    public AssessmentController(AssessmentService service) {
        this.service = service;
    }

    @PostMapping("/api/students/{studentId}/assessments")
    @PreAuthorize("hasAnyRole('TRAINER','ADMIN')")
    public ResponseEntity<AssessmentResponse> create(
            @PathVariable UUID studentId,
            @RequestBody CreateAssessmentRequest req,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(service.create(studentId, req, currentUser.getId()));
    }

    @GetMapping("/api/students/{studentId}/assessments")
    @PreAuthorize("isAuthenticated()")
    public List<AssessmentResponse> list(
            @PathVariable UUID studentId,
            @AuthenticationPrincipal User currentUser) {
        return service.list(studentId, currentUser);
    }

    @GetMapping("/api/students/{studentId}/assessments/evolution")
    @PreAuthorize("isAuthenticated()")
    public List<EvolutionPoint> evolution(
            @PathVariable UUID studentId,
            @AuthenticationPrincipal User currentUser) {
        return service.evolution(studentId, currentUser);
    }

    @PutMapping("/api/assessments/{id}")
    @PreAuthorize("hasAnyRole('TRAINER','ADMIN')")
    public AssessmentResponse update(
            @PathVariable UUID id,
            @RequestBody CreateAssessmentRequest req,
            @AuthenticationPrincipal User currentUser) {
        return service.update(id, req, currentUser);
    }
}
```

- [ ] **Step 10: Write integration test**

```java
// backend/src/test/java/com/github/mwacha/wachafit/assessment/AssessmentControllerIntegrationTest.java
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
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository userRepo;
    @Autowired PasswordEncoder passwordEncoder;

    private String trainerToken;
    private UUID studentId;

    @BeforeEach
    void setUp() throws Exception {
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
```

- [ ] **Step 11: Run unit tests**

```bash
cd /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit/backend
mvn test -Dtest=AssessmentServiceTest 2>&1 | tail -8
```

Expected: `BUILD SUCCESS`, 5 tests passing.

- [ ] **Step 12: Run integration tests**

```bash
mvn test -Dtest=AssessmentControllerIntegrationTest 2>&1 | tail -8
```

Expected: `BUILD SUCCESS`, 3 tests passing.

- [ ] **Step 13: Run full suite**

```bash
mvn test 2>&1 | tail -5
```

Expected: `BUILD SUCCESS` (62 + 8 = 70+ tests).

- [ ] **Step 14: Commit**

```bash
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit add \
    backend/src/main/java/com/github/mwacha/wachafit/assessment/ \
    backend/src/test/java/com/github/mwacha/wachafit/assessment/AssessmentControllerIntegrationTest.java
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit commit -m "feat: Assessment module (T-15)"
```

---

## Task 2: Goal module (T-15)

**Files:**
- Create: `backend/src/main/java/com/github/mwacha/wachafit/goal/GoalStatus.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/goal/StudentGoal.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/goal/StudentGoalRepository.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/goal/GoalService.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/goal/GoalController.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/goal/dto/CreateGoalRequest.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/goal/dto/GoalResponse.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/goal/dto/UpdateGoalStatusRequest.java`
- Create: `backend/src/test/java/com/github/mwacha/wachafit/goal/GoalServiceTest.java`
- Create: `backend/src/test/java/com/github/mwacha/wachafit/goal/GoalControllerIntegrationTest.java`

**Interfaces:**
- Produces:
  - `GoalService.create(UUID studentId, CreateGoalRequest, UUID createdById) → GoalResponse`
  - `GoalService.list(UUID studentId, User requestingUser) → List<GoalResponse>`
  - `GoalService.updateStatus(UUID goalId, UpdateGoalStatusRequest, User requestingUser) → GoalResponse`

- [ ] **Step 1: Write failing unit tests**

```java
// backend/src/test/java/com/github/mwacha/wachafit/goal/GoalServiceTest.java
package com.github.mwacha.wachafit.goal;

import com.github.mwacha.wachafit.goal.dto.*;
import com.github.mwacha.wachafit.shared.exception.ForbiddenException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock StudentGoalRepository repo;
    @Mock UserRepository userRepo;
    @InjectMocks GoalService service;

    private User trainer;
    private User student;
    private UUID studentId;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        student = new User(); student.setRole(Role.STUDENT);
        try { var f = User.class.getDeclaredField("id"); f.setAccessible(true); f.set(student, studentId); }
        catch (Exception e) { throw new RuntimeException(e); }
        trainer = new User(); trainer.setRole(Role.TRAINER);
        try { var f = User.class.getDeclaredField("id"); f.setAccessible(true); f.set(trainer, UUID.randomUUID()); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void create_shouldPersistGoal() {
        when(userRepo.findById(studentId)).thenReturn(Optional.of(student));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        GoalResponse res = service.create(studentId, new CreateGoalRequest("Lose 5kg", "weight", null, null), trainer.getId());
        assertThat(res.description()).isEqualTo("Lose 5kg");
        assertThat(res.status()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void create_shouldThrowNotFound_whenStudentMissing() {
        when(userRepo.findById(studentId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(studentId, new CreateGoalRequest("d", null, null, null), trainer.getId()))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void list_shouldThrowForbidden_whenStudentAccessesOtherStudent() {
        User other = new User(); other.setRole(Role.STUDENT);
        try { var f = User.class.getDeclaredField("id"); f.setAccessible(true); f.set(other, UUID.randomUUID()); }
        catch (Exception e) { throw new RuntimeException(e); }
        assertThatThrownBy(() -> service.list(studentId, other)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateStatus_shouldThrowNotFound_whenGoalMissing() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateStatus(UUID.randomUUID(),
            new UpdateGoalStatusRequest("ACHIEVED"), trainer))
            .isInstanceOf(NotFoundException.class);
    }
}
```

- [ ] **Step 2: Run tests — confirm they fail**

```bash
mvn test -Dtest=GoalServiceTest 2>&1 | tail -5
```

Expected: compilation error.

- [ ] **Step 3: Commit failing tests**

```bash
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit add backend/src/test/java/com/github/mwacha/wachafit/goal/GoalServiceTest.java
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit commit -m "test: Goal unit tests (failing)"
```

- [ ] **Step 4: Create GoalStatus enum**

```java
// GoalStatus.java
package com.github.mwacha.wachafit.goal;
public enum GoalStatus { IN_PROGRESS, ACHIEVED, EXPIRED }
```

- [ ] **Step 5: Create DTOs**

```java
// dto/CreateGoalRequest.java
package com.github.mwacha.wachafit.goal.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
public record CreateGoalRequest(String description, String metric, BigDecimal targetValue, LocalDate targetDate) {}

// dto/GoalResponse.java
package com.github.mwacha.wachafit.goal.dto;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
public record GoalResponse(UUID id, UUID studentId, UUID createdById, String description,
    String metric, BigDecimal targetValue, LocalDate targetDate, String status, Instant createdAt) {}

// dto/UpdateGoalStatusRequest.java
package com.github.mwacha.wachafit.goal.dto;
public record UpdateGoalStatusRequest(String status) {}
```

- [ ] **Step 6: Create StudentGoal entity**

```java
// StudentGoal.java
package com.github.mwacha.wachafit.goal;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "student_goals")
public class StudentGoal {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(length = 40)
    private String metric;

    @Column(name = "target_value", precision = 8, scale = 2)
    private BigDecimal targetValue;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GoalStatus status = GoalStatus.IN_PROGRESS;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID studentId) { this.studentId = studentId; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }
    public BigDecimal getTargetValue() { return targetValue; }
    public void setTargetValue(BigDecimal targetValue) { this.targetValue = targetValue; }
    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
    public GoalStatus getStatus() { return status; }
    public void setStatus(GoalStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 7: Create StudentGoalRepository**

```java
// StudentGoalRepository.java
package com.github.mwacha.wachafit.goal;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface StudentGoalRepository extends JpaRepository<StudentGoal, UUID> {
    List<StudentGoal> findByStudentIdOrderByCreatedAtDesc(UUID studentId);
}
```

- [ ] **Step 8: Create GoalService**

```java
// GoalService.java
package com.github.mwacha.wachafit.goal;

import com.github.mwacha.wachafit.goal.dto.*;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.shared.exception.ForbiddenException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class GoalService {

    private final StudentGoalRepository repo;
    private final UserRepository userRepo;

    public GoalService(StudentGoalRepository repo, UserRepository userRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
    }

    public GoalResponse create(UUID studentId, CreateGoalRequest req, UUID createdById) {
        userRepo.findById(studentId).orElseThrow(() -> new NotFoundException("Student not found"));
        StudentGoal g = new StudentGoal();
        g.setStudentId(studentId);
        g.setCreatedBy(createdById);
        g.setDescription(req.description());
        g.setMetric(req.metric());
        g.setTargetValue(req.targetValue());
        g.setTargetDate(req.targetDate());
        return toResponse(repo.save(g));
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> list(UUID studentId, User requestingUser) {
        assertCanAccess(studentId, requestingUser);
        return repo.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
            .map(this::toResponse).toList();
    }

    public GoalResponse updateStatus(UUID goalId, UpdateGoalStatusRequest req, User requestingUser) {
        StudentGoal g = repo.findById(goalId).orElseThrow(() -> new NotFoundException("Goal not found"));
        GoalStatus newStatus;
        try { newStatus = GoalStatus.valueOf(req.status()); }
        catch (IllegalArgumentException e) { throw new BusinessException("Invalid status: " + req.status()); }
        if (newStatus == GoalStatus.IN_PROGRESS) throw new BusinessException("Cannot revert to IN_PROGRESS");
        g.setStatus(newStatus);
        return toResponse(repo.save(g));
    }

    private void assertCanAccess(UUID studentId, User requestingUser) {
        if (requestingUser.getRole() == Role.STUDENT && !studentId.equals(requestingUser.getId())) {
            throw new ForbiddenException("Access denied");
        }
    }

    private GoalResponse toResponse(StudentGoal g) {
        return new GoalResponse(g.getId(), g.getStudentId(), g.getCreatedBy(),
            g.getDescription(), g.getMetric(), g.getTargetValue(), g.getTargetDate(),
            g.getStatus().name(), g.getCreatedAt());
    }
}
```

- [ ] **Step 9: Create GoalController**

```java
// GoalController.java
package com.github.mwacha.wachafit.goal;

import com.github.mwacha.wachafit.goal.dto.*;
import com.github.mwacha.wachafit.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class GoalController {

    private final GoalService service;

    public GoalController(GoalService service) { this.service = service; }

    @PostMapping("/api/students/{studentId}/goals")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GoalResponse> create(
            @PathVariable UUID studentId,
            @RequestBody CreateGoalRequest req,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(service.create(studentId, req, currentUser.getId()));
    }

    @GetMapping("/api/students/{studentId}/goals")
    @PreAuthorize("isAuthenticated()")
    public List<GoalResponse> list(
            @PathVariable UUID studentId,
            @AuthenticationPrincipal User currentUser) {
        return service.list(studentId, currentUser);
    }

    @PatchMapping("/api/goals/{id}/status")
    @PreAuthorize("hasAnyRole('TRAINER','ADMIN')")
    public GoalResponse updateStatus(
            @PathVariable UUID id,
            @RequestBody UpdateGoalStatusRequest req,
            @AuthenticationPrincipal User currentUser) {
        return service.updateStatus(id, req, currentUser);
    }
}
```

- [ ] **Step 10: Write and run integration test**

```java
// backend/src/test/java/com/github/mwacha/wachafit/goal/GoalControllerIntegrationTest.java
package com.github.mwacha.wachafit.goal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mwacha.wachafit.auth.dto.LoginRequest;
import com.github.mwacha.wachafit.goal.dto.CreateGoalRequest;
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
class GoalControllerIntegrationTest {

    @Container static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository userRepo;
    @Autowired PasswordEncoder passwordEncoder;

    private String trainerToken;
    private UUID studentId;

    @BeforeEach
    void setUp() throws Exception {
        userRepo.deleteAll();
        User trainer = new User(); trainer.setName("T"); trainer.setEmail("t@t.com");
        trainer.setPasswordHash(passwordEncoder.encode("pass")); trainer.setRole(Role.TRAINER); trainer.setActive(true);
        userRepo.save(trainer);
        User student = new User(); student.setName("S"); student.setEmail("s@t.com");
        student.setPasswordHash(passwordEncoder.encode("pass")); student.setRole(Role.STUDENT); student.setActive(true);
        userRepo.save(student);
        studentId = student.getId();
        var r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(new LoginRequest("t@t.com", "pass")))).andReturn();
        trainerToken = mapper.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void create_shouldReturn201() throws Exception {
        mvc.perform(post("/api/students/" + studentId + "/goals")
                .header("Authorization", "Bearer " + trainerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CreateGoalRequest("Lose 5kg", "weight", null, null))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void list_withoutToken_shouldReturn401() throws Exception {
        mvc.perform(get("/api/students/" + studentId + "/goals")).andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 11: Run all tests**

```bash
mvn test -Dtest=GoalServiceTest,GoalControllerIntegrationTest 2>&1 | tail -8
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 12: Commit**

```bash
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit add \
    backend/src/main/java/com/github/mwacha/wachafit/goal/ \
    backend/src/test/java/com/github/mwacha/wachafit/goal/GoalControllerIntegrationTest.java
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit commit -m "feat: Goal module (T-15)"
```

---

## Task 3: Exercise module (T-15)

**Files:**
- Create: `backend/src/main/java/com/github/mwacha/wachafit/exercise/Exercise.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/exercise/ExerciseRepository.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/exercise/ExerciseService.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/exercise/ExerciseController.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/exercise/dto/CreateExerciseRequest.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/exercise/dto/UpdateExerciseRequest.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/exercise/dto/ExerciseResponse.java`
- Create: `backend/src/test/java/com/github/mwacha/wachafit/exercise/ExerciseServiceTest.java`
- Create: `backend/src/test/java/com/github/mwacha/wachafit/exercise/ExerciseControllerIntegrationTest.java`

**Interfaces:**
- Produces:
  - `ExerciseService.search(String q, String muscleGroup) → List<ExerciseResponse>`
  - `ExerciseService.create(CreateExerciseRequest) → ExerciseResponse`
  - `ExerciseService.update(UUID id, UpdateExerciseRequest) → ExerciseResponse`
  - `ExerciseService.deactivate(UUID id) → void`

- [ ] **Step 1: Write failing unit tests**

```java
// backend/src/test/java/com/github/mwacha/wachafit/exercise/ExerciseServiceTest.java
package com.github.mwacha.wachafit.exercise;

import com.github.mwacha.wachafit.exercise.dto.*;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceTest {

    @Mock ExerciseRepository repo;
    @InjectMocks ExerciseService service;

    @Test
    void create_shouldPersistExercise() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ExerciseResponse res = service.create(new CreateExerciseRequest("Squat", "legs", null, null));
        assertThat(res.name()).isEqualTo("Squat");
        assertThat(res.muscleGroup()).isEqualTo("legs");
        assertThat(res.active()).isTrue();
    }

    @Test
    void update_shouldThrowNotFound_whenMissing() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(UUID.randomUUID(),
            new UpdateExerciseRequest("Squat", "legs", null, null)))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deactivate_shouldSetActiveFalse() {
        Exercise e = new Exercise();
        e.setName("Squat"); e.setMuscleGroup("legs"); e.setActive(true);
        when(repo.findById(any())).thenReturn(Optional.of(e));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.deactivate(UUID.randomUUID());
        assertThat(e.isActive()).isFalse();
    }

    @Test
    void search_shouldDelegateToRepository() {
        when(repo.search("squat", null)).thenReturn(List.of());
        assertThatNoException().isThrownBy(() -> service.search("squat", null));
    }
}
```

- [ ] **Step 2: Run tests — confirm they fail**

```bash
mvn test -Dtest=ExerciseServiceTest 2>&1 | tail -5
```

Expected: compilation error.

- [ ] **Step 3: Commit failing tests**

```bash
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit add backend/src/test/java/com/github/mwacha/wachafit/exercise/ExerciseServiceTest.java
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit commit -m "test: Exercise unit tests (failing)"
```

- [ ] **Step 4: Create DTOs**

```java
// dto/CreateExerciseRequest.java
package com.github.mwacha.wachafit.exercise.dto;
public record CreateExerciseRequest(String name, String muscleGroup, String description, String videoUrl) {}

// dto/UpdateExerciseRequest.java
package com.github.mwacha.wachafit.exercise.dto;
public record UpdateExerciseRequest(String name, String muscleGroup, String description, String videoUrl) {}

// dto/ExerciseResponse.java
package com.github.mwacha.wachafit.exercise.dto;
import java.util.UUID;
public record ExerciseResponse(UUID id, String name, String muscleGroup, String description, String videoUrl, boolean active) {}
```

- [ ] **Step 5: Create Exercise entity**

```java
// Exercise.java
package com.github.mwacha.wachafit.exercise;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "exercises")
public class Exercise {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "muscle_group", nullable = false, length = 60)
    private String muscleGroup;

    private String description;

    @Column(name = "video_url", length = 255)
    private String videoUrl;

    @Column(nullable = false)
    private boolean active = true;

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMuscleGroup() { return muscleGroup; }
    public void setMuscleGroup(String muscleGroup) { this.muscleGroup = muscleGroup; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
```

- [ ] **Step 6: Create ExerciseRepository**

```java
// ExerciseRepository.java
package com.github.mwacha.wachafit.exercise;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {

    @Query("""
        SELECT e FROM Exercise e
        WHERE e.active = true
        AND (:q IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :q, '%')))
        AND (:muscleGroup IS NULL OR e.muscleGroup = :muscleGroup)
        ORDER BY e.name ASC
    """)
    List<Exercise> search(@Param("q") String q, @Param("muscleGroup") String muscleGroup);
}
```

- [ ] **Step 7: Create ExerciseService**

```java
// ExerciseService.java
package com.github.mwacha.wachafit.exercise;

import com.github.mwacha.wachafit.exercise.dto.*;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ExerciseService {

    private final ExerciseRepository repo;

    public ExerciseService(ExerciseRepository repo) { this.repo = repo; }

    public ExerciseResponse create(CreateExerciseRequest req) {
        Exercise e = new Exercise();
        e.setName(req.name());
        e.setMuscleGroup(req.muscleGroup());
        e.setDescription(req.description());
        e.setVideoUrl(req.videoUrl());
        return toResponse(repo.save(e));
    }

    @Transactional(readOnly = true)
    public List<ExerciseResponse> search(String q, String muscleGroup) {
        return repo.search(
            (q != null && q.isBlank()) ? null : q,
            (muscleGroup != null && muscleGroup.isBlank()) ? null : muscleGroup
        ).stream().map(this::toResponse).toList();
    }

    public ExerciseResponse update(UUID id, UpdateExerciseRequest req) {
        Exercise e = repo.findById(id).orElseThrow(() -> new NotFoundException("Exercise not found"));
        e.setName(req.name());
        e.setMuscleGroup(req.muscleGroup());
        e.setDescription(req.description());
        e.setVideoUrl(req.videoUrl());
        return toResponse(repo.save(e));
    }

    public void deactivate(UUID id) {
        Exercise e = repo.findById(id).orElseThrow(() -> new NotFoundException("Exercise not found"));
        e.setActive(false);
        repo.save(e);
    }

    private ExerciseResponse toResponse(Exercise e) {
        return new ExerciseResponse(e.getId(), e.getName(), e.getMuscleGroup(),
            e.getDescription(), e.getVideoUrl(), e.isActive());
    }
}
```

- [ ] **Step 8: Create ExerciseController**

```java
// ExerciseController.java
package com.github.mwacha.wachafit.exercise;

import com.github.mwacha.wachafit.exercise.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {

    private final ExerciseService service;

    public ExerciseController(ExerciseService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<ExerciseResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String muscleGroup) {
        return service.search(q, muscleGroup);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TRAINER','ADMIN')")
    public ResponseEntity<ExerciseResponse> create(@RequestBody CreateExerciseRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TRAINER','ADMIN')")
    public ExerciseResponse update(@PathVariable UUID id, @RequestBody UpdateExerciseRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 9: Write integration test**

```java
// backend/src/test/java/com/github/mwacha/wachafit/exercise/ExerciseControllerIntegrationTest.java
package com.github.mwacha.wachafit.exercise;

import com.fasterxml.jackson.databind.ObjectMapper;
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

@SpringBootTest @AutoConfigureMockMvc @Testcontainers @ActiveProfiles("test")
class ExerciseControllerIntegrationTest {

    @Container static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository userRepo;
    @Autowired PasswordEncoder passwordEncoder;

    private String trainerToken;

    @BeforeEach
    void setUp() throws Exception {
        userRepo.deleteAll();
        User trainer = new User(); trainer.setName("T"); trainer.setEmail("t@t.com");
        trainer.setPasswordHash(passwordEncoder.encode("pass")); trainer.setRole(Role.TRAINER); trainer.setActive(true);
        userRepo.save(trainer);
        var r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(new LoginRequest("t@t.com", "pass")))).andReturn();
        trainerToken = mapper.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void create_shouldReturn201() throws Exception {
        mvc.perform(post("/api/exercises")
                .header("Authorization", "Bearer " + trainerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CreateExerciseRequest("Squat", "legs", null, null))))
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
        mvc.perform(get("/api/exercises")).andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 10: Run all tests**

```bash
cd /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit/backend
mvn test -Dtest=ExerciseServiceTest,ExerciseControllerIntegrationTest 2>&1 | tail -8
mvn test 2>&1 | tail -5
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 11: Commit**

```bash
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit add \
    backend/src/main/java/com/github/mwacha/wachafit/exercise/ \
    backend/src/test/java/com/github/mwacha/wachafit/exercise/ExerciseControllerIntegrationTest.java
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit commit -m "feat: Exercise library module (T-15)"
```
