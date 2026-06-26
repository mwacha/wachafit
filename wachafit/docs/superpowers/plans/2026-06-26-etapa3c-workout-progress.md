# WachaFit Etapa 3C — Workout + Progress Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the Workout module (plans, items, logs, personal records) and the Progress Photos module (T-16, T-17).

**Architecture:** Two modules. `workout` manages prescriptions and execution logs with automatic PR upsert (RN-10) and single-active-plan enforcement (RN-12). `progress` handles multipart photo upload to the local filesystem and serves files via `StreamingResponseBody`.

**Tech Stack:** Java 21, Spring Boot 3, Spring Security, JPA, PostgreSQL 16, Flyway (V11–V15 already applied), Mockito, Testcontainers, JUnit 5, Spring `MultipartFile`, `FileSystemResource`

## Global Constraints

- Package root: `com.github.mwacha.wachafit`
- Worktree: `/Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit/`
- Git: `git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit`
- Maven: `cd backend && mvn ...`
- No Lombok — manual getters/setters
- Tests: `@ActiveProfiles("test")`, Testcontainers Postgres
- TDD: failing tests committed before implementation
- RN-09: `POST /workout-plans` TRAINER/ADMIN only; `POST /workout-logs` STUDENT (own) only
- RN-10: When saving WorkoutLog with load_kg, upsert `personal_records` if new record. Same `@Transactional` boundary.
- RN-11: student data visible only to owner + TRAINER/ADMIN
- RN-12: `activatePlan` sets `active=false` for ALL plans of the student before setting this one to `true`. Same transaction.
- Photos: stored at `${app.upload-dir}/photos/{studentId}/{uuid}.{ext}`. `storage_key` in DB is `{studentId}/{uuid}.{ext}`.
- `application-dev.yml` must have `app.upload-dir: uploads`. Add `uploads/` to `.gitignore`.
- Existing shared exceptions: `NotFoundException`, `BusinessException`, `ForbiddenException`

---

## Task 1: Workout module (T-16)

**Files:**
- Create: `backend/src/main/java/com/github/mwacha/wachafit/workout/WorkoutPlan.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/workout/WorkoutPlanItem.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/workout/WorkoutLog.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/workout/PersonalRecord.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/workout/WorkoutPlanRepository.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/workout/WorkoutPlanItemRepository.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/workout/WorkoutLogRepository.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/workout/PersonalRecordRepository.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/workout/WorkoutService.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/workout/WorkoutController.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/workout/dto/WorkoutPlanItemRequest.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/workout/dto/CreateWorkoutPlanRequest.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/workout/dto/WorkoutPlanItemResponse.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/workout/dto/WorkoutPlanResponse.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/workout/dto/CreateWorkoutLogRequest.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/workout/dto/WorkoutLogResponse.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/workout/dto/PersonalRecordResponse.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/workout/dto/ProgressionPoint.java`
- Create: `backend/src/test/java/com/github/mwacha/wachafit/workout/WorkoutServiceTest.java`
- Create: `backend/src/test/java/com/github/mwacha/wachafit/workout/WorkoutControllerIntegrationTest.java`
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/user/UserRepository.java` — add `existsById` if not present (already inherited from JpaRepository, no change needed)

**Interfaces:**
- Consumes: `Exercise` entity (id only for FK), `UserRepository`
- Produces:
  - `WorkoutService.createPlan(UUID studentId, CreateWorkoutPlanRequest, UUID trainerId) → WorkoutPlanResponse`
  - `WorkoutService.listPlans(UUID studentId, User) → List<WorkoutPlanResponse>`
  - `WorkoutService.getActivePlan(UUID studentId, User) → WorkoutPlanResponse`
  - `WorkoutService.updatePlan(UUID planId, CreateWorkoutPlanRequest, User) → WorkoutPlanResponse`
  - `WorkoutService.activatePlan(UUID planId, User) → WorkoutPlanResponse`
  - `WorkoutService.createLog(UUID studentId, CreateWorkoutLogRequest, User) → WorkoutLogResponse`
  - `WorkoutService.listLogs(UUID studentId, User) → List<WorkoutLogResponse>`
  - `WorkoutService.listRecords(UUID studentId, User) → List<PersonalRecordResponse>`
  - `WorkoutService.progression(UUID studentId, UUID exerciseId, User) → List<ProgressionPoint>`

- [ ] **Step 1: Write failing unit tests**

```java
// backend/src/test/java/com/github/mwacha/wachafit/workout/WorkoutServiceTest.java
package com.github.mwacha.wachafit.workout;

import com.github.mwacha.wachafit.shared.exception.ForbiddenException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import com.github.mwacha.wachafit.workout.dto.*;
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
class WorkoutServiceTest {

    @Mock WorkoutPlanRepository planRepo;
    @Mock WorkoutPlanItemRepository itemRepo;
    @Mock WorkoutLogRepository logRepo;
    @Mock PersonalRecordRepository prRepo;
    @Mock UserRepository userRepo;
    @InjectMocks WorkoutService service;

