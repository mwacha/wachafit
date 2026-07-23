package com.github.mwacha.wachafit.auth;

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
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final TenantRepository tenantRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final String frontendUrl;

    public AuthService(
        UserRepository userRepository,
        PasswordResetTokenRepository tokenRepository,
        TenantRepository tenantRepository,
        JwtUtil jwtUtil,
        PasswordEncoder passwordEncoder,
        EmailService emailService,
        @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.userRepository = userRepository;
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
        if (userRepository.existsByEmailAndTenantId(request.email(), tenant.getId())) {
            throw new BusinessException("E-mail já cadastrado nesta academia");
        }
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.STUDENT);
        user.setTenant(tenant);
        User saved = userRepository.save(user);
        emailService.sendHtml(
            saved.getEmail(),
            "Bem-vindo ao WachaFit!",
            "email/welcome",
            Map.of("name", saved.getName())
        );
        String token = jwtUtil.generateToken(saved);
        return new LoginResponse(token, saved.getRole().name(), saved.getId().toString(),
                                 tenant.getId().toString());
    }

    public LoginResponse login(LoginRequest request) {
        Tenant tenant = tenantRepository.findBySlug(request.tenantSlug())
            .filter(Tenant::isActive)
            .orElseThrow(() -> new UnauthorizedException("Academia não encontrada"));
        User user = userRepository.findByEmailAndTenantId(request.email(), tenant.getId())
            .orElseThrow(() -> new UnauthorizedException("Credenciais inválidas"));
        if (!user.isActive()) {
            throw new UnauthorizedException("Usuário inativo");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Credenciais inválidas");
        }
        String token = jwtUtil.generateToken(user);
        return new LoginResponse(token, user.getRole().name(), user.getId().toString(),
                                 tenant.getId().toString());
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUser(user);
            resetToken.setToken(UUID.randomUUID().toString());
            resetToken.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
            tokenRepository.save(resetToken);
            String resetLink = frontendUrl + "/reset-password?token=" + resetToken.getToken();
            emailService.sendHtml(
                user.getEmail(),
                "Redefinição de senha — WachaFit",
                "email/password-reset",
                Map.of("name", user.getName(), "resetLink", resetLink)
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
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        resetToken.setUsed(true);
        // Both entities are managed within this transaction; Hibernate dirty-checks flush at commit.
    }
}
