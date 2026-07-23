package com.github.mwacha.wachafit.auth;

import com.github.mwacha.wachafit.auth.dto.LoginRequest;
import com.github.mwacha.wachafit.auth.dto.LoginResponse;
import com.github.mwacha.wachafit.notification.EmailService;
import com.github.mwacha.wachafit.shared.exception.UnauthorizedException;
import com.github.mwacha.wachafit.shared.security.JwtUtil;
import com.github.mwacha.wachafit.tenant.Tenant;
import com.github.mwacha.wachafit.tenant.TenantRepository;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepo;
    private TenantRepository tenantRepo;
    private JwtUtil jwtUtil;
    private AuthService authService;

    @BeforeEach
    void setup() {
        userRepo = mock(UserRepository.class);
        tenantRepo = mock(TenantRepository.class);
        jwtUtil = new JwtUtil("super-secret-key-with-at-least-32-chars!!", 3600L);
        authService = new AuthService(
            userRepo,
            mock(PasswordResetTokenRepository.class),
            tenantRepo,
            jwtUtil,
            new BCryptPasswordEncoder(),
            mock(EmailService.class),
            "http://localhost:5173"
        );
    }

    @Test
    void login_returnsTokenWithTenantId() {
        Tenant tenant = new Tenant();
        tenant.setName("Academia Teste");
        tenant.setSlug("academia-teste");
        setId(tenant, UUID.fromString("00000000-0000-0000-0000-000000000099"));

        User user = new User();
        user.setEmail("admin@test.com");
        user.setPasswordHash(new BCryptPasswordEncoder().encode("senha123"));
        user.setRole(Role.ADMIN);
        user.setTenant(tenant);
        setId(user, UUID.randomUUID());

        when(tenantRepo.findBySlug("academia-teste")).thenReturn(Optional.of(tenant));
        when(userRepo.findByEmailAndTenantId("admin@test.com", tenant.getId()))
            .thenReturn(Optional.of(user));

        LoginResponse resp = authService.login(new LoginRequest("admin@test.com", "senha123", "academia-teste"));

        assertThat(resp.token()).isNotBlank();
        assertThat(resp.role()).isEqualTo("ADMIN");
        assertThat(resp.tenantId()).isEqualTo("00000000-0000-0000-0000-000000000099");
    }

    @Test
    void login_throwsUnauthorized_whenTenantNotFound() {
        when(tenantRepo.findBySlug("inexistente")).thenReturn(Optional.empty());
        assertThatThrownBy(() ->
            authService.login(new LoginRequest("x@y.com", "pass", "inexistente"))
        ).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_throwsUnauthorized_whenUserNotFoundInTenant() {
        Tenant tenant = new Tenant();
        tenant.setSlug("academia-teste");
        setId(tenant, UUID.randomUUID());

        when(tenantRepo.findBySlug("academia-teste")).thenReturn(Optional.of(tenant));
        when(userRepo.findByEmailAndTenantId(anyString(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            authService.login(new LoginRequest("x@y.com", "pass", "academia-teste"))
        ).isInstanceOf(UnauthorizedException.class);
    }

    private static void setId(Object entity, UUID id) {
        try {
            var f = entity.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
