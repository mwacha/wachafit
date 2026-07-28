package com.github.mwacha.wachafit.user;

import com.github.mwacha.wachafit.account.Account;
import com.github.mwacha.wachafit.account.AccountRepository;
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
    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserService(UserRepository userRepository, AccountRepository accountRepository,
                       TenantRepository tenantRepository, PasswordEncoder passwordEncoder,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers(String role, Boolean active) {
        // User não é TenantAwareEntity (não tem @Filter do Hibernate) -- isso é proposital, pois
        // o fluxo de login precisa buscar vínculos de uma Account em VÁRIOS tenants ao mesmo
        // tempo. Por isso aqui, ao contrário da maioria das entidades do sistema, o tenant
        // precisa ser filtrado explicitamente, nunca via findAll().
        UUID tenantId = TenantContext.get();
        return userRepository.findByTenantId(tenantId).stream()
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
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new NotFoundException("Tenant não encontrado"));

        // Se o e-mail já tiver conta, ignora a senha digitada e só cria o vínculo — a pessoa
        // continua usando a senha que já tinha em outra academia.
        Account account = accountRepository.findByEmail(req.email())
            .orElseGet(() -> {
                Account a = new Account();
                a.setName(req.name());
                a.setEmail(req.email());
                a.setPasswordHash(passwordEncoder.encode(req.password()));
                return accountRepository.save(a);
            });

        if (userRepository.existsByAccountIdAndTenantId(account.getId(), tenantId)) {
            throw new BusinessException("E-mail já cadastrado");
        }

        User user = new User();
        user.setAccount(account);
        user.setRole(req.role());
        user.setTenant(tenant);
        User saved = userRepository.save(user);
        emailService.sendHtml(
            account.getEmail(),
            "Bem-vindo ao M2W Active Suite!",
            "email/welcome",
            Map.of("name", account.getName())
        );
        return toResponse(saved);
    }

    public UserResponse updateUser(UUID id, UpdateUserRequest req) {
        User user = findOrThrow(id);
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
        // Mesmo motivo do comentário em listUsers(): User não tem @Filter automático de tenant,
        // então findById(id) sozinho deixaria um admin de uma academia editar/desativar/ativar
        // um usuário de OUTRA academia só sabendo (ou adivinhando) o UUID dele.
        return userRepository.findByIdAndTenantId(id, TenantContext.get())
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
