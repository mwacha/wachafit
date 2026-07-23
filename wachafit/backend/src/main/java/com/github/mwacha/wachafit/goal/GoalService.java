package com.github.mwacha.wachafit.goal;

import com.github.mwacha.wachafit.goal.dto.*;
import com.github.mwacha.wachafit.shared.exception.ForbiddenException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import com.github.mwacha.wachafit.tenant.TenantContext;
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
        if (!userRepo.existsByIdAndTenantId(studentId, TenantContext.get())) {
            throw new NotFoundException("Student not found: " + studentId);
        }

        StudentGoal goal = new StudentGoal();
        goal.setStudentId(studentId);
        goal.setCreatedBy(createdById);
        goal.setDescription(req.description());
        goal.setMetric(req.metric());
        goal.setTargetValue(req.targetValue());
        goal.setTargetDate(req.targetDate());
        goal.setStatus(GoalStatus.IN_PROGRESS);

        return toResponse(repo.save(goal));
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> list(UUID studentId, User requestingUser) {
        // STUDENT can only view their own goals
        if (requestingUser.getRole() == Role.STUDENT
                && !studentId.equals(requestingUser.getId())) {
            throw new ForbiddenException("Students can only view their own goals");
        }
        return repo.findByStudentIdOrderByCreatedAtDesc(studentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public GoalResponse updateStatus(UUID goalId, UpdateGoalStatusRequest req, User requestingUser) {
        StudentGoal goal = repo.findById(goalId)
            .orElseThrow(() -> new NotFoundException("Goal not found: " + goalId));

        // STUDENT cannot update status (only TRAINER/ADMIN can)
        if (requestingUser.getRole() == Role.STUDENT) {
            throw new ForbiddenException("Students cannot update goal status");
        }

        GoalStatus newStatus;
        try {
            newStatus = GoalStatus.valueOf(req.status());
        } catch (IllegalArgumentException e) {
            throw new com.github.mwacha.wachafit.shared.exception.BusinessException(
                "Invalid status: " + req.status());
        }

        goal.setStatus(newStatus);
        return toResponse(repo.save(goal));
    }

    public void delete(UUID goalId, User requestingUser) {
        StudentGoal goal = repo.findById(goalId)
            .orElseThrow(() -> new NotFoundException("Goal not found: " + goalId));

        // Only TRAINER who created it or ADMIN can delete
        if (requestingUser.getRole() == Role.STUDENT) {
            throw new ForbiddenException("Students cannot delete goals");
        }
        if (requestingUser.getRole() == Role.TRAINER
                && !goal.getCreatedBy().equals(requestingUser.getId())) {
            throw new ForbiddenException("Trainers can only delete goals they created");
        }

        repo.delete(goal);
    }

    private GoalResponse toResponse(StudentGoal g) {
        return new GoalResponse(
            g.getId(),
            g.getStudentId(),
            g.getCreatedBy(),
            g.getDescription(),
            g.getMetric(),
            g.getTargetValue(),
            g.getTargetDate(),
            g.getStatus().name(),
            g.getCreatedAt()
        );
    }
}