    private User trainer;
    private User student;
    private UUID studentId;
    private UUID trainerId;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        trainerId = UUID.randomUUID();
        student = new User(); student.setRole(Role.STUDENT);
        try { var f = User.class.getDeclaredField("id"); f.setAccessible(true); f.set(student, studentId); }
        catch (Exception e) { throw new RuntimeException(e); }
        trainer = new User(); trainer.setRole(Role.TRAINER);
        try { var f = User.class.getDeclaredField("id"); f.setAccessible(true); f.set(trainer, trainerId); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void createPlan_shouldPersistPlanWithItems() {
        when(userRepo.findById(studentId)).thenReturn(Optional.of(student));
        when(planRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        CreateWorkoutPlanRequest req = new CreateWorkoutPlanRequest("Plan A", null,
            List.of(new WorkoutPlanItemRequest(UUID.randomUUID(), "A", 3, "12", null, 60, 1, null)));
        WorkoutPlanResponse res = service.createPlan(studentId, req, trainerId);
        assertThat(res.name()).isEqualTo("Plan A");
        assertThat(res.items()).hasSize(1);
    }

    @Test
    void createPlan_shouldThrowNotFound_whenStudentMissing() {
        when(userRepo.findById(studentId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createPlan(studentId,
            new CreateWorkoutPlanRequest("P", null, List.of()), trainerId))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createLog_shouldThrowForbidden_whenStudentLogsForOther() {
        User other = new User(); other.setRole(Role.STUDENT);
        try { var f = User.class.getDeclaredField("id"); f.setAccessible(true); f.set(other, UUID.randomUUID()); }
        catch (Exception e) { throw new RuntimeException(e); }
        assertThatThrownBy(() -> service.createLog(studentId,
            new CreateWorkoutLogRequest(UUID.randomUUID(), null, LocalDate.now(), 3, 12, null, null), other))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void createLog_shouldUpsertPersonalRecord_whenNewRecord() {
        when(logRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(prRepo.findByStudentIdAndExerciseId(studentId, any())).thenReturn(Optional.empty());
        when(prRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        UUID exerciseId = UUID.randomUUID();
        service.createLog(studentId,
            new CreateWorkoutLogRequest(exerciseId, null, LocalDate.now(), 3, 12, new BigDecimal("100"), null),
            student);
        verify(prRepo).save(any(PersonalRecord.class));
    }

    @Test
    void createLog_shouldNotUpsertPersonalRecord_whenLoadNotSuperior() {
        PersonalRecord existing = new PersonalRecord();
        existing.setRecordLoadKg(new BigDecimal("110"));
        when(logRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(prRepo.findByStudentIdAndExerciseId(studentId, any())).thenReturn(Optional.of(existing));
        UUID exerciseId = UUID.randomUUID();
        service.createLog(studentId,
            new CreateWorkoutLogRequest(exerciseId, null, LocalDate.now(), 3, 12, new BigDecimal("100"), null),
            student);
        verify(prRepo, never()).save(any());
    }

    @Test
    void activatePlan_shouldDeactivateOthersFirst() {
        WorkoutPlan plan = new WorkoutPlan();
        plan.setStudentId(studentId); plan.setActive(false);
        when(planRepo.findById(any())).thenReturn(Optional.of(plan));
        when(planRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.activatePlan(UUID.randomUUID(), trainer);
        verify(planRepo).deactivateAllForStudent(studentId);
        assertThat(plan.isActive()).isTrue();
    }
}
```

- [ ] **Step 2: Run tests — confirm they fail**

```bash
cd /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit/backend
mvn test -Dtest=WorkoutServiceTest 2>&1 | tail -5
```

Expected: compilation error.

- [ ] **Step 3: Commit failing tests**

```bash
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit add backend/src/test/java/com/github/mwacha/wachafit/workout/WorkoutServiceTest.java
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit commit -m "test: Workout unit tests (failing)"
```

- [ ] **Step 4: Create DTOs**

```java
// dto/WorkoutPlanItemRequest.java
package com.github.mwacha.wachafit.workout.dto;
import java.math.BigDecimal;
import java.util.UUID;
public record WorkoutPlanItemRequest(
    UUID exerciseId, String division, int sets, String reps,
    BigDecimal suggestedLoadKg, Integer restSeconds, int orderIndex, String notes) {}

// dto/CreateWorkoutPlanRequest.java
package com.github.mwacha.wachafit.workout.dto;
import java.util.List;
public record CreateWorkoutPlanRequest(String name, String description, List<WorkoutPlanItemRequest> items) {}

// dto/WorkoutPlanItemResponse.java
package com.github.mwacha.wachafit.workout.dto;
import java.math.BigDecimal;
import java.util.UUID;
public record WorkoutPlanItemResponse(
    UUID id, UUID exerciseId, String division, int sets, String reps,
    BigDecimal suggestedLoadKg, Integer restSeconds, int orderIndex, String notes) {}

// dto/WorkoutPlanResponse.java
package com.github.mwacha.wachafit.workout.dto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
public record WorkoutPlanResponse(
    UUID id, UUID studentId, UUID trainerId, String name, String description,
    boolean active, Instant createdAt, List<WorkoutPlanItemResponse> items) {}

// dto/CreateWorkoutLogRequest.java
package com.github.mwacha.wachafit.workout.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
public record CreateWorkoutLogRequest(
    UUID exerciseId, UUID workoutPlanItemId, LocalDate performedAt,
    Integer sets, Integer reps, BigDecimal loadKg, String notes) {}

// dto/WorkoutLogResponse.java
package com.github.mwacha.wachafit.workout.dto;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
public record WorkoutLogResponse(
    UUID id, UUID studentId, UUID exerciseId, UUID workoutPlanItemId,
    LocalDate performedAt, Integer sets, Integer reps, BigDecimal loadKg, String notes, Instant createdAt) {}

// dto/PersonalRecordResponse.java
package com.github.mwacha.wachafit.workout.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
public record PersonalRecordResponse(UUID id, UUID exerciseId, BigDecimal recordLoadKg, LocalDate achievedAt) {}

// dto/ProgressionPoint.java
package com.github.mwacha.wachafit.workout.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
public record ProgressionPoint(LocalDate performedAt, BigDecimal loadKg, Integer reps) {}
```

- [ ] **Step 5: Create entities**

```java
// WorkoutPlan.java
package com.github.mwacha.wachafit.workout;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "workout_plans")
public class WorkoutPlan {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "student_id", nullable = false) private UUID studentId;
    @Column(name = "trainer_id", nullable = false) private UUID trainerId;
    @Column(nullable = false, length = 120) private String name;
    private String description;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false) private Instant createdAt;
    @OneToMany(mappedBy = "workoutPlan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orderIndex ASC")
    private List<WorkoutPlanItem> items = new ArrayList<>();

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID v) { this.studentId = v; }
    public UUID getTrainerId() { return trainerId; }
    public void setTrainerId(UUID v) { this.trainerId = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }
    public Instant getCreatedAt() { return createdAt; }
    public List<WorkoutPlanItem> getItems() { return items; }
}

// WorkoutPlanItem.java
package com.github.mwacha.wachafit.workout;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "workout_plan_items")
public class WorkoutPlanItem {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "workout_plan_id", nullable = false) private WorkoutPlan workoutPlan;
    @Column(name = "exercise_id", nullable = false) private UUID exerciseId;
    @Column(length = 10) private String division;
    @Column(nullable = false) private int sets;
    @Column(nullable = false, length = 20) private String reps;
    @Column(name = "suggested_load_kg", precision = 6, scale = 2) private BigDecimal suggestedLoadKg;
    @Column(name = "rest_seconds") private Integer restSeconds;
    @Column(name = "order_index", nullable = false) private int orderIndex;
    @Column(length = 200) private String notes;

    public UUID getId() { return id; }
    public WorkoutPlan getWorkoutPlan() { return workoutPlan; }
    public void setWorkoutPlan(WorkoutPlan v) { this.workoutPlan = v; }
    public UUID getExerciseId() { return exerciseId; }
    public void setExerciseId(UUID v) { this.exerciseId = v; }
    public String getDivision() { return division; }
    public void setDivision(String v) { this.division = v; }
    public int getSets() { return sets; }
    public void setSets(int v) { this.sets = v; }
    public String getReps() { return reps; }
    public void setReps(String v) { this.reps = v; }
    public BigDecimal getSuggestedLoadKg() { return suggestedLoadKg; }
    public void setSuggestedLoadKg(BigDecimal v) { this.suggestedLoadKg = v; }
    public Integer getRestSeconds() { return restSeconds; }
    public void setRestSeconds(Integer v) { this.restSeconds = v; }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int v) { this.orderIndex = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
}

// WorkoutLog.java
package com.github.mwacha.wachafit.workout;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "workout_logs")
public class WorkoutLog {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "student_id", nullable = false) private UUID studentId;
    @Column(name = "exercise_id", nullable = false) private UUID exerciseId;
    @Column(name = "workout_plan_item_id") private UUID workoutPlanItemId;
    @Column(name = "performed_at", nullable = false) private LocalDate performedAt;
    private Integer sets;
    private Integer reps;
    @Column(name = "load_kg", precision = 6, scale = 2) private BigDecimal loadKg;
    @Column(length = 200) private String notes;
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false) private Instant createdAt;

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID v) { this.studentId = v; }
    public UUID getExerciseId() { return exerciseId; }
    public void setExerciseId(UUID v) { this.exerciseId = v; }
    public UUID getWorkoutPlanItemId() { return workoutPlanItemId; }
    public void setWorkoutPlanItemId(UUID v) { this.workoutPlanItemId = v; }
    public LocalDate getPerformedAt() { return performedAt; }
    public void setPerformedAt(LocalDate v) { this.performedAt = v; }
    public Integer getSets() { return sets; }
    public void setSets(Integer v) { this.sets = v; }
    public Integer getReps() { return reps; }
    public void setReps(Integer v) { this.reps = v; }
    public BigDecimal getLoadKg() { return loadKg; }
    public void setLoadKg(BigDecimal v) { this.loadKg = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
    public Instant getCreatedAt() { return createdAt; }
}

