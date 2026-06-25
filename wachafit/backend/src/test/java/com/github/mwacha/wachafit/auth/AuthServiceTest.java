package com.github.mwacha.wachafit.auth;

import com.github.mwacha.wachafit.auth.dto.LoginRequest;
import com.github.mwacha.wachafit.auth.dto.LoginResponse;
import com.github.mwacha.wachafit.auth.dto.RegisterRequest;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.shared.security.JwtUtil;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordResetTokenRepository tokenRepository;
    @Mock JwtUtil jwtUtil;
    @Mock JavaMailSender mailSender;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, tokenRepository, jwtUtil, passwordEncoder, mailSender, "http://localhost:5173");
    }

    @Test
    void register_shouldCreateStudentUser_andReturnToken() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            try {
                var f = User.class.getDeclaredField("id");
                f.setAccessible(true);
                f.set(u, UUID.randomUUID());
            } catch (Exception e) { throw new RuntimeException(e); }
            return u;
        });
        when(jwtUtil.generateToken(any(User.class))).thenReturn("token123");

        LoginResponse response = authService.register(new RegisterRequest("Alice", "new@example.com", "password1"));

        assertThat(response.token()).isEqualTo("token123");
        assertThat(response.role()).isEqualTo("STUDENT");
        verify(userRepository).save(argThat(u -> u.getRole() == Role.STUDENT && u.isActive()));
    }

    @Test
    void register_shouldThrow_whenEmailAlreadyExists() {
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("Bob", "dup@example.com", "password1")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("já cadastrado");
    }

    @Test
    void login_shouldReturnToken_whenCredentialsValid() {
        User user = buildUser("user@example.com", "secret", Role.STUDENT, true);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(user)).thenReturn("jwt-token");

        LoginResponse response = authService.login(new LoginRequest("user@example.com", "secret"));

        assertThat(response.token()).isEqualTo("jwt-token");
    }

    @Test
    void login_shouldThrow_whenUserNotFound() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@example.com", "pass")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("inválidas");
    }

    @Test
    void login_shouldThrow_whenUserInactive() {
        User user = buildUser("inactive@example.com", "pass", Role.STUDENT, false);
        when(userRepository.findByEmail("inactive@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("inactive@example.com", "pass")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("inativo");
    }

    @Test
    void login_shouldThrow_whenPasswordWrong() {
        User user = buildUser("user@example.com", "correct", Role.STUDENT, true);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", "wrong")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("inválidas");
    }

    private User buildUser(String email, String rawPassword, Role role, boolean active) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setRole(role);
        u.setActive(active);
        u.setName("Test");
        try {
            var f = User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(u, UUID.randomUUID());
        } catch (Exception e) { throw new RuntimeException(e); }
        return u;
    }
}
