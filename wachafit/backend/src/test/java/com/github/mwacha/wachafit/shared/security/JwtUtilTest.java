package com.github.mwacha.wachafit.shared.security;

import com.github.mwacha.wachafit.account.Account;
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
    private UUID accountId;

    @BeforeEach
    void setup() throws Exception {
        jwtUtil = new JwtUtil("super-secret-key-with-at-least-32-chars!!", 3600L);
        tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        accountId = UUID.fromString("00000000-0000-0000-0000-000000000099");
    }

    private User buildUser() throws Exception {
        Tenant tenant = new Tenant();
        setId(tenant, tenantId);

        Account account = new Account();
        account.setEmail("teste@email.com");
        account.setPasswordHash("hash");
        setId(account, accountId);

        User user = new User();
        setId(user, UUID.randomUUID());
        user.setRole(Role.ADMIN);
        user.setTenant(tenant);
        user.setAccount(account);
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

    @Test
    void tokenIsValidAndExtractsAccountId() throws Exception {
        User user = buildUser();
        String token = jwtUtil.generateToken(user);

        assertThat(jwtUtil.isTokenValid(token)).isTrue();
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(user.getId());
        assertThat(jwtUtil.extractTenantId(token)).isEqualTo(tenantId);
        assertThat(jwtUtil.extractAccountId(token)).isEqualTo(accountId);
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void selectTenantToken_carriesAccountIdAsSubject_andHasNoTenantClaims() throws Exception {
        Account account = new Account();
        setId(account, accountId);

        String token = jwtUtil.generateSelectTenantToken(account);

        assertThat(jwtUtil.isSelectTenantToken(token)).isTrue();
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(accountId);
        assertThat(jwtUtil.extractTenantId(token)).isNull();
    }

    @Test
    void isSelectTenantToken_isFalseForNormalToken() throws Exception {
        User user = buildUser();
        String token = jwtUtil.generateToken(user);
        assertThat(jwtUtil.isSelectTenantToken(token)).isFalse();
    }

    private static void setId(Object entity, UUID id) throws Exception {
        Field f = entity.getClass().getDeclaredField("id");
        f.setAccessible(true);
        f.set(entity, id);
    }
}
