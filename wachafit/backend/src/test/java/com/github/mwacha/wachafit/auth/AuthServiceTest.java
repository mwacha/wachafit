package com.github.mwacha.wachafit.auth;

import com.github.mwacha.wachafit.account.Account;
import com.github.mwacha.wachafit.account.AccountRepository;
import com.github.mwacha.wachafit.auth.dto.*;
import com.github.mwacha.wachafit.notification.EmailService;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
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

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepo;
    private AccountRepository accountRepo;
    private PasswordResetTokenRepository tokenRepo;
    private TenantRepository tenantRepo;
    private JwtUtil jwtUtil;
    private AuthService authService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setup() {
        userRepo = mock(UserRepository.class);
        accountRepo = mock(AccountRepository.class);
        tokenRepo = mock(PasswordResetTokenRepository.class);
        tenantRepo = mock(TenantRepository.class);
        jwtUtil = new JwtUtil("super-secret-key-with-at-least-32-chars!!", 3600L);
        authService = new AuthService(
            userRepo, accountRepo, tokenRepo, tenantRepo, jwtUtil, encoder,
            mock(EmailService.class), "http://localhost:5173"
        );
    }

    private Account buildAccount(String email, String rawPassword) throws Exception {
        Account a = new Account();
        a.setEmail(email);
        a.setPasswordHash(encoder.encode(rawPassword));
        a.setActive(true);
        setId(a, UUID.randomUUID());
        return a;
    }

    private User buildMembership(Account account, Tenant tenant, Role role) throws Exception {
        User u = new User();
        u.setAccount(account);
        u.setTenant(tenant);
        u.setRole(role);
        u.setActive(true);
        setId(u, UUID.randomUUID());
        return u;
    }

    private Tenant buildTenant(String name, String slug) throws Exception {
        Tenant t = new Tenant();
        t.setName(name);
        t.setSlug(slug);
        setId(t, UUID.randomUUID());
        return t;
    }

    @Test
    void login_returnsFullToken_whenSingleMembership() throws Exception {
        Account account = buildAccount("admin@teste.com", "senha123");
        Tenant tenant = buildTenant("Academia Teste", "academia-teste");
        User membership = buildMembership(account, tenant, Role.ADMIN);

        when(accountRepo.findByEmail("admin@teste.com")).thenReturn(Optional.of(account));
        when(userRepo.findByAccountIdAndActiveTrue(account.getId())).thenReturn(List.of(membership));

        LoginResponse resp = authService.login(new LoginRequest("admin@teste.com", "senha123"));

        assertThat(resp.token()).isNotBlank();
        assertThat(resp.role()).isEqualTo("ADMIN");
        assertThat(resp.tenantId()).isEqualTo(tenant.getId().toString());
        assertThat(resp.selectTenantToken()).isNull();
        assertThat(resp.memberships()).isNull();
    }

    @Test
    void login_returnsSelectTenantToken_whenMultipleMemberships() throws Exception {
        Account account = buildAccount("multi@teste.com", "senha123");
        Tenant tenantA = buildTenant("Academia A", "academia-a");
        Tenant tenantB = buildTenant("Academia B", "academia-b");
        User membershipA = buildMembership(account, tenantA, Role.TRAINER);
        User membershipB = buildMembership(account, tenantB, Role.ADMIN);

        when(accountRepo.findByEmail("multi@teste.com")).thenReturn(Optional.of(account));
        when(userRepo.findByAccountIdAndActiveTrue(account.getId()))
            .thenReturn(List.of(membershipA, membershipB));

        LoginResponse resp = authService.login(new LoginRequest("multi@teste.com", "senha123"));

        assertThat(resp.token()).isNull();
        assertThat(resp.selectTenantToken()).isNotBlank();
        assertThat(resp.memberships()).hasSize(2);
        assertThat(resp.memberships().stream().map(TenantMembershipSummary::tenantSlug))
            .containsExactlyInAnyOrder("academia-a", "academia-b");
    }

    @Test
    void login_throwsUnauthorized_whenPasswordWrong() throws Exception {
        Account account = buildAccount("admin@teste.com", "senha123");
        when(accountRepo.findByEmail("admin@teste.com")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin@teste.com", "errada")))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_throwsUnauthorized_whenAccountNotFound() {
        when(accountRepo.findByEmail("ghost@teste.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@teste.com", "pass")))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_throwsUnauthorized_whenNoActiveMemberships() throws Exception {
        Account account = buildAccount("semvinculo@teste.com", "senha123");
        when(accountRepo.findByEmail("semvinculo@teste.com")).thenReturn(Optional.of(account));
        when(userRepo.findByAccountIdAndActiveTrue(account.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> authService.login(new LoginRequest("semvinculo@teste.com", "senha123")))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void selectTenant_issuesFullToken_whenTenantBelongsToAccount() throws Exception {
        Account account = buildAccount("multi@teste.com", "senha123");
        Tenant tenant = buildTenant("Academia A", "academia-a");
        User membership = buildMembership(account, tenant, Role.TRAINER);
        String selectToken = jwtUtil.generateSelectTenantToken(account);

        when(userRepo.findByAccountIdAndTenantId(account.getId(), tenant.getId()))
            .thenReturn(Optional.of(membership));

        LoginResponse resp = authService.selectTenant(
            new SelectTenantRequest(selectToken, tenant.getId().toString()));

        assertThat(resp.token()).isNotBlank();
        assertThat(resp.role()).isEqualTo("TRAINER");
    }

    @Test
    void selectTenant_throwsUnauthorized_whenTokenIsNotSelectTenantPurpose() throws Exception {
        Account account = buildAccount("x@teste.com", "senha123");
        Tenant tenant = buildTenant("Academia A", "academia-a");
        User membership = buildMembership(account, tenant, Role.TRAINER);
        String normalToken = jwtUtil.generateToken(membership);

        assertThatThrownBy(() -> authService.selectTenant(
            new SelectTenantRequest(normalToken, tenant.getId().toString())))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void switchTenant_issuesTokenForOtherMembership() throws Exception {
        Account account = buildAccount("multi@teste.com", "senha123");
        Tenant currentTenant = buildTenant("Academia A", "academia-a");
        Tenant otherTenant = buildTenant("Academia B", "academia-b");
        User currentMembership = buildMembership(account, currentTenant, Role.TRAINER);
        User otherMembership = buildMembership(account, otherTenant, Role.ADMIN);

        when(userRepo.findByAccountIdAndTenantId(account.getId(), otherTenant.getId()))
            .thenReturn(Optional.of(otherMembership));

        LoginResponse resp = authService.switchTenant(
            new SwitchTenantRequest(otherTenant.getId().toString()), currentMembership);

        assertThat(resp.role()).isEqualTo("ADMIN");
        assertThat(resp.tenantId()).isEqualTo(otherTenant.getId().toString());
    }

    @Test
    void myTenants_listsAllActiveMemberships() throws Exception {
        Account account = buildAccount("multi@teste.com", "senha123");
        Tenant tenantA = buildTenant("Academia A", "academia-a");
        User membershipA = buildMembership(account, tenantA, Role.TRAINER);

        when(userRepo.findByAccountIdAndActiveTrue(account.getId())).thenReturn(List.of(membershipA));

        List<TenantMembershipSummary> result = authService.myTenants(membershipA);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).tenantSlug()).isEqualTo("academia-a");
    }

    private static void setId(Object entity, UUID id) throws Exception {
        Field f = entity.getClass().getDeclaredField("id");
        f.setAccessible(true);
        f.set(entity, id);
    }
}