// PersonalRecord.java
package com.github.mwacha.wachafit.workout;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "personal_records")
public class PersonalRecord {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "student_id", nullable = false) private UUID studentId;
    @Column(name = "exercise_id", nullable = false) private UUID exerciseId;
    @Column(name = "record_load_kg", nullable = false, precision = 6, scale = 2) private BigDecimal recordLoadKg;
    @Column(name = "achieved_at", nullable = false) private LocalDate achievedAt;

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID v) { this.studentId = v; }
    public UUID getExerciseId() { return exerciseId; }
    public void setExerciseId(UUID v) { this.exerciseId = v; }
    public BigDecimal getRecordLoadKg() { return recordLoadKg; }
    public void setRecordLoadKg(BigDecimal v) { this.recordLoadKg = v; }
    public LocalDate getAchievedAt() { return achievedAt; }
    public void setAchievedAt(LocalDate v) { this.achievedAt = v; }
}
```

- [ ] **Step 6: Create repositories**

```java
// WorkoutPlanRepository.java
package com.github.mwacha.wachafit.workout;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, UUID> {
    List<WorkoutPlan> findByStudentIdOrderByCreatedAtDesc(UUID studentId);
    Optional<WorkoutPlan> findByStudentIdAndActiveTrue(UUID studentId);

    @Modifying
    @Query("UPDATE WorkoutPlan p SET p.active = false WHERE p.studentId = :studentId")
    void deactivateAllForStudent(@Param("studentId") UUID studentId);
}

