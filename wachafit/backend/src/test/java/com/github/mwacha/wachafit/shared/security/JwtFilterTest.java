package com.github.mwacha.wachafit.shared.security;

import com.github.mwacha.wachafit.tenant.Tenant;
import com.github.mwacha.wachafit.tenant.TenantContext;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtFilterTest {

    private static final String SECRET = "super-secret-key-with-at-least-32-chars!!";

    private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    private JwtUtil jwtUtil;
    private UserDetailsService userDetailsService;
    private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, 3600L);
        userDetailsService = mock(UserDetailsService.class);
        jwtFilter = new JwtFilter(jwtUtil, userDetailsService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void validTokenWithTenantId_authenticatesAndSetsTenantContext() throws Exception {
        UUID tenantId = UUID.randomUUID();
        User user = buildUser(tenantId);
        String token = jwtUtil.generateToken(user);
        when(userDetailsService.loadUserByUsername(user.getId().toString())).thenReturn(user);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        UUID[] tenantSeenDuringChain = new UUID[1];
        doAnswer(inv -> {
            tenantSeenDuringChain[0] = TenantContext.get();
            return null;
        }).when(chain).doFilter(any(), any());

        jwtFilter.doFilter(request, response, chain);

        assertThat(tenantSeenDuringChain[0]).isEqualTo(tenantId);
        assertThat(TenantContext.get()).isNull(); // cleared in finally after chain returns
        verify(chain).doFilter(request, response);
    }

    @Test
    void validTokenWithoutTenantId_doesNotAuthenticate_preMigrationToken() throws Exception {
        // Token assinado com a mesma chave, mas sem a claim "tenantId" -- simula um token
        // emitido antes da migração multi-tenant (Task 2).
        String preMigrationToken = Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .claim("role", "ADMIN")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 3600_000))
            .signWith(key)
            .compact();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + preMigrationToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        jwtFilter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(userDetailsService);
        verify(chain).doFilter(request, response);
    }

    private User buildUser(UUID tenantId) throws Exception {
        Tenant tenant = new Tenant();
        setId(tenant, tenantId);

        User user = new User();
        setId(user, UUID.randomUUID());
        user.setName("Admin Teste");
        user.setEmail("admin@teste.com");
        user.setPasswordHash("hash");
        user.setRole(Role.ADMIN);
        user.setTenant(tenant);
        user.setActive(true);
        return user;
    }

    private static void setId(Object entity, UUID id) throws Exception {
        Field f = entity.getClass().getDeclaredField("id");
        f.setAccessible(true);
        f.set(entity, id);
    }
}
