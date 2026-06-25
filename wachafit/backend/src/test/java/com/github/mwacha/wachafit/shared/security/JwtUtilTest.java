package com.github.mwacha.wachafit.shared.security;

import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("test-secret-must-be-at-least-32-chars", 3600L);
    }

    @Test
    void generateToken_shouldProduceValidToken() {
        User testUser = buildTestUser(UUID.randomUUID(), "test@example.com", Role.STUDENT);
        String token = jwtUtil.generateToken(testUser);

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    void extractUserId_shouldReturnCorrectUUID() {
        UUID id = UUID.randomUUID();
        User testUser = buildTestUser(id, "test@example.com", Role.ADMIN);
        String token = jwtUtil.generateToken(testUser);

        assertThat(jwtUtil.extractUserId(token)).isEqualTo(id);
    }

    @Test
    void extractRole_shouldReturnCorrectRole() {
        User testUser = buildTestUser(UUID.randomUUID(), "trainer@example.com", Role.TRAINER);
        String token = jwtUtil.generateToken(testUser);

        assertThat(jwtUtil.extractRole(token)).isEqualTo("TRAINER");
    }

    @Test
    void isTokenValid_shouldReturnFalseForTamperedToken() {
        User testUser = buildTestUser(UUID.randomUUID(), "test@example.com", Role.STUDENT);
        String token = jwtUtil.generateToken(testUser);
        String tampered = token.substring(0, token.length() - 4) + "XXXX";

        assertThat(jwtUtil.isTokenValid(tampered)).isFalse();
    }

    private User buildTestUser(UUID id, String email, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash("hash");
        u.setRole(role);
        u.setName("Test");
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
