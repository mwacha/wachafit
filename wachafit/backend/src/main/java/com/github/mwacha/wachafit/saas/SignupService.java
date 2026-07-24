package com.github.mwacha.wachafit.saas;

import com.github.mwacha.wachafit.auth.dto.LoginResponse;
import com.github.mwacha.wachafit.saas.dto.SignupRequest;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.shared.security.JwtUtil;
import com.github.mwacha.wachafit.tenant.Tenant;
import com.github.mwacha.wachafit.tenant.TenantRepository;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class SignupService {

    private static final Logger log = LoggerFactory.getLogger(SignupService.class);
    private static final int TRIAL_DAYS = 14;

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final SaasPlanRepository saasPlanRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final TenantChargeRepository chargeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public SignupService(
        TenantRepository tenantRepository,
        UserRepository userRepository,
        SaasPlanRepository saasPlanRepository,
        TenantSubscriptionRepository subscriptionRepository,
        TenantChargeRepository chargeRepository,
        PasswordEncoder passwordEncoder,
        JwtUtil jwtUtil
    ) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.saasPlanRepository = saasPlanRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.chargeRepository = chargeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public LoginResponse signup(SignupRequest req) {
        if (tenantRepository.findBySlug(req.company().slug()).isPresent()) {
            throw new BusinessException("Slug já em uso: " + req.company().slug());
        }
        if (tenantRepository.findByCnpj(req.company().cnpj()).isPresent()) {
            throw new BusinessException("CNPJ já cadastrado");
        }
        if (!CnpjValidator.isValid(req.company().cnpj())) {
            throw new BusinessException("CNPJ inválido");
        }
        SaasPlan plan = saasPlanRepository.findById(req.plan().saasPlanId())
            .filter(SaasPlan::isActive)
            .orElseThrow(() -> new BusinessException("Plano inválido ou inativo"));

        Tenant tenant = new Tenant();
        tenant.setName(req.company().name());
        tenant.setSlug(req.company().slug());
        tenant.setCnpj(req.company().cnpj());
        tenant.setPhone(req.company().phone());
        tenant.setActive(true);
        tenant = tenantRepository.save(tenant);

        User admin = new User();
        admin.setName(req.admin().name());
        admin.setEmail(req.admin().email());
        admin.setPasswordHash(passwordEncoder.encode(req.admin().password()));
        admin.setRole(Role.ADMIN);
        admin.setTenant(tenant);
        admin = userRepository.save(admin);

        Instant trialEndsAt = Instant.now().plus(TRIAL_DAYS, ChronoUnit.DAYS);
        TenantSubscription subscription = new TenantSubscription();
        subscription.setTenantId(tenant.getId());
        subscription.setSaasPlanId(plan.getId());
        subscription.setStatus("TRIALING");
        subscription.setTrialEndsAt(trialEndsAt);
        subscription = subscriptionRepository.save(subscription);

        // Best-effort: se a cobrança inicial falhar, a conta é criada mesmo assim.
        // A ausência de charge fica visível para o SUPER_ADMIN reconciliar manualmente depois.
        createFirstCharge(tenant.getId(), subscription.getId(), plan.getPrice(),
            trialEndsAt, req.plan().paymentMethod());

        String token = jwtUtil.generateToken(admin);
        return new LoginResponse(token, admin.getRole().name(), admin.getId().toString(),
            tenant.getId().toString());
    }

    private void createFirstCharge(UUID tenantId, UUID subscriptionId, BigDecimal amount,
                                    Instant dueInstant, PaymentMethod method) {
        try {
            TenantCharge charge = new TenantCharge();
            charge.setTenantId(tenantId);
            charge.setSubscriptionId(subscriptionId);
            charge.setAmount(amount);
            charge.setDueDate(LocalDate.ofInstant(dueInstant, ZoneOffset.UTC));
            charge.setStatus("PENDING");
            charge.setPaymentMethod(method.name());
            chargeRepository.save(charge);
        } catch (Exception e) {
            log.error("Falha ao criar cobrança inicial do tenant {}: {}", tenantId, e.getMessage(), e);
        }
    }
}
