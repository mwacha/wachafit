package com.github.mwacha.wachafit.saas;

import com.github.mwacha.wachafit.auth.dto.LoginResponse;
import com.github.mwacha.wachafit.saas.dto.SignupRequest;
import com.github.mwacha.wachafit.tenant.TenantRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicSignupController {

    private final SignupService signupService;
    private final SaasPlanRepository saasPlanRepository;
    private final TenantRepository tenantRepository;

    public PublicSignupController(SignupService signupService, SaasPlanRepository saasPlanRepository,
                                   TenantRepository tenantRepository) {
        this.signupService = signupService;
        this.saasPlanRepository = saasPlanRepository;
        this.tenantRepository = tenantRepository;
    }

    @GetMapping("/saas-plans")
    public List<SaasPlan> listActivePlans() {
        return saasPlanRepository.findByActiveTrueOrderByPriceAsc();
    }

    @GetMapping("/check-slug")
    public Map<String, Boolean> checkSlug(@RequestParam String slug) {
        return Map.of("available", tenantRepository.findBySlug(slug).isEmpty());
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse signup(@Valid @RequestBody SignupRequest req) {
        return signupService.signup(req);
    }
}
