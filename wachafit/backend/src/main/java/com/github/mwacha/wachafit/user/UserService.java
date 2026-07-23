package com.github.mwacha.wachafit.user;

import com.github.mwacha.wachafit.notification.EmailService;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import com.github.mwacha.wachafit.tenant.Tenant;
import com.github.mwacha.wachafit.tenant.TenantContext;
import com.github.mwacha.wachafit.tenant.TenantRepository;
import com.github.mwacha.wachafit.user.dto.CreateUserRequest;
import com.github.mwacha.wachafit.user.dto.UpdateUserRequest;
import com.github.mwacha.wachafit.user.dto.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserService(UserRepository userRepository, TenantRepository tenantRepository,
                       PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers(String role, Boolean active) {
        return userRepository.findAll().stream()   // Hibernate filter filtrará por tenant (Task 4)
            .filter(u -> role == null || u.getRole().name().equals(role))
            .filter(u -> active == null || u.isActive() == active)
            .map(this::toResponse)
            .toList();
    }

    public UserResponse createUser(CreateUserRequest req) {
        if (req.role() == Role.STUDENT) {
            throw new BusinessException("Não é permitido criar usuário com role STUDENT por este endpoint");
        }
        UUID tenantId = TenantContext.get();
        if (userRepository.existsByEmailAndTenantId(req.email(), tenantId)) {
            throw new BusinessException("E-mail já cadastrado");
        }
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new NotFoundException("Tenant não encontrado"));
        User user = new User();
        user.setName(req.name());
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(req.role());
        user.setTenant(tenant);
        User saved = userRepository.save(user);
        emailService.sendHtml(
            saved.getEmail(),
            "Bem-vindo ao WachaFit!",
            "email/welcome",
            Map.of("name", saved.getName())
        );
        return toResponse(saved);
    }

    public UserResponse updateUser(UUID id, UpdateUserRequest req) {
        User user = findOrThrow(id);
        user.setName(req.name());
        user.setRole(req.role());
        return toResponse(userRepository.save(user));
    }

    public void deactivateUser(UUID id, UUID currentUserId) {
        User user = findOrThrow(id);
        if (user.getRole() == Role.STUDENT) {
            throw new BusinessException("Cannot deactivate a student user");
        }
        if (id.equals(currentUserId)) {
            throw new BusinessException("Não é possível desativar a própria conta");
        }
        user.setActive(false);
        userRepository.save(user);
    }

    public void activateUser(UUID id) {
        User user = findOrThrow(id);
        user.setActive(true);
        userRepository.save(user);
    }

    private User findOrThrow(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + id));
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(
            u.getId().toString(),
            u.getName(),
            u.getEmail(),
            u.getRole().name(),
            u.isActive(),
            u.getCreatedAt() != null ? u.getCreatedAt().toString() : null
        );
    }
}
