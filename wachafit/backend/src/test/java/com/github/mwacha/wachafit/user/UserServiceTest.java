package com.github.mwacha.wachafit.user;

import com.github.mwacha.wachafit.account.Account;
import com.github.mwacha.wachafit.account.AccountRepository;
import com.github.mwacha.wachafit.notification.EmailService;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.tenant.Tenant;
import com.github.mwacha.wachafit.tenant.TenantContext;
import com.github.mwacha.wachafit.tenant.TenantRepository;
import com.github.mwacha.wachafit.user.dto.CreateUserRequest;
import com.github.mwacha.wachafit.user.dto.UpdateUserRequest;
import com.github.mwacha.wachafit.user.dto.UserResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock AccountRepository accountRepository;
    @Mock TenantRepository tenantRepository;
    @Mock EmailService emailService;
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private UserService userService;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, accountRepository, tenantRepository,
            passwordEncoder, emailService);
        TenantContext.set(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Tenant buildTenant() throws Exception {
        Tenant t = new Tenant();
        setId(t, tenantId);
        return t;
    }

    @Test
    void createUser_createsNewAccount_whenEmailNotFound() throws Exception {
        when(accountRepository.findByEmail("trainer@example.com")).thenReturn(Optional.empty());
        when(accountRepository.save(any())).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            setId(a, UUID.randomUUID());
            return a;
        });
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(buildTenant()));
        when(userRepository.existsByAccountIdAndTenantId(any(), eq(tenantId))).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            setId(u, UUID.randomUUID());
            return u;
        });

        UserResponse result = userService.createUser(
            new CreateUserRequest("João Trainer", "trainer@example.com", "senha123", Role.TRAINER));

        assertThat(result.role()).isEqualTo("TRAINER");
        verify(accountRepository).save(argThat(a -> a.getEmail().equals("trainer@example.com")));
        verify(userRepository).save(argThat(u -> u.getRole() == Role.TRAINER));
        verify(emailService).sendHtml(eq("trainer@example.com"), contains("Bem-vindo"), eq("email/welcome"), anyMap());
    }

    @Test
    void createUser_linksToExistingAccount_ignoringSubmittedPassword() throws Exception {
        Account existing = new Account();
        existing.setName("Pessoa Já Cadastrada");
        existing.setEmail("ja-existe@example.com");
        existing.setPasswordHash("hash-antigo-nao-deve-mudar");
        setId(existing, UUID.randomUUID());

        when(accountRepository.findByEmail("ja-existe@example.com")).thenReturn(Optional.of(existing));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(buildTenant()));
        when(userRepository.existsByAccountIdAndTenantId(existing.getId(), tenantId)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            setId(u, UUID.randomUUID());
            return u;
        });

        UserResponse result = userService.createUser(
            new CreateUserRequest("Nome Ignorado", "ja-existe@example.com", "senhaQualquerDigitada", Role.TRAINER));

        verify(accountRepository, never()).save(any());
        assertThat(existing.getPasswordHash()).isEqualTo("hash-antigo-nao-deve-mudar");
        verify(userRepository).save(argThat(u -> u.getAccount() == existing));
    }

    @Test
    void createUser_shouldRejectStudentRole() {
        assertThatThrownBy(() -> userService.createUser(
            new CreateUserRequest("Student", "s@example.com", "senha123", Role.STUDENT)))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("STUDENT");
    }

    @Test
    void createUser_shouldRejectDuplicateMembership() throws Exception {
        Account existing = new Account();
        setId(existing, UUID.randomUUID());
        when(accountRepository.findByEmail("dup@example.com")).thenReturn(Optional.of(existing));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(buildTenant()));
        when(userRepository.existsByAccountIdAndTenantId(existing.getId(), tenantId)).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(
            new CreateUserRequest("Dup", "dup@example.com", "senha123", Role.TRAINER)))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void deactivateUser_shouldSetActiveFalse() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        User user = buildUser(userId, Role.TRAINER, true);
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        userService.deactivateUser(userId, currentUserId);

        verify(userRepository).save(argThat(u -> !u.isActive()));
    }

    @Test
    void deactivateUser_shouldRejectSelfDeactivation() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, Role.ADMIN, true);
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.deactivateUser(userId, userId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("própria conta");
    }

    @Test
    void deactivateUser_shouldRejectStudentRole() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        User user = buildUser(userId, Role.STUDENT, true);
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.deactivateUser(userId, currentUserId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Cannot deactivate a student user");
    }

    // Regressão: User não tem @Filter automático de tenant (proposital, para o login buscar
    // vínculos de uma Account em vários tenants) -- findOrThrow()/listUsers() precisam filtrar
    // por tenant EXPLICITAMENTE. Sem isso, um admin de uma academia conseguia ler/editar/
    // desativar usuários de OUTRA academia só sabendo o UUID.
    @Test
    void deactivateUser_shouldThrowNotFound_whenUserBelongsToDifferentTenant() {
        UUID userId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        // userRepository.findByIdAndTenantId NÃO foi stubado para este userId+tenantId -> retorna
        // Optional.empty() por padrão (Mockito), simulando um usuário que existe mas é de outro tenant.

        assertThatThrownBy(() -> userService.deactivateUser(userId, currentUserId))
            .isInstanceOf(com.github.mwacha.wachafit.shared.exception.NotFoundException.class);
    }

    @Test
    void listUsers_onlyQueriesCurrentTenant() throws Exception {
        User u1 = buildUser(UUID.randomUUID(), Role.TRAINER, true);
        when(userRepository.findByTenantId(tenantId)).thenReturn(java.util.List.of(u1));

        var result = userService.listUsers(null, null);

        assertThat(result).hasSize(1);
        verify(userRepository).findByTenantId(tenantId);
        verify(userRepository, never()).findAll();
    }

    private User buildUser(UUID id, Role role, boolean active) throws Exception {
        Account account = new Account();
        account.setName("Test");
        account.setEmail("test-" + id + "@example.com");
        account.setPasswordHash("hash");
        setId(account, UUID.randomUUID());

        User u = new User();
        u.setAccount(account);
        u.setRole(role);
        u.setActive(active);
        setId(u, id);
        return u;
    }

    private static void setId(Object entity, UUID id) throws Exception {
        Field f = entity.getClass().getDeclaredField("id");
        f.setAccessible(true);
        f.set(entity, id);
    }
}
