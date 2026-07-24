package com.github.mwacha.wachafit.saas;

import com.github.mwacha.wachafit.auth.dto.LoginResponse;
import com.github.mwacha.wachafit.saas.dto.SignupRequest;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.shared.security.JwtUtil;
import com.github.mwacha.wachafit.tenant.Tenant;
import com.github.mwacha.wachafit.tenant.TenantRepository;
import com.github.mwacha.wachafit.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SignupServiceTest {

    private TenantRepository tenantRepository;
    private UserRepository userRepository;
    private SaasPlanRepository saasPlanRepository;
    private TenantSubscriptionRepository subscriptionRepository;
    private TenantChargeRepository chargeRepository;
    private SignupService signupService;

    private static final String VALID_CNPJ = "11222333000181";

    @BeforeEach
    void setup() {
        tenantRepository = mock(TenantRepository.class);
        userRepository = mock(UserRepository.class);
        saasPlanRepository = mock(SaasPlanRepository.class);
        subscriptionRepository = mock(TenantSubscriptionRepository.class);
        chargeRepository = mock(TenantChargeRepository.class);
        JwtUtil jwtUtil = new JwtUtil("super-secret-key-with-at-least-32-chars!!", 3600L);

        signupService = new SignupService(
            tenantRepository, userRepository, saasPlanRepository,
            subscriptionRepository, chargeRepository,
            new BCryptPasswordEncoder(), jwtUtil
        );

        when(tenantRepository.findBySlug(any())).thenReturn(Optional.empty());
        when(tenantRepository.findByCnpj(any())).thenReturn(Optional.empty());
        when(tenantRepository.save(any())).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            setId(t, UUID.randomUUID());
            return t;
        });
        when(userRepository.save(any())).thenAnswer(inv -> {
            var u = inv.getArgument(0, com.github.mwacha.wachafit.user.User.class);
            setId(u, UUID.randomUUID());
            return u;
        });
        when(subscriptionRepository.save(any())).thenAnswer(inv -> {
            TenantSubscription s = inv.getArgument(0);
            setId(s, UUID.randomUUID());
            return s;
        });

        SaasPlan plan = new SaasPlan();
        plan.setPrice(new BigDecimal("299.90"));
        plan.setActive(true);
        setId(plan, UUID.fromString("00000000-0000-0000-0000-000000000102"));
        when(saasPlanRepository.findById(plan.getId())).thenReturn(Optional.of(plan));
    }

    private SignupRequest buildRequest() {
        return new SignupRequest(
            new SignupRequest.AdminData("Maria Admin", "maria@academia.com", "senha1234"),
            new SignupRequest.CompanyData("Academia Fitness Ltda", VALID_CNPJ, "11999998888", "academia-fitness"),
            new SignupRequest.PlanData(UUID.fromString("00000000-0000-0000-0000-000000000102"), PaymentMethod.PIX)
        );
    }

    @Test
    void signup_createsTenantAdminAndReturnsToken() {
        LoginResponse resp = signupService.signup(buildRequest());

        assertThat(resp.token()).isNotBlank();
        assertThat(resp.role()).isEqualTo("ADMIN");
        verify(tenantRepository).save(any());
        verify(userRepository).save(any());
        verify(subscriptionRepository).save(any());
        verify(chargeRepository).saveAndFlush(any());
    }

    @Test
    void signup_throwsBusinessException_whenSlugTaken() {
        Tenant existing = new Tenant();
        when(tenantRepository.findBySlug("academia-fitness")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> signupService.signup(buildRequest()))
            .isInstanceOf(BusinessException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void signup_throwsBusinessException_whenCnpjInvalid() {
        SignupRequest req = new SignupRequest(
            new SignupRequest.AdminData("Maria Admin", "maria@academia.com", "senha1234"),
            new SignupRequest.CompanyData("Academia Fitness Ltda", "11222333000199", "11999998888", "academia-fitness"),
            new SignupRequest.PlanData(UUID.fromString("00000000-0000-0000-0000-000000000102"), PaymentMethod.PIX)
        );

        assertThatThrownBy(() -> signupService.signup(req))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void signup_doesNotRollBack_whenChargeCreationFails() {
        when(chargeRepository.saveAndFlush(any())).thenThrow(new RuntimeException("falha simulada"));

        LoginResponse resp = signupService.signup(buildRequest());

        assertThat(resp.token()).isNotBlank();
        verify(tenantRepository).save(any());
        verify(subscriptionRepository).save(any());
    }

    private static void setId(Object entity, UUID id) {
        try {
            var f = entity.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