// WorkoutPlanItemRepository.java
package com.github.mwacha.wachafit.workout;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface WorkoutPlanItemRepository extends JpaRepository<WorkoutPlanItem, UUID> {}

// WorkoutLogRepository.java
package com.github.mwacha.wachafit.workout;

import com.github.mwacha.wachafit.workout.dto.ProgressionPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface WorkoutLogRepository extends JpaRepository<WorkoutLog, UUID> {
    List<WorkoutLog> findByStudentIdOrderByPerformedAtDesc(UUID studentId);

    @Query("""
        SELECT new com.github.mwacha.wachafit.workout.dto.ProgressionPoint(
            l.performedAt, l.loadKg, l.reps)
        FROM WorkoutLog l
        WHERE l.studentId = :studentId AND l.exerciseId = :exerciseId
        ORDER BY l.performedAt ASC
    """)
    List<ProgressionPoint> findProgressionByStudentAndExercise(
        @Param("studentId") UUID studentId, @Param("exerciseId") UUID exerciseId);
}

// PersonalRecordRepository.java
package com.github.mwacha.wachafit.workout;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface PersonalRecordRepository extends JpaRepository<PersonalRecord, UUID> {
    Optional<PersonalRecord> findByStudentIdAndExerciseId(UUID studentId, UUID exerciseId);
    List<PersonalRecord> findByStudentIdOrderByAchievedAtDesc(UUID studentId);
}
```

- [ ] **Step 7: Create WorkoutService**

```java
// WorkoutService.java
package com.github.mwacha.wachafit.workout;

