package com.github.mwacha.wachafit.saas;

import com.github.mwacha.wachafit.saas.dto.CreateSaasPlanRequest;
import com.github.mwacha.wachafit.saas.dto.SaasPlanResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/super/saas-plans")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SaasPlanController {

    private final SaasPlanService service;

    public SaasPlanController(SaasPlanService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaasPlanResponse create(@Valid @RequestBody CreateSaasPlanRequest req) {
        return service.create(req);
    }

    @GetMapping
    public List<SaasPlanResponse> list() {
        return service.list();
    }

    @PutMapping("/{id}")
    public SaasPlanResponse update(@PathVariable UUID id, @Valid @RequestBody CreateSaasPlanRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID id) {
        service.deactivate(id);
    }
}
