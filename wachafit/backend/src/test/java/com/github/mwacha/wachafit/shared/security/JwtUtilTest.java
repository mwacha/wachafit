package com.github.mwacha.wachafit.shared.security;

import com.github.mwacha.wachafit.tenant.Tenant;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private UUID tenantId;

    @BeforeEach
    void setup() throws Exception {
        jwtUtil = new JwtUtil("super-secret-key-with-at-least-32-chars!!", 3600L);
        tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    }

    private User buildUser() throws Exception {
        Tenant tenant = new Tenant();
        // set id via reflection (UUID generated)
        Field tenantIdField = Tenant.class.getDeclaredField("id");
        tenantIdField.setAccessible(true);
        tenantIdField.set(tenant, tenantId);

        User user = new User();
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, UUID.randomUUID());

        user.setName("Teste");
        user.setEmail("teste@email.com");
        user.setPasswordHash("hash");
        user.setRole(Role.ADMIN);
        user.setTenant(tenant);
        return user;
    }

    @Test
    void tokenIsValidAndExtractsTenantId() throws Exception {
        User user = buildUser();
        String token = jwtUtil.generateToken(user);

        assertThat(jwtUtil.isTokenValid(token)).isTrue();
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(user.getId());
        assertThat(jwtUtil.extractTenantId(token)).isEqualTo(tenantId);
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
    }
}
