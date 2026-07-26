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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final TenantRepository tenantRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final String frontendUrl;

    public AuthService(
        UserRepository userRepository,
        AccountRepository accountRepository,
        PasswordResetTokenRepository tokenRepository,
        TenantRepository tenantRepository,
        JwtUtil jwtUtil,
        PasswordEncoder passwordEncoder,
        EmailService emailService,
        @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.tokenRepository = tokenRepository;
        this.tenantRepository = tenantRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    public LoginResponse register(RegisterRequest request) {
        Tenant tenant = tenantRepository.findBySlug(request.tenantSlug())
            .filter(Tenant::isActive)
            .orElseThrow(() -> new UnauthorizedException("Academia não encontrada"));

        Account account = accountRepository.findByEmail(request.email())
            .orElseGet(() -> createAccount(request.name(), request.email(), request.password()));

        if (userRepository.existsByAccountIdAndTenantId(account.getId(), tenant.getId())) {
            throw new BusinessException("E-mail já cadastrado nesta academia");
        }

        User user = new User();
        user.setAccount(account);
        user.setRole(Role.STUDENT);
        user.setTenant(tenant);
        User saved = userRepository.save(user);
        emailService.sendHtml(
            account.getEmail(),
            "Bem-vindo ao WachaFit!",
            "email/welcome",
            Map.of("name", account.getName())
        );
        return issueFullLogin(saved);
    }

    public LoginResponse login(LoginRequest request) {
        Account account = accountRepository.findByEmail(request.email())
            .filter(Account::isActive)
            .orElseThrow(() -> new UnauthorizedException("Credenciais inválidas"));
        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new UnauthorizedException("Credenciais inválidas");
        }

        List<User> memberships = userRepository.findByAccountIdAndActiveTrue(account.getId());
        if (memberships.isEmpty()) {
            throw new UnauthorizedException("Credenciais inválidas");
        }
        if (memberships.size() == 1) {
            return issueFullLogin(memberships.get(0));
        }

        String selectToken = jwtUtil.generateSelectTenantToken(account);
        List<TenantMembershipSummary> summaries = memberships.stream()
            .map(this::toSummary)
            .toList();
        return new LoginResponse(null, null, null, null, selectToken, summaries);
    }

    public LoginResponse selectTenant(SelectTenantRequest request) {
        if (!jwtUtil.isSelectTenantToken(request.selectTenantToken())) {
            throw new UnauthorizedException("Token inválido");
        }
        UUID accountId = jwtUtil.extractUserId(request.selectTenantToken());
        UUID tenantId = UUID.fromString(request.tenantId());
        User membership = userRepository.findByAccountIdAndTenantId(accountId, tenantId)
            .filter(User::isActive)
            .orElseThrow(() -> new UnauthorizedException("Academia inválida para esta conta"));
        return issueFullLogin(membership);
    }

    public LoginResponse switchTenant(SwitchTenantRequest request, User currentUser) {
        UUID accountId = currentUser.getAccount().getId();
        UUID tenantId = UUID.fromString(request.tenantId());
        User membership = userRepository.findByAccountIdAndTenantId(accountId, tenantId)
            .filter(User::isActive)
            .orElseThrow(() -> new UnauthorizedException("Academia inválida para esta conta"));
        return issueFullLogin(membership);
    }

    @Transactional(readOnly = true)
    public List<TenantMembershipSummary> myTenants(User currentUser) {
        return userRepository.findByAccountIdAndActiveTrue(currentUser.getAccount().getId()).stream()
            .map(this::toSummary)
            .toList();
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        accountRepository.findByEmail(request.email()).ifPresent(account -> {
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setAccount(account);
            resetToken.setToken(UUID.randomUUID().toString());
            resetToken.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
            tokenRepository.save(resetToken);
            String resetLink = frontendUrl + "/reset-password?token=" + resetToken.getToken();
            emailService.sendHtml(
                account.getEmail(),
                "Redefinição de senha — WachaFit",
                "email/password-reset",
                Map.of("name", account.getName(), "resetLink", resetLink)
            );
        });
    }

    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = tokenRepository.findByToken(request.token())
            .orElseThrow(() -> new BusinessException("Token inválido"));
        if (resetToken.isUsed()) {
            throw new BusinessException("Token já utilizado");
        }
        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Token expirado");
        }
        Account account = resetToken.getAccount();
        account.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        resetToken.setUsed(true);
        // Both entities are managed within this transaction; Hibernate dirty-checks flush at commit.
    }

    private Account createAccount(String name, String email, String rawPassword) {
        Account a = new Account();
        a.setName(name);
        a.setEmail(email);
        a.setPasswordHash(passwordEncoder.encode(rawPassword));
        return accountRepository.save(a);
    }

    private LoginResponse issueFullLogin(User membership) {
        String token = jwtUtil.generateToken(membership);
        return new LoginResponse(token, membership.getRole().name(), membership.getId().toString(),
            membership.getTenant().getId().toString());
    }

    private TenantMembershipSummary toSummary(User membership) {
        Tenant tenant = membership.getTenant();
        return new TenantMembershipSummary(
            tenant.getId().toString(), tenant.getName(), tenant.getSlug(), membership.getRole().name());
    }
}
