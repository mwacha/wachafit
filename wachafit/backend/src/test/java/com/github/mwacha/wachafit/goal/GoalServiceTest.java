package com.github.mwacha.wachafit.goal;

import com.github.mwacha.wachafit.goal.dto.*;
import com.github.mwacha.wachafit.shared.exception.ForbiddenException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import com.github.mwacha.wachafit.tenant.TenantContext;
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
import java.time.Instant;
import java.time.LocalDate;
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
    private UUID trainerId;

    @BeforeEach
    void setUp() {
        trainerId = UUID.randomUUID();
        studentId = UUID.randomUUID();

        trainer = new User();
        trainer.setName("Trainer");
        trainer.setEmail("trainer@test.com");
        trainer.setRole(Role.TRAINER);

        student = new User();
        student.setName("Student");
        student.setEmail("student@test.com");
        student.setRole(Role.STUDENT);
    }

    @Test
    void create_shouldPersistAndReturnGoalResponse() {
        when(userRepo.existsByIdAndTenantId(eq(studentId), any())).thenReturn(true);
        StudentGoal saved = new StudentGoal();
        saved.setId(UUID.randomUUID());
        saved.setStudentId(studentId);
        saved.setCreatedBy(trainerId);
        saved.setDescription("Lose 5kg");
        saved.setMetric("weight");
        saved.setTargetValue(new BigDecimal("5.00"));
        saved.setTargetDate(LocalDate.of(2026, 12, 31));
        saved.setStatus(GoalStatus.IN_PROGRESS);
        saved.setCreatedAt(Instant.now());
        when(repo.save(any())).thenReturn(saved);

        GoalResponse resp = service.create(studentId,
            new CreateGoalRequest("Lose 5kg", "weight", new BigDecimal("5.00"), LocalDate.of(2026, 12, 31)),
            trainerId);

        assertThat(resp.status()).isEqualTo("IN_PROGRESS");
        assertThat(resp.description()).isEqualTo("Lose 5kg");
        verify(repo).save(any());
    }

    @Test
    void create_shouldThrowNotFound_whenStudentMissing() {
        when(userRepo.existsByIdAndTenantId(eq(studentId), any())).thenReturn(false);

        assertThatThrownBy(() -> service.create(studentId,
            new CreateGoalRequest("Lose 5kg", "weight", null, null), trainerId))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void create_throwsNotFound_whenStudentBelongsToDifferentTenant() {
        UUID myTenantId = UUID.randomUUID();
        TenantContext.set(myTenantId);
        try {
            when(userRepo.existsByIdAndTenantId(studentId, myTenantId)).thenReturn(false);

            assertThatThrownBy(() -> service.create(studentId,
                new CreateGoalRequest("Lose 5kg", "weight", null, null), trainerId))
                .isInstanceOf(NotFoundException.class);

            verify(repo, never()).save(any());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void list_shouldReturnGoalsForStudent() {
        StudentGoal g = new StudentGoal();
        g.setId(UUID.randomUUID());
        g.setStudentId(studentId);
        g.setCreatedBy(trainerId);
        g.setDescription("Run 5km");
        g.setStatus(GoalStatus.IN_PROGRESS);
        g.setCreatedAt(Instant.now());
        when(repo.findByStudentIdOrderByCreatedAtDesc(studentId)).thenReturn(List.of(g));

        List<GoalResponse> results = service.list(studentId, trainer);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).description()).isEqualTo("Run 5km");
    }

    @Test
    void list_asStudent_shouldOnlySeeOwnGoals_forbidden_whenOtherStudent() {
        // Inject studentId into student via reflection
        try { var f = User.class.getDeclaredField("id"); f.setAccessible(true); f.set(student, studentId); }
        catch (Exception e) { throw new RuntimeException(e); }

        when(repo.findByStudentIdOrderByCreatedAtDesc(studentId)).thenReturn(List.of());
        List<GoalResponse> results = service.list(studentId, student);
        assertThat(results).isEmpty();

        // Also verify a different student cannot access these goals
        User otherStudent = new User();
        otherStudent.setRole(Role.STUDENT);
        try { var f = User.class.getDeclaredField("id"); f.setAccessible(true); f.set(otherStudent, UUID.randomUUID()); }
        catch (Exception e) { throw new RuntimeException(e); }
        assertThatThrownBy(() -> service.list(studentId, otherStudent))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateStatus_shouldChangeStatus() {
        UUID goalId = UUID.randomUUID();
        StudentGoal goal = new StudentGoal();
        goal.setId(goalId);
        goal.setStudentId(studentId);
        goal.setCreatedBy(trainerId);
        goal.setDescription("Lose 5kg");
        goal.setStatus(GoalStatus.IN_PROGRESS);
        goal.setCreatedAt(Instant.now());
        when(repo.findById(goalId)).thenReturn(Optional.of(goal));
        when(repo.save(any())).thenReturn(goal);

        GoalResponse resp = service.updateStatus(goalId, new UpdateGoalStatusRequest("ACHIEVED"), trainer);

        assertThat(resp.status()).isEqualTo("ACHIEVED");
    }

    @Test
    void updateStatus_shouldThrowNotFound_whenGoalMissing() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateStatus(UUID.randomUUID(),
            new UpdateGoalStatusRequest("ACHIEVED"), trainer))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateStatus_asStudent_shouldThrowForbidden() {
        UUID goalId = UUID.randomUUID();
        StudentGoal goal = new StudentGoal();
        goal.setId(goalId);
        goal.setStudentId(UUID.randomUUID()); // different student
        goal.setCreatedBy(trainerId);
        goal.setDescription("Lose 5kg");
        goal.setStatus(GoalStatus.IN_PROGRESS);
        goal.setCreatedAt(Instant.now());
        when(repo.findById(goalId)).thenReturn(Optional.of(goal));

        assertThatThrownBy(() -> service.updateStatus(goalId,
            new UpdateGoalStatusRequest("ACHIEVED"), student))
            .isInstanceOf(ForbiddenException.class);
    }
}