import com.github.mwacha.wachafit.shared.exception.ForbiddenException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import com.github.mwacha.wachafit.workout.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class WorkoutService {

    private final WorkoutPlanRepository planRepo;
    private final WorkoutPlanItemRepository itemRepo;
    private final WorkoutLogRepository logRepo;
    private final PersonalRecordRepository prRepo;
    private final UserRepository userRepo;

    public WorkoutService(WorkoutPlanRepository planRepo, WorkoutPlanItemRepository itemRepo,
            WorkoutLogRepository logRepo, PersonalRecordRepository prRepo, UserRepository userRepo) {
        this.planRepo = planRepo; this.itemRepo = itemRepo;
        this.logRepo = logRepo; this.prRepo = prRepo; this.userRepo = userRepo;
    }

    public WorkoutPlanResponse createPlan(UUID studentId, CreateWorkoutPlanRequest req, UUID trainerId) {
        userRepo.findById(studentId).orElseThrow(() -> new NotFoundException("Student not found"));
        WorkoutPlan plan = new WorkoutPlan();
        plan.setStudentId(studentId); plan.setTrainerId(trainerId);
        plan.setName(req.name()); plan.setDescription(req.description());
        if (req.items() != null) {
            for (WorkoutPlanItemRequest i : req.items()) {
                WorkoutPlanItem item = new WorkoutPlanItem();
                item.setExerciseId(i.exerciseId()); item.setDivision(i.division());
                item.setSets(i.sets()); item.setReps(i.reps());
                item.setSuggestedLoadKg(i.suggestedLoadKg()); item.setRestSeconds(i.restSeconds());
                item.setOrderIndex(i.orderIndex()); item.setNotes(i.notes());
                item.setWorkoutPlan(plan); plan.getItems().add(item);
            }
        }
        return toPlanResponse(planRepo.save(plan));
    }

    @Transactional(readOnly = true)
    public List<WorkoutPlanResponse> listPlans(UUID studentId, User requestingUser) {
        assertCanAccess(studentId, requestingUser);
        return planRepo.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
            .map(this::toPlanResponse).toList();
    }

    @Transactional(readOnly = true)
    public WorkoutPlanResponse getActivePlan(UUID studentId, User requestingUser) {
        assertCanAccess(studentId, requestingUser);
        return planRepo.findByStudentIdAndActiveTrue(studentId)
            .map(this::toPlanResponse)
            .orElseThrow(() -> new NotFoundException("No active plan found"));
    }

    public WorkoutPlanResponse updatePlan(UUID planId, CreateWorkoutPlanRequest req, User requestingUser) {
        WorkoutPlan plan = planRepo.findById(planId)
            .orElseThrow(() -> new NotFoundException("Plan not found"));
        plan.setName(req.name()); plan.setDescription(req.description());
        plan.getItems().clear();
        if (req.items() != null) {
            for (WorkoutPlanItemRequest i : req.items()) {
                WorkoutPlanItem item = new WorkoutPlanItem();
                item.setExerciseId(i.exerciseId()); item.setDivision(i.division());
                item.setSets(i.sets()); item.setReps(i.reps());
                item.setSuggestedLoadKg(i.suggestedLoadKg()); item.setRestSeconds(i.restSeconds());
                item.setOrderIndex(i.orderIndex()); item.setNotes(i.notes());
                item.setWorkoutPlan(plan); plan.getItems().add(item);
            }
        }
        return toPlanResponse(planRepo.save(plan));
    }

    public WorkoutPlanResponse activatePlan(UUID planId, User requestingUser) {
        WorkoutPlan plan = planRepo.findById(planId)
            .orElseThrow(() -> new NotFoundException("Plan not found"));
        planRepo.deactivateAllForStudent(plan.getStudentId());
        plan.setActive(true);
        return toPlanResponse(planRepo.save(plan));
    }

    public WorkoutLogResponse createLog(UUID studentId, CreateWorkoutLogRequest req, User requestingUser) {
        if (requestingUser.getRole() == Role.STUDENT && !studentId.equals(requestingUser.getId())) {
            throw new ForbiddenException("Access denied");
        }
        WorkoutLog log = new WorkoutLog();
        log.setStudentId(studentId); log.setExerciseId(req.exerciseId());
        log.setWorkoutPlanItemId(req.workoutPlanItemId()); log.setPerformedAt(req.performedAt());
        log.setSets(req.sets()); log.setReps(req.reps());
        log.setLoadKg(req.loadKg()); log.setNotes(req.notes());
        WorkoutLog saved = logRepo.save(log);
        // RN-10: upsert personal record
        if (req.loadKg() != null) {
            prRepo.findByStudentIdAndExerciseId(studentId, req.exerciseId())
                .ifPresentOrElse(pr -> {
                    if (req.loadKg().compareTo(pr.getRecordLoadKg()) > 0) {
                        pr.setRecordLoadKg(req.loadKg());
                        pr.setAchievedAt(req.performedAt());
                        prRepo.save(pr);
                    }
                }, () -> {
                    PersonalRecord pr = new PersonalRecord();
                    pr.setStudentId(studentId); pr.setExerciseId(req.exerciseId());
                    pr.setRecordLoadKg(req.loadKg()); pr.setAchievedAt(req.performedAt());
                    prRepo.save(pr);
                });
        }
        return toLogResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<WorkoutLogResponse> listLogs(UUID studentId, User requestingUser) {
        assertCanAccess(studentId, requestingUser);
        return logRepo.findByStudentIdOrderByPerformedAtDesc(studentId).stream()
            .map(this::toLogResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PersonalRecordResponse> listRecords(UUID studentId, User requestingUser) {
        assertCanAccess(studentId, requestingUser);
        return prRepo.findByStudentIdOrderByAchievedAtDesc(studentId).stream()
            .map(pr -> new PersonalRecordResponse(pr.getId(), pr.getExerciseId(),
                pr.getRecordLoadKg(), pr.getAchievedAt()))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ProgressionPoint> progression(UUID studentId, UUID exerciseId, User requestingUser) {
        assertCanAccess(studentId, requestingUser);
        return logRepo.findProgressionByStudentAndExercise(studentId, exerciseId);
    }

    private void assertCanAccess(UUID studentId, User requestingUser) {
        if (requestingUser.getRole() == Role.STUDENT && !studentId.equals(requestingUser.getId())) {
            throw new ForbiddenException("Access denied");
        }
    }

    private WorkoutPlanResponse toPlanResponse(WorkoutPlan p) {
        List<WorkoutPlanItemResponse> items = p.getItems().stream()
            .map(i -> new WorkoutPlanItemResponse(i.getId(), i.getExerciseId(), i.getDivision(),
                i.getSets(), i.getReps(), i.getSuggestedLoadKg(), i.getRestSeconds(),
                i.getOrderIndex(), i.getNotes()))
            .toList();
        return new WorkoutPlanResponse(p.getId(), p.getStudentId(), p.getTrainerId(),
            p.getName(), p.getDescription(), p.isActive(), p.getCreatedAt(), items);
    }

    private WorkoutLogResponse toLogResponse(WorkoutLog l) {
        return new WorkoutLogResponse(l.getId(), l.getStudentId(), l.getExerciseId(),
            l.getWorkoutPlanItemId(), l.getPerformedAt(), l.getSets(), l.getReps(),
            l.getLoadKg(), l.getNotes(), l.getCreatedAt());
    }
}
```

- [ ] **Step 8: Create WorkoutController**

```java
// WorkoutController.java
package com.github.mwacha.wachafit.workout;

import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.workout.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class WorkoutController {

    private final WorkoutService service;

    public WorkoutController(WorkoutService service) { this.service = service; }

    @PostMapping("/api/students/{studentId}/workout-plans")
    @PreAuthorize("hasAnyRole('TRAINER','ADMIN')")
    public ResponseEntity<WorkoutPlanResponse> createPlan(@PathVariable UUID studentId,
            @RequestBody CreateWorkoutPlanRequest req, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(service.createPlan(studentId, req, currentUser.getId()));
    }

    @GetMapping("/api/students/{studentId}/workout-plans")
    @PreAuthorize("isAuthenticated()")
    public List<WorkoutPlanResponse> listPlans(@PathVariable UUID studentId,
            @AuthenticationPrincipal User currentUser) {
        return service.listPlans(studentId, currentUser);
    }

    @GetMapping("/api/students/{studentId}/workout-plans/active")
    @PreAuthorize("isAuthenticated()")
    public WorkoutPlanResponse getActivePlan(@PathVariable UUID studentId,
            @AuthenticationPrincipal User currentUser) {
        return service.getActivePlan(studentId, currentUser);
    }

    @PutMapping("/api/workout-plans/{id}")
    @PreAuthorize("hasAnyRole('TRAINER','ADMIN')")
    public WorkoutPlanResponse updatePlan(@PathVariable UUID id,
            @RequestBody CreateWorkoutPlanRequest req, @AuthenticationPrincipal User currentUser) {
        return service.updatePlan(id, req, currentUser);
    }

    @PatchMapping("/api/workout-plans/{id}/activate")
    @PreAuthorize("hasAnyRole('TRAINER','ADMIN')")
    public WorkoutPlanResponse activatePlan(@PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        return service.activatePlan(id, currentUser);
    }

    @PostMapping("/api/students/{studentId}/workout-logs")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<WorkoutLogResponse> createLog(@PathVariable UUID studentId,
            @RequestBody CreateWorkoutLogRequest req, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(service.createLog(studentId, req, currentUser));
    }

    @GetMapping("/api/students/{studentId}/workout-logs")
    @PreAuthorize("isAuthenticated()")
    public List<WorkoutLogResponse> listLogs(@PathVariable UUID studentId,
            @AuthenticationPrincipal User currentUser) {
        return service.listLogs(studentId, currentUser);
    }

    @GetMapping("/api/students/{studentId}/records")
    @PreAuthorize("isAuthenticated()")
    public List<PersonalRecordResponse> listRecords(@PathVariable UUID studentId,
            @AuthenticationPrincipal User currentUser) {
        return service.listRecords(studentId, currentUser);
    }

    @GetMapping("/api/students/{studentId}/exercises/{exerciseId}/progression")
    @PreAuthorize("isAuthenticated()")
    public List<ProgressionPoint> progression(@PathVariable UUID studentId,
            @PathVariable UUID exerciseId, @AuthenticationPrincipal User currentUser) {
        return service.progression(studentId, exerciseId, currentUser);
    }
}
```

- [ ] **Step 9: Write integration test**

```java
// backend/src/test/java/com/github/mwacha/wachafit/workout/WorkoutControllerIntegrationTest.java
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

@SpringBootTest @AutoConfigureMockMvc @Testcontainers @ActiveProfiles("test")
class WorkoutControllerIntegrationTest {

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
    @Autowired ExerciseRepository exerciseRepo;
    @Autowired PasswordEncoder passwordEncoder;

    private String trainerToken;
    private String studentToken;
    private UUID studentId;
    private UUID exerciseId;

    @BeforeEach
    void setUp() throws Exception {
        userRepo.deleteAll(); exerciseRepo.deleteAll();
        User trainer = new User(); trainer.setName("T"); trainer.setEmail("t@t.com");
        trainer.setPasswordHash(passwordEncoder.encode("pass")); trainer.setRole(Role.TRAINER); trainer.setActive(true);
        userRepo.save(trainer);
        User student = new User(); student.setName("S"); student.setEmail("s@t.com");
        student.setPasswordHash(passwordEncoder.encode("pass")); student.setRole(Role.STUDENT); student.setActive(true);
        userRepo.save(student);
        studentId = student.getId();
        Exercise ex = new Exercise(); ex.setName("Squat"); ex.setMuscleGroup("legs");
        exerciseRepo.save(ex);
        exerciseId = ex.getId();

        var r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(new LoginRequest("t@t.com", "pass")))).andReturn();
        trainerToken = mapper.readTree(r.getResponse().getContentAsString()).get("token").asText();
        var r2 = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(new LoginRequest("s@t.com", "pass")))).andReturn();
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
    void listRecords_withStudentToken_shouldReturn200() throws Exception {
        mvc.perform(get("/api/students/" + studentId + "/records")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }
}
```

- [ ] **Step 10: Run all tests**

```bash
cd /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit/backend
mvn test -Dtest=WorkoutServiceTest,WorkoutControllerIntegrationTest 2>&1 | tail -10
mvn test 2>&1 | tail -5
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 11: Commit**

```bash
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit add \
    backend/src/main/java/com/github/mwacha/wachafit/workout/ \
    backend/src/test/java/com/github/mwacha/wachafit/workout/WorkoutControllerIntegrationTest.java
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit commit -m "feat: Workout module with RN-09, RN-10, RN-12 (T-16)"
```

---

## Task 2: Progress Photos module (T-17)

**Files:**
- Create: `backend/src/main/java/com/github/mwacha/wachafit/progress/ProgressPhoto.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/progress/ProgressPhotoRepository.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/progress/ProgressService.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/progress/ProgressController.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/progress/dto/PhotoResponse.java`
- Create: `backend/src/test/java/com/github/mwacha/wachafit/progress/ProgressServiceTest.java`
- Create: `backend/src/test/java/com/github/mwacha/wachafit/progress/ProgressControllerIntegrationTest.java`
- Modify: `backend/src/main/resources/application-dev.yml` — add `app.upload-dir: uploads`
- Modify: `backend/src/main/resources/application-test.yml` — add `app.upload-dir: ${java.io.tmpdir}/wachafit-test-uploads`
- Modify: `.gitignore` (at worktree root) — add `wachafit/uploads/`

**Interfaces:**
- Produces:
  - `ProgressService.upload(UUID studentId, MultipartFile, LocalDate takenAt, String notes, User uploadedBy) → PhotoResponse`
  - `ProgressService.list(UUID studentId, User) → List<PhotoResponse>`
  - `ProgressService.loadFile(UUID photoId, User) → Resource`
  - `ProgressService.delete(UUID photoId, User) → void`

- [ ] **Step 1: Add upload-dir config and update .gitignore**

In `backend/src/main/resources/application-dev.yml`, add:
```yaml
app:
  upload-dir: uploads
```

In `backend/src/main/resources/application-test.yml` (create if needed), ensure it has:
```yaml
app:
  upload-dir: ${java.io.tmpdir}/wachafit-test-uploads
```

In `.gitignore` at the worktree root (`/Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/.gitignore`), add:
```
wachafit/uploads/
```

- [ ] **Step 2: Write failing unit tests**

```java
// backend/src/test/java/com/github/mwacha/wachafit/progress/ProgressServiceTest.java
package com.github.mwacha.wachafit.progress;

import com.github.mwacha.wachafit.shared.exception.ForbiddenException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    @Mock ProgressPhotoRepository repo;
    ProgressService service;

    private User student;
    private UUID studentId;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        student = new User(); student.setRole(Role.STUDENT);
        try { var f = User.class.getDeclaredField("id"); f.setAccessible(true); f.set(student, studentId); }
        catch (Exception e) { throw new RuntimeException(e); }
        service = new ProgressService(repo, System.getProperty("java.io.tmpdir") + "/wachafit-test");
    }

    @Test
    void loadFile_shouldThrowNotFound_whenPhotoMissing() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.loadFile(UUID.randomUUID(), student))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void delete_shouldThrowForbidden_whenStudentDeletesOthersPhoto() {
        ProgressPhoto photo = new ProgressPhoto();
        photo.setStudentId(UUID.randomUUID()); // different student
        photo.setStorageKey("other/file.jpg");
        when(repo.findById(any())).thenReturn(Optional.of(photo));
        User other = new User(); other.setRole(Role.STUDENT);
        try { var f = User.class.getDeclaredField("id"); f.setAccessible(true); f.set(other, UUID.randomUUID()); }
        catch (Exception e) { throw new RuntimeException(e); }
        assertThatThrownBy(() -> service.delete(UUID.randomUUID(), other))
            .isInstanceOf(ForbiddenException.class);
    }
}
```

- [ ] **Step 3: Run tests — confirm they fail**

```bash
mvn test -Dtest=ProgressServiceTest 2>&1 | tail -5
```

Expected: compilation error.

- [ ] **Step 4: Commit failing tests**

```bash
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit add backend/src/test/java/com/github/mwacha/wachafit/progress/ProgressServiceTest.java
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit commit -m "test: Progress unit tests (failing)"
```

- [ ] **Step 5: Create ProgressPhoto entity**

```java
// ProgressPhoto.java
package com.github.mwacha.wachafit.progress;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "progress_photos")
public class ProgressPhoto {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "student_id", nullable = false) private UUID studentId;
    @Column(name = "uploaded_by", nullable = false) private UUID uploadedBy;
    @Column(name = "storage_key", nullable = false, length = 255) private String storageKey;
    @Column(name = "taken_at", nullable = false) private LocalDate takenAt;
    @Column(length = 200) private String notes;
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false) private Instant createdAt;

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID v) { this.studentId = v; }
    public UUID getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(UUID v) { this.uploadedBy = v; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String v) { this.storageKey = v; }
    public LocalDate getTakenAt() { return takenAt; }
    public void setTakenAt(LocalDate v) { this.takenAt = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
    public Instant getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 6: Create ProgressPhotoRepository**

```java
// ProgressPhotoRepository.java
package com.github.mwacha.wachafit.progress;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ProgressPhotoRepository extends JpaRepository<ProgressPhoto, UUID> {
    List<ProgressPhoto> findByStudentIdOrderByTakenAtDesc(UUID studentId);
}
```

- [ ] **Step 7: Create PhotoResponse DTO**

```java
// dto/PhotoResponse.java
package com.github.mwacha.wachafit.progress.dto;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
public record PhotoResponse(UUID id, UUID studentId, UUID uploadedBy, LocalDate takenAt, String notes, String fileUrl, Instant createdAt) {}
```

- [ ] **Step 8: Create ProgressService**

```java
// ProgressService.java
package com.github.mwacha.wachafit.progress;

import com.github.mwacha.wachafit.progress.dto.PhotoResponse;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.shared.exception.ForbiddenException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ProgressService {

    private final ProgressPhotoRepository repo;
    private final Path uploadDir;

    public ProgressService(ProgressPhotoRepository repo,
            @Value("${app.upload-dir:uploads}") String uploadDir) {
        this.repo = repo;
        this.uploadDir = Paths.get(uploadDir, "photos");
    }

    public PhotoResponse upload(UUID studentId, MultipartFile file, LocalDate takenAt, String notes, User uploadedBy) {
        assertCanAccessOrUpload(studentId, uploadedBy);
        try {
            Path dir = uploadDir.resolve(studentId.toString());
            Files.createDirectories(dir);
            String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + (ext != null ? "." + ext : "");
            file.transferTo(dir.resolve(filename));
            String storageKey = studentId + "/" + filename;
            ProgressPhoto photo = new ProgressPhoto();
            photo.setStudentId(studentId);
            photo.setUploadedBy(uploadedBy.getId());
            photo.setStorageKey(storageKey);
            photo.setTakenAt(takenAt != null ? takenAt : LocalDate.now());
            photo.setNotes(notes);
            return toResponse(repo.save(photo));
        } catch (IOException e) {
            throw new BusinessException("Failed to store photo: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<PhotoResponse> list(UUID studentId, User requestingUser) {
        assertCanAccess(studentId, requestingUser);
        return repo.findByStudentIdOrderByTakenAtDesc(studentId).stream()
            .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Resource loadFile(UUID photoId, User requestingUser) {
        ProgressPhoto photo = repo.findById(photoId)
            .orElseThrow(() -> new NotFoundException("Photo not found"));
        assertCanAccess(photo.getStudentId(), requestingUser);
        Path file = uploadDir.resolve(photo.getStorageKey());
        Resource resource = new FileSystemResource(file);
        if (!resource.exists()) throw new NotFoundException("File not found on disk");
        return resource;
    }

    public void delete(UUID photoId, User requestingUser) {
        ProgressPhoto photo = repo.findById(photoId)
            .orElseThrow(() -> new NotFoundException("Photo not found"));
        assertCanAccess(photo.getStudentId(), requestingUser);
        try { Files.deleteIfExists(uploadDir.resolve(photo.getStorageKey())); }
        catch (IOException ignored) {}
        repo.delete(photo);
    }

    private void assertCanAccess(UUID studentId, User requestingUser) {
        if (requestingUser.getRole() == Role.STUDENT && !studentId.equals(requestingUser.getId())) {
            throw new ForbiddenException("Access denied");
        }
    }

    private void assertCanAccessOrUpload(UUID studentId, User uploadedBy) {
        if (uploadedBy.getRole() == Role.ADMIN) throw new ForbiddenException("ADMIN cannot upload photos");
        if (uploadedBy.getRole() == Role.STUDENT && !studentId.equals(uploadedBy.getId())) {
            throw new ForbiddenException("Access denied");
        }
    }

    private PhotoResponse toResponse(ProgressPhoto p) {
        return new PhotoResponse(p.getId(), p.getStudentId(), p.getUploadedBy(),
            p.getTakenAt(), p.getNotes(), "/api/photos/" + p.getId() + "/file", p.getCreatedAt());
    }
}
```

- [ ] **Step 9: Create ProgressController**

```java
// ProgressController.java
package com.github.mwacha.wachafit.progress;

import com.github.mwacha.wachafit.progress.dto.PhotoResponse;
import com.github.mwacha.wachafit.user.User;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
public class ProgressController {

    private final ProgressService service;

    public ProgressController(ProgressService service) { this.service = service; }

    @PostMapping(value = "/api/students/{studentId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('STUDENT','TRAINER')")
    public ResponseEntity<PhotoResponse> upload(
            @PathVariable UUID studentId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) LocalDate takenAt,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(service.upload(studentId, file, takenAt, notes, currentUser));
    }

    @GetMapping("/api/students/{studentId}/photos")
    @PreAuthorize("isAuthenticated()")
    public List<PhotoResponse> list(@PathVariable UUID studentId,
            @AuthenticationPrincipal User currentUser) {
        return service.list(studentId, currentUser);
    }

    @GetMapping("/api/photos/{id}/file")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> getFile(@PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        Resource resource = service.loadFile(id, currentUser);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource);
    }

    @DeleteMapping("/api/photos/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        service.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 10: Write integration test**

```java
// backend/src/test/java/com/github/mwacha/wachafit/progress/ProgressControllerIntegrationTest.java
package com.github.mwacha.wachafit.progress;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.mock.web.MockMultipartFile;
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
class ProgressControllerIntegrationTest {

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

    private String studentToken;
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
            .content(mapper.writeValueAsString(new LoginRequest("s@t.com", "pass")))).andReturn();
        studentToken = mapper.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void upload_withStudentToken_shouldReturn201() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg",
            MediaType.IMAGE_JPEG_VALUE, "fake-image-bytes".getBytes());
        mvc.perform(multipart("/api/students/" + studentId + "/photos")
                .file(file)
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.studentId").value(studentId.toString()))
            .andExpect(jsonPath("$.fileUrl").isString());
    }

    @Test
    void list_withStudentToken_shouldReturn200() throws Exception {
        mvc.perform(get("/api/students/" + studentId + "/photos")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void upload_withoutToken_shouldReturn401() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg",
            MediaType.IMAGE_JPEG_VALUE, "bytes".getBytes());
        mvc.perform(multipart("/api/students/" + UUID.randomUUID() + "/photos").file(file))
            .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 11: Run all tests**

```bash
cd /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit/backend
mvn test -Dtest=ProgressServiceTest,ProgressControllerIntegrationTest 2>&1 | tail -10
mvn test 2>&1 | tail -5
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 12: Commit**

```bash
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit add \
    backend/src/main/java/com/github/mwacha/wachafit/progress/ \
    backend/src/test/java/com/github/mwacha/wachafit/progress/ProgressControllerIntegrationTest.java \
    backend/src/main/resources/application-dev.yml \
    backend/src/main/resources/application-test.yml
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit commit -m "feat: Progress photos module with local disk storage (T-17)"
```
