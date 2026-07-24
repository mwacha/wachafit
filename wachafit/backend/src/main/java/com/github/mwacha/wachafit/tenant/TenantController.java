package com.github.mwacha.wachafit.tenant;

import com.github.mwacha.wachafit.tenant.dto.CreateTenantRequest;
import com.github.mwacha.wachafit.tenant.dto.TenantResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/super/tenants")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class TenantController {

    private final TenantService service;

    public TenantController(TenantService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TenantResponse create(@Valid @RequestBody CreateTenantRequest req) {
        return service.create(req);
    }

    @GetMapping
    public List<TenantResponse> list() {
        return service.list();
    }
}
