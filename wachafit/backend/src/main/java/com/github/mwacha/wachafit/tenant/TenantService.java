package com.github.mwacha.wachafit.tenant;

import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.tenant.dto.CreateTenantRequest;
import com.github.mwacha.wachafit.tenant.dto.TenantResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TenantService {

    private final TenantRepository repo;

    public TenantService(TenantRepository repo) { this.repo = repo; }

    public TenantResponse create(CreateTenantRequest req) {
        if (repo.findBySlug(req.slug()).isPresent()) {
            throw new BusinessException("Slug já em uso: " + req.slug());
        }
        Tenant t = new Tenant();
        t.setName(req.name());
        t.setSlug(req.slug());
        Tenant saved = repo.save(t);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> list() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    private TenantResponse toResponse(Tenant t) {
        return new TenantResponse(
            t.getId().toString(), t.getName(), t.getSlug(),
            t.isActive(),
            t.getCreatedAt() != null ? t.getCreatedAt().toString() : null
        );
    }
}
