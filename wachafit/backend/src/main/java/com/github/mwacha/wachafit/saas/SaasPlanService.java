package com.github.mwacha.wachafit.saas;

import com.github.mwacha.wachafit.saas.dto.CreateSaasPlanRequest;
import com.github.mwacha.wachafit.saas.dto.SaasPlanResponse;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SaasPlanService {

    private final SaasPlanRepository repo;

    public SaasPlanService(SaasPlanRepository repo) { this.repo = repo; }

    public SaasPlanResponse create(CreateSaasPlanRequest req) {
        SaasPlan plan = new SaasPlan();
        applyRequest(plan, req);
        return toResponse(repo.save(plan));
    }

    public SaasPlanResponse update(UUID id, CreateSaasPlanRequest req) {
        SaasPlan plan = repo.findById(id).orElseThrow(() -> new NotFoundException("Plano não encontrado"));
        applyRequest(plan, req);
        return toResponse(repo.save(plan));
    }

    public void deactivate(UUID id) {
        SaasPlan plan = repo.findById(id).orElseThrow(() -> new NotFoundException("Plano não encontrado"));
        plan.setActive(false);
        repo.save(plan);
    }

    @Transactional(readOnly = true)
    public List<SaasPlanResponse> list() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    private void applyRequest(SaasPlan plan, CreateSaasPlanRequest req) {
        plan.setName(req.name());
        plan.setDescription(req.description());
        plan.setPrice(req.price());
        plan.setBillingPeriodMonths(req.billingPeriodMonths());
        plan.setMaxUsers(req.maxUsers());
    }

    private SaasPlanResponse toResponse(SaasPlan p) {
        return new SaasPlanResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice(),
            p.getBillingPeriodMonths(), p.getMaxUsers(), p.isActive(), p.getCreatedAt());
    }
}
