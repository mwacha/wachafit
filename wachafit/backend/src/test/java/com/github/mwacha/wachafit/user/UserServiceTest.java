package com.github.mwacha.wachafit.user;

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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock TenantRepository tenantRepository;
    @Mock EmailService emailService;
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private UserService userService;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, tenantRepository, passwordEncoder, emailService);
        TenantContext.set(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createUser_shouldCreateTrainer() {
        when(userRepository.existsByEmailAndTenantId("trainer@example.com", tenantId)).thenReturn(false);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(new Tenant()));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            try {
                var f = User.class.getDeclaredField("id");
                f.setAccessible(true);
                f.set(u, UUID.randomUUID());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return u;
        });

        UserResponse result = userService.createUser(
            new CreateUserRequest("João Trainer", "trainer@example.com", "senha123", Role.TRAINER));

        assertThat(result.role()).isEqualTo("TRAINER");
        assertThat(result.active()).isTrue();
        verify(userRepository).save(argThat(u -> u.getRole() == Role.TRAINER));
        verify(emailService).sendHtml(eq("trainer@example.com"), contains("Bem-vindo"), eq("email/welcome"), anyMap());
    }

    @Test
    void createUser_shouldRejectStudentRole() {
        assertThatThrownBy(() -> userService.createUser(
            new CreateUserRequest("Student", "s@example.com", "senha123", Role.STUDENT)))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("STUDENT");
    }

    @Test
    void createUser_shouldRejectDuplicateEmail() {
        when(userRepository.existsByEmailAndTenantId("dup@example.com", tenantId)).thenReturn(true);
        assertThatThrownBy(() -> userService.createUser(
            new CreateUserRequest("Dup", "dup@example.com", "senha123", Role.TRAINER)))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void deactivateUser_shouldSetActiveFalse() {
        UUID userId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID(); // different user
        User user = buildUser(userId, "target@example.com", Role.TRAINER, true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        userService.deactivateUser(userId, currentUserId);

        verify(userRepository).save(argThat(u -> !u.isActive()));
    }

    @Test
    void deactivateUser_shouldRejectSelfDeactivation() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, "admin@example.com", Role.ADMIN, true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.deactivateUser(userId, userId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("própria conta");
    }

    @Test
    void deactivateUser_shouldRejectStudentRole() {
        UUID userId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        User user = buildUser(userId, "student@example.com", Role.STUDENT, true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.deactivateUser(userId, currentUserId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Cannot deactivate a student user");
    }

    private User buildUser(UUID id, String email, Role role, boolean active) {
        User u = new User();
        u.setEmail(email);
        u.setRole(role);
        u.setActive(active);
        u.setName("Test");
        u.setPasswordHash("hash");
        try {
            var f = User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(u, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return u;
    }
}
