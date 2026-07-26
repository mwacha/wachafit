package com.github.mwacha.wachafit.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mwacha.wachafit.account.Account;
import com.github.mwacha.wachafit.account.AccountRepository;
import com.github.mwacha.wachafit.auth.dto.LoginRequest;
import com.github.mwacha.wachafit.auth.dto.RegisterRequest;
import com.github.mwacha.wachafit.auth.dto.SelectTenantRequest;
import com.github.mwacha.wachafit.auth.dto.SwitchTenantRequest;
import com.github.mwacha.wachafit.tenant.Tenant;
import com.github.mwacha.wachafit.tenant.TenantRepository;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        r.add("jwt.secret", () -> "integration-test-secret-32-chars-ok");
        r.add("jwt.expiration", () -> "3600");
        r.add("app.frontend-url", () -> "http://localhost:5173");
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired TenantRepository tenantRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private Tenant createTenant(String slugSuffix) {
        Tenant t = new Tenant();
        t.setName("Academia " + slugSuffix);
        t.setSlug("isolation-test-" + slugSuffix + "-" + UUID.randomUUID());
        // cnpj deliberadamente não setado (fica NULL) -- é UNIQUE (V33), e cada teste cria
        // vários tenants; NULL é permitido repetir em uma coluna UNIQUE no Postgres.
        t.setActive(true);
        return tenantRepository.save(t);
    }

    private User createMembership(Account account, Tenant tenant, Role role) {
        User u = new User();
        u.setAccount(account);
        u.setRole(role);
        u.setTenant(tenant);
        u.setActive(true);
        return userRepository.save(u);
    }

    private Account createAccount(String email) {
        Account a = new Account();
        a.setName(email);
        a.setEmail(email);
        a.setPasswordHash(passwordEncoder.encode("password123"));
        return accountRepository.save(a);
    }

    @Test
    void register_shouldReturn200WithToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RegisterRequest("Alice", "alice@test.com", "password123", "personal-studio"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.role").value("STUDENT"))
            .andExpect(jsonPath("$.userId").isNotEmpty());
    }

    @Test
    void login_afterRegister_shouldReturnToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RegisterRequest("Bob", "bob@test.com", "password123", "personal-studio"))))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new LoginRequest("bob@test.com", "password123"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_withWrongPassword_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RegisterRequest("Carol", "carol@test.com", "password123", "personal-studio"))))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new LoginRequest("carol@test.com", "wrongpassword"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Credenciais inválidas"));
    }

    @Test
    void switchTenant_rejectsAccountWithoutMembershipInTargetTenant() throws Exception {
        Tenant tenantA = createTenant("a");
        Tenant tenantB = createTenant("b");

        // Account X só tem vínculo com tenantB
        Account accountX = createAccount("x-isolation@test.com");
        createMembership(accountX, tenantB, Role.ADMIN);

        var loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new LoginRequest("x-isolation@test.com", "password123"))))
            .andExpect(status().isOk())
            .andReturn();
        String tokenX = objectMapper.readTree(loginResult.getResponse().getContentAsString())
            .get("token").asText();

        // X tenta trocar para tenantA, onde não tem nenhum vínculo -> deve ser rejeitado
        mockMvc.perform(post("/api/auth/switch-tenant")
                .header("Authorization", "Bearer " + tokenX)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new SwitchTenantRequest(tenantA.getId().toString()))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void selectTenant_rejectsTenantIdNotBelongingToAccount() throws Exception {
        Tenant tenantA = createTenant("a2");
        Tenant tenantB = createTenant("b2");
        Tenant tenantC = createTenant("c2");

        // Account Y tem 2 vínculos (tenantA e tenantB), então o login retorna selectTenantToken
        Account accountY = createAccount("y-isolation@test.com");
        createMembership(accountY, tenantA, Role.TRAINER);
        createMembership(accountY, tenantB, Role.ADMIN);

        var loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new LoginRequest("y-isolation@test.com", "password123"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.selectTenantToken").isNotEmpty())
            .andExpect(jsonPath("$.memberships.length()").value(2))
            .andReturn();
        String selectTenantToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
            .get("selectTenantToken").asText();

        // Y tenta selecionar tenantC, onde não tem nenhum vínculo -> deve ser rejeitado
        mockMvc.perform(post("/api/auth/select-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new SelectTenantRequest(selectTenantToken, tenantC.getId().toString()))))
            .andExpect(status().isUnauthorized());
    }
}
