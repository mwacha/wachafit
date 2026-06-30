package com.github.mwacha.wachafit.membership;

import com.github.mwacha.wachafit.membership.dto.CreatePlanRequest;
import com.github.mwacha.wachafit.membership.dto.PlanResponse;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MembershipPlanService {

    private final MembershipPlanRepository planRepo;

    public MembershipPlanService(MembershipPlanRepository planRepo) {
        this.planRepo = planRepo;
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> getAllPlans() {
        return planRepo.findAll().stream().map(this::toResponse).toList();
    }

    public PlanResponse createPlan(CreatePlanRequest req) {
        MembershipPlan plan = new MembershipPlan();
        plan.setName(req.name());
        plan.setDescription(req.description());
        plan.setDurationMonths(req.durationMonths());
        plan.setPrice(req.price());
        plan.setMaxClassesPerWeek(req.maxClassesPerWeek());
        return toResponse(planRepo.save(plan));
    }

    public PlanResponse updatePlan(UUID id, CreatePlanRequest req) {
        MembershipPlan plan = planRepo.findById(id)
            .orElseThrow(() -> new NotFoundException("Plano não encontrado"));
        plan.setName(req.name());
        plan.setDescription(req.description());
        plan.setDurationMonths(req.durationMonths());
        plan.setPrice(req.price());
        plan.setMaxClassesPerWeek(req.maxClassesPerWeek());
        return toResponse(planRepo.save(plan));
    }

    public void deactivatePlan(UUID id) {
        MembershipPlan plan = planRepo.findById(id)
            .orElseThrow(() -> new NotFoundException("Plano não encontrado"));
        plan.setActive(false);
        planRepo.save(plan);
    }

    private PlanResponse toResponse(MembershipPlan p) {
        return new PlanResponse(p.getId(), p.getName(), p.getDescription(),
            p.getDurationMonths(), p.getPrice(), p.getMaxClassesPerWeek(),
            p.isActive(), p.getCreatedAt());
    }
}
