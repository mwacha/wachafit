package com.github.mwacha.wachafit.workout;

import com.github.mwacha.wachafit.notification.event.WorkoutPlanAssignedEvent;
import com.github.mwacha.wachafit.shared.exception.ForbiddenException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import com.github.mwacha.wachafit.tenant.TenantContext;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import com.github.mwacha.wachafit.workout.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

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
    @Mock ApplicationEventPublisher eventPublisher;

    WorkoutService service;

    private User trainer;
    private User student;
    private UUID studentId;
    private UUID trainerId;

    @BeforeEach
    void setUp() {
        service = new WorkoutService(planRepo, itemRepo, logRepo, prRepo, userRepo, eventPublisher);
        studentId = UUID.randomUUID();
        trainerId = UUID.randomUUID();
        student = new User();
        student.setRole(Role.STUDENT);
        try {
            var f = User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(student, studentId);
        } catch (Exception e) { throw new RuntimeException(e); }
        trainer = new User();
        trainer.setRole(Role.TRAINER);
        try {
            var f = User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(trainer, trainerId);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void createPlan_shouldPersistPlanWithItems() {
        when(userRepo.existsByIdAndTenantId(eq(studentId), any())).thenReturn(true);
        when(planRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        CreateWorkoutPlanRequest req = new CreateWorkoutPlanRequest("Plan A", null,
            List.of(new WorkoutPlanItemRequest(UUID.randomUUID(), "A", 3, "12", null, 60, 1, null)));
        WorkoutPlanResponse res = service.createPlan(studentId, req, trainerId);
        assertThat(res.name()).isEqualTo("Plan A");
        assertThat(res.items()).hasSize(1);
    }

    @Test
    void createPlan_shouldThrowNotFound_whenStudentMissing() {
        when(userRepo.existsByIdAndTenantId(any(), any())).thenReturn(false);
        CreateWorkoutPlanRequest req = new CreateWorkoutPlanRequest("Plan A", null, List.of());
        assertThatThrownBy(() -> service.createPlan(studentId, req, trainerId))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createPlan_throwsNotFound_whenStudentBelongsToDifferentTenant() {
        UUID myTenantId = UUID.randomUUID();
        TenantContext.set(myTenantId);
        try {
            when(userRepo.existsByIdAndTenantId(studentId, myTenantId)).thenReturn(false);
            CreateWorkoutPlanRequest req = new CreateWorkoutPlanRequest("Plan A", null, List.of());

            assertThatThrownBy(() -> service.createPlan(studentId, req, trainerId))
                .isInstanceOf(NotFoundException.class);

            verify(planRepo, never()).save(any());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void activatePlan_shouldDeactivateOthersAndActivateTarget() {
        WorkoutPlan plan = new WorkoutPlan();
        plan.setStudentId(studentId);
        plan.setActive(false);
        UUID planId = UUID.randomUUID();
        try {
            var f = WorkoutPlan.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(plan, planId);
        } catch (Exception e) { throw new RuntimeException(e); }
        // findById called twice: once to get studentId, once after cache clear
        when(planRepo.findById(planId)).thenReturn(Optional.of(plan));
        when(planRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        WorkoutPlanResponse res = service.activatePlan(planId, trainer);
        verify(planRepo).deactivateAllForStudent(studentId);
        assertThat(res.active()).isTrue();
    }

    @Test
    void activatePlan_shouldThrowNotFound_whenPlanMissing() {
        when(planRepo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.activatePlan(UUID.randomUUID(), trainer))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createLog_shouldUpsertPersonalRecord_whenNewRecord() {
        when(prRepo.findByStudentIdAndExerciseId(any(), any())).thenReturn(Optional.empty());
        when(logRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
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
        when(prRepo.findByStudentIdAndExerciseId(any(), any())).thenReturn(Optional.of(existing));
        UUID exerciseId = UUID.randomUUID();
        service.createLog(studentId,
            new CreateWorkoutLogRequest(exerciseId, null, LocalDate.now(), 3, 12, new BigDecimal("100"), null),
            student);
        verify(prRepo, never()).save(any());
    }

    @Test
    void createLog_shouldUpdatePersonalRecord_whenLoadSuperior() {
        PersonalRecord existing = new PersonalRecord();
        existing.setRecordLoadKg(new BigDecimal("90"));
        when(logRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(prRepo.findByStudentIdAndExerciseId(any(), any())).thenReturn(Optional.of(existing));
        when(prRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        UUID exerciseId = UUID.randomUUID();
        service.createLog(studentId,
            new CreateWorkoutLogRequest(exerciseId, null, LocalDate.now(), 3, 12, new BigDecimal("100"), null),
            student);
        verify(prRepo).save(existing);
        assertThat(existing.getRecordLoadKg()).isEqualByComparingTo("100");
    }

    @Test
    void createLog_shouldThrowForbidden_whenStudentLogsForOther() {
        UUID otherId = UUID.randomUUID();
        assertThatThrownBy(() -> service.createLog(otherId,
            new CreateWorkoutLogRequest(UUID.randomUUID(), null, LocalDate.now(), 3, 12, null, null),
            student))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void listPlans_shouldThrowForbidden_whenStudentAccessesOtherStudent() {
        UUID otherId = UUID.randomUUID();
        assertThatThrownBy(() -> service.listPlans(otherId, student))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void listPlans_shouldAllowTrainerToAccessAnyStudent() {
        when(planRepo.findByStudentIdOrderByCreatedAtDesc(studentId)).thenReturn(List.of());
        assertThatCode(() -> service.listPlans(studentId, trainer)).doesNotThrowAnyException();
    }

    @Test
    void createLog_shouldNotCreatePersonalRecord_whenNoLoad() {
        when(logRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        UUID exerciseId = UUID.randomUUID();
        service.createLog(studentId,
            new CreateWorkoutLogRequest(exerciseId, null, LocalDate.now(), 3, 12, null, null),
            student);
        verify(prRepo, never()).findByStudentIdAndExerciseId(any(), any());
        verify(prRepo, never()).save(any());
    }

    @Test
    void createPlan_shouldPublishWorkoutPlanAssignedEvent() {
        when(userRepo.existsByIdAndTenantId(eq(studentId), any())).thenReturn(true);
        when(planRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        CreateWorkoutPlanRequest req = new CreateWorkoutPlanRequest("Plano Verão", null, List.of());
        service.createPlan(studentId, req, trainerId);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(WorkoutPlanAssignedEvent.class);
        WorkoutPlanAssignedEvent evt = (WorkoutPlanAssignedEvent) captor.getValue();
        assertThat(evt.studentId()).isEqualTo(studentId);
        assertThat(evt.trainerId()).isEqualTo(trainerId);
        assertThat(evt.planName()).isEqualTo("Plano Verão");
    }

    @Test
    void activatePlan_shouldPublishWorkoutPlanAssignedEvent() {
        WorkoutPlan plan = new WorkoutPlan();
        plan.setStudentId(studentId);
        plan.setTrainerId(trainerId);
        plan.setName("Plano B");
        plan.setActive(false);
        UUID planId = UUID.randomUUID();
        try {
            var f = WorkoutPlan.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(plan, planId);
        } catch (Exception e) { throw new RuntimeException(e); }
        when(planRepo.findById(planId)).thenReturn(Optional.of(plan));
        when(planRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.activatePlan(planId, trainer);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(WorkoutPlanAssignedEvent.class);
        WorkoutPlanAssignedEvent evt = (WorkoutPlanAssignedEvent) captor.getValue();
        assertThat(evt.studentId()).isEqualTo(studentId);
        assertThat(evt.planName()).isEqualTo("Plano B");
    }
}
