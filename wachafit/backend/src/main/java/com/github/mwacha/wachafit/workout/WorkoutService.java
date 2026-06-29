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
        this.planRepo = planRepo;
        this.itemRepo = itemRepo;
        this.logRepo = logRepo;
        this.prRepo = prRepo;
        this.userRepo = userRepo;
    }

    public WorkoutPlanResponse createPlan(UUID studentId, CreateWorkoutPlanRequest req, UUID trainerId) {
        userRepo.findById(studentId)
            .orElseThrow(() -> new NotFoundException("Student not found"));
        WorkoutPlan plan = new WorkoutPlan();
        plan.setStudentId(studentId);
        plan.setTrainerId(trainerId);
        plan.setName(req.name());
        plan.setDescription(req.description());
        plan.setActive(true);
        if (req.items() != null) {
            for (WorkoutPlanItemRequest i : req.items()) {
                WorkoutPlanItem item = new WorkoutPlanItem();
                item.setExerciseId(i.exerciseId());
                item.setDivision(i.division());
                item.setSets(i.sets());
                item.setReps(i.reps());
                item.setSuggestedLoadKg(i.suggestedLoadKg());
                item.setRestSeconds(i.restSeconds());
                item.setOrderIndex(i.orderIndex());
                item.setNotes(i.notes());
                item.setWorkoutPlan(plan);
                plan.getItems().add(item);
            }
        }
        return toPlanResponse(planRepo.save(plan));
    }

    @Transactional(readOnly = true)
    public List<WorkoutPlanResponse> listPlans(UUID studentId, User requestingUser) {
        assertCanAccess(studentId, requestingUser);
        return planRepo.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
            .map(this::toPlanResponse)
            .toList();
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
        plan.setName(req.name());
        plan.setDescription(req.description());
        plan.getItems().clear();
        if (req.items() != null) {
            for (WorkoutPlanItemRequest i : req.items()) {
                WorkoutPlanItem item = new WorkoutPlanItem();
                item.setExerciseId(i.exerciseId());
                item.setDivision(i.division());
                item.setSets(i.sets());
                item.setReps(i.reps());
                item.setSuggestedLoadKg(i.suggestedLoadKg());
                item.setRestSeconds(i.restSeconds());
                item.setOrderIndex(i.orderIndex());
                item.setNotes(i.notes());
                item.setWorkoutPlan(plan);
                plan.getItems().add(item);
            }
        }
        return toPlanResponse(planRepo.save(plan));
    }

    public WorkoutPlanResponse activatePlan(UUID planId, User requestingUser) {
        // Load to get studentId, then bulk-deactivate (clears persistence context via clearAutomatically=true)
        UUID studentId = planRepo.findById(planId)
            .orElseThrow(() -> new NotFoundException("Plan not found"))
            .getStudentId();
        planRepo.deactivateAllForStudent(studentId);
        // Re-fetch after cache clear, set active, save
        WorkoutPlan plan = planRepo.findById(planId)
            .orElseThrow(() -> new NotFoundException("Plan not found"));
        plan.setActive(true);
        return toPlanResponse(planRepo.save(plan));
    }

    public WorkoutLogResponse createLog(UUID studentId, CreateWorkoutLogRequest req, User requestingUser) {
        if (requestingUser.getRole() == Role.STUDENT && !studentId.equals(requestingUser.getId())) {
            throw new ForbiddenException("Students can only log their own workouts");
        }
        WorkoutLog log = new WorkoutLog();
        log.setStudentId(studentId);
        log.setExerciseId(req.exerciseId());
        log.setWorkoutPlanItemId(req.workoutPlanItemId());
        log.setPerformedAt(req.performedAt());
        log.setSets(req.sets());
        log.setReps(req.reps());
        log.setLoadKg(req.loadKg());
        log.setNotes(req.notes());
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
                    pr.setStudentId(studentId);
                    pr.setExerciseId(req.exerciseId());
                    pr.setRecordLoadKg(req.loadKg());
                    pr.setAchievedAt(req.performedAt());
                    prRepo.save(pr);
                });
        }
        return toLogResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<WorkoutLogResponse> listLogs(UUID studentId, User requestingUser) {
        assertCanAccess(studentId, requestingUser);
        return logRepo.findByStudentIdOrderByPerformedAtDesc(studentId).stream()
            .map(this::toLogResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PersonalRecordResponse> listRecords(UUID studentId, User requestingUser) {
        assertCanAccess(studentId, requestingUser);
        return prRepo.findByStudentIdOrderByAchievedAtDesc(studentId).stream()
            .map(pr -> new PersonalRecordResponse(pr.getId(), pr.getStudentId(),
                pr.getExerciseId(), pr.getRecordLoadKg(), pr.getAchievedAt()))
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
