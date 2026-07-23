# WachaFit Multi-Tenant Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Converter o WachaFit em uma plataforma SaaS multi-tenant onde cada academia (tenant) tem seus dados isolados por `tenant_id` em todas as tabelas, autenticados via JWT com claim de tenant.

**Architecture:** Row-level tenancy — coluna `tenant_id UUID NOT NULL` em todas as tabelas operacionais; Hibernate `@Filter` ativado automaticamente via `@Aspect` em cada repositório; JWT carrega `tenantId` como claim; login requer `tenantSlug` para resolver o tenant. Um `SUPER_ADMIN` bypassa todos os filtros de tenant para gerenciar academias.

**Tech Stack:** Spring Boot 3.x, Hibernate 6, PostgreSQL 14+, Flyway, jjwt-api (já em uso), Vue 3 + Pinia (frontend).

## Global Constraints

- Flyway: próxima migration disponível é `V29`; nunca reusar números existentes
- Hibernate Filter name: `"tenantFilter"`, parâmetro: `"tenantId"` (type `java.util.UUID`)
- ThreadLocal holder: `com.github.mwacha.wachafit.tenant.TenantContext`
- JWT claim name para tenant: `"tenantId"` (string UUID)
- Tenant slug: `VARCHAR(60) UNIQUE`, apenas letras minúsculas, números e hífens
- Todos os dados existentes migram para o tenant da academia atual: `slug = 'personal-studio'`, `name = 'Personal Studio'`, `id = '00000000-0000-0000-0000-000000000001'`
- A unique constraint de `email` na tabela `users` passa de global para `(email, tenant_id)`
- Role `SUPER_ADMIN` é adicionada ao enum `Role.java` e bypassa o guard de tenant em todos os endpoints
- Novos endpoints de gerenciamento de tenants: `POST /api/super/tenants`, `GET /api/super/tenants`, protegidos por `ROLE_SUPER_ADMIN`
- Package base: `com.github.mwacha.wachafit`

---

## Arquitetura de Arquivos

### Novos arquivos
```
backend/src/main/java/com/github/mwacha/wachafit/
  tenant/
    Tenant.java                        — Entity JPA (tabela tenants)
    TenantRepository.java              — JpaRepository<Tenant, UUID>
    TenantContext.java                 — ThreadLocal<UUID> (tenantId corrente)
    TenantAwareEntity.java             — @MappedSuperclass com tenant_id + @Filter + @PrePersist
    TenantFilterAspect.java            — @Aspect que ativa o Hibernate filter antes de cada repositório
    TenantService.java                 — create/list tenants (uso do super admin)
    TenantController.java              — POST/GET /api/super/tenants
    dto/
      CreateTenantRequest.java
      TenantResponse.java

backend/src/main/resources/db/migration/
  V29__create_tenants.sql
  V30__add_tenant_to_users.sql
  V31__add_tenant_to_all_tables.sql
```

### Arquivos modificados
```
backend:
  user/Role.java                       — add SUPER_ADMIN
  user/User.java                       — add @ManyToOne Tenant tenant
  user/UserRepository.java             — add findByEmailAndTenantId, existsByEmailAndTenantId
  user/UserService.java                — all queries scoped to TenantContext.get()
  user/UserDetailsServiceImpl.java     — unchanged (loads by UUID — already globally unique)
  auth/dto/LoginRequest.java           — add tenantSlug field
  auth/dto/LoginResponse.java          — add tenantId field
  auth/dto/RegisterRequest.java        — add tenantSlug field
  auth/AuthService.java                — resolve tenant on login/register; set tenantId in response
  shared/security/JwtUtil.java         — generateToken includes tenantId claim; add extractTenantId()
  shared/security/JwtFilter.java       — extract tenantId from JWT → TenantContext.set()
  config/SecurityConfig.java           — permit /api/super/tenants only SUPER_ADMIN
  report/ReportRepository.java         — all native queries add AND tenant_id = :tenantId

  (all tenant-scoped entities extend TenantAwareEntity):
  groupclass/GroupClass.java
  schedule/Schedule.java
  booking/Booking.java
  assessment/PhysicalAssessment.java
  assessment/AssessmentMeasurement.java
  exercise/Exercise.java
  workout/WorkoutPlan.java
  workout/WorkoutPlanItem.java
  workout/WorkoutLog.java
  workout/PersonalRecord.java
  progress/ProgressPhoto.java
  goal/StudentGoal.java
  profile/StudentProfile.java
  profile/StudentHealth.java
  profile/TrainerProfile.java
  membership/MembershipPlan.java
  membership/MemberSubscription.java
  billing/PaymentCharge.java

frontend:
  src/types/api.ts                     — LoginRequest add tenantSlug; LoginResponse add tenantId
  src/stores/auth.store.ts             — persist tenantId; send on login
  src/views/auth/LoginView.vue         — add campo "Slug da academia"
  src/views/auth/RegisterView.vue      — add campo "Slug da academia"
```

---

## Task 1: Tenant entity + migration V29

**Files:**
- Create: `backend/src/main/resources/db/migration/V29__create_tenants.sql`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/tenant/Tenant.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/tenant/TenantRepository.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/tenant/dto/CreateTenantRequest.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/tenant/dto/TenantResponse.java`
- Test: `backend/src/test/java/com/github/mwacha/wachafit/tenant/TenantRepositoryTest.java`

**Interfaces:**
- Produces: `TenantRepository.findBySlug(String slug): Optional<Tenant>` — usado nos Tasks 3 e 8

- [ ] **Step 1: Escrever o teste que falha**

```java
// backend/src/test/java/com/github/mwacha/wachafit/tenant/TenantRepositoryTest.java
package com.github.mwacha.wachafit.tenant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TenantRepositoryTest {

    @Autowired TenantRepository repo;

    @Test
    void savesAndFindsbySlug() {
        Tenant t = new Tenant();
        t.setName("Academia Teste");
        t.setSlug("academia-teste");
        repo.save(t);

        var found = repo.findBySlug("academia-teste");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Academia Teste");
    }

    @Test
    void findBySlugReturnsEmpty_whenNotFound() {
        assertThat(repo.findBySlug("nao-existe")).isEmpty();
    }
}
```

- [ ] **Step 2: Rodar o teste para confirmar que falha**

```bash
cd backend
mvn test -pl . -Dtest=TenantRepositoryTest -q 2>&1 | tail -20
```
Esperado: FAIL — `Tenant` não existe ainda.

- [ ] **Step 3: Criar a migration V29**

```sql
-- backend/src/main/resources/db/migration/V29__create_tenants.sql
CREATE TABLE tenants (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(120) NOT NULL,
    slug       VARCHAR(60)  NOT NULL UNIQUE
                   CHECK (slug ~ '^[a-z0-9][a-z0-9\-]*[a-z0-9]$'),
    active     BOOLEAN      NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Tenant padrão para dados existentes: a academia Personal Studio,
-- que já usa o sistema hoje, herda todos os dados atuais do banco.
INSERT INTO tenants (id, name, slug)
VALUES ('00000000-0000-0000-0000-000000000001', 'Personal Studio', 'personal-studio');
```

- [ ] **Step 4: Criar a entidade Tenant**

```java
// backend/src/main/java/com/github/mwacha/wachafit/tenant/Tenant.java
package com.github.mwacha.wachafit.tenant;

import jakarta.persistence.*;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenants")
@FilterDef(
    name = "tenantFilter",
    parameters = @ParamDef(name = "tenantId", type = UUID.class)
)
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 60, unique = true)
    private String slug;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    public UUID getId()              { return id; }
    public String getName()          { return name; }
    public void setName(String n)    { this.name = n; }
    public String getSlug()          { return slug; }
    public void setSlug(String s)    { this.slug = s; }
    public boolean isActive()        { return active; }
    public void setActive(boolean a) { this.active = a; }
    public Instant getCreatedAt()    { return createdAt; }
}
```

> **Nota:** `@FilterDef` está na entidade `Tenant` para ser registrado globalmente no Hibernate SessionFactory. O nome `"tenantFilter"` será referenciado por todos os `@Filter` nas entidades scoped.

- [ ] **Step 5: Criar o TenantRepository**

```java
// backend/src/main/java/com/github/mwacha/wachafit/tenant/TenantRepository.java
package com.github.mwacha.wachafit.tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findBySlug(String slug);
}
```

- [ ] **Step 6: Criar os DTOs**

```java
// backend/src/main/java/com/github/mwacha/wachafit/tenant/dto/CreateTenantRequest.java
package com.github.mwacha.wachafit.tenant.dto;

import jakarta.validation.constraints.*;

public record CreateTenantRequest(
    @NotBlank String name,
    @NotBlank @Pattern(regexp = "^[a-z0-9][a-z0-9\\-]*[a-z0-9]$", message = "Slug inválido") String slug
) {}
```

```java
// backend/src/main/java/com/github/mwacha/wachafit/tenant/dto/TenantResponse.java
package com.github.mwacha.wachafit.tenant.dto;

public record TenantResponse(String id, String name, String slug, boolean active, String createdAt) {}
```

- [ ] **Step 7: Rodar o teste novamente**

```bash
mvn test -pl . -Dtest=TenantRepositoryTest -q 2>&1 | tail -10
```
Esperado: PASS (2 testes).

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/resources/db/migration/V29__create_tenants.sql \
        backend/src/main/java/com/github/mwacha/wachafit/tenant/ \
        backend/src/test/java/com/github/mwacha/wachafit/tenant/
git commit -m "feat(tenant): entidade Tenant + migration V29 + FilterDef global"
```

---

## Task 2: TenantContext + JWT com tenantId + JwtFilter

**Files:**
- Create: `backend/src/main/java/com/github/mwacha/wachafit/tenant/TenantContext.java`
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/shared/security/JwtUtil.java`
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/shared/security/JwtFilter.java`
- Test: `backend/src/test/java/com/github/mwacha/wachafit/shared/security/JwtUtilTest.java`

**Interfaces:**
- Consumes: `Tenant` (Task 1) — `user.getTenant().getId()`
- Produces:
  - `TenantContext.set(UUID)`, `TenantContext.get(): UUID`, `TenantContext.clear()`
  - `JwtUtil.generateToken(User): String` — agora inclui claim `"tenantId"`
  - `JwtUtil.extractTenantId(String token): UUID`

- [ ] **Step 1: Escrever o teste que falha**

```java
// backend/src/test/java/com/github/mwacha/wachafit/shared/security/JwtUtilTest.java
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
```

- [ ] **Step 2: Rodar o teste para confirmar que falha**

```bash
mvn test -pl . -Dtest=JwtUtilTest -q 2>&1 | tail -10
```
Esperado: FAIL — `user.getTenant()` não existe ainda no User.

- [ ] **Step 3: Criar TenantContext**

```java
// backend/src/main/java/com/github/mwacha/wachafit/tenant/TenantContext.java
package com.github.mwacha.wachafit.tenant;

import java.util.UUID;

public final class TenantContext {
    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(UUID tenantId) { CURRENT.set(tenantId); }
    public static UUID get()              { return CURRENT.get(); }
    public static void clear()            { CURRENT.remove(); }
}
```

- [ ] **Step 4: Atualizar JwtUtil — adicionar tenantId ao token**

Substitua o método `generateToken` e adicione `extractTenantId`:

```java
// JwtUtil.java — método generateToken (User deve ter getTenant() — será adicionado no Task 3)
public String generateToken(User user) {
    return Jwts.builder()
        .subject(user.getId().toString())
        .claim("role", user.getRole().name())
        .claim("tenantId", user.getTenant().getId().toString())
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + expirationSeconds * 1000))
        .signWith(key)
        .compact();
}

public UUID extractTenantId(String token) {
    String raw = parseClaims(token).get("tenantId", String.class);
    return raw != null ? UUID.fromString(raw) : null;
}
```

- [ ] **Step 5: Atualizar JwtFilter — extrair tenantId e setar TenantContext**

Dentro do bloco `if (token != null && jwtUtil.isTokenValid(token))`, após setar o SecurityContext, adicione:

```java
// Dentro de doFilterInternal, após SecurityContextHolder.getContext().setAuthentication(auth)
UUID tenantId = jwtUtil.extractTenantId(token);
if (tenantId != null) {
    TenantContext.set(tenantId);
}
```

E no `finally` do try-catch (adicionar finally se não existir):

```java
// No doFilterInternal, garantir limpeza após a chain executar:
try {
    chain.doFilter(request, response);
} finally {
    TenantContext.clear();
}
```

O `chain.doFilter` deve ficar dentro desse try-finally, fora do bloco que autentica o token. A estrutura final do método fica:

```java
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                FilterChain chain) throws ServletException, IOException {
    String token = extractToken(request);
    if (token != null && jwtUtil.isTokenValid(token)) {
        try {
            String userId = jwtUtil.extractUserId(token).toString();
            UserDetails user = userDetailsService.loadUserByUsername(userId);
            if (user.isEnabled()) {
                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
                UUID tenantId = jwtUtil.extractTenantId(token);
                if (tenantId != null) TenantContext.set(tenantId);
            }
        } catch (Exception e) {
            log.debug("Could not authenticate from JWT token: {}", e.getMessage());
        }
    }
    try {
        chain.doFilter(request, response);
    } finally {
        TenantContext.clear();
    }
}
```

- [ ] **Step 6: Rodar o teste novamente (parcial — User.getTenant() ainda não existe)**

Este teste só passará completamente após o Task 3. Verifique que compila e avance.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/github/mwacha/wachafit/tenant/TenantContext.java \
        backend/src/main/java/com/github/mwacha/wachafit/shared/security/JwtUtil.java \
        backend/src/main/java/com/github/mwacha/wachafit/shared/security/JwtFilter.java \
        backend/src/test/java/com/github/mwacha/wachafit/shared/security/JwtUtilTest.java
git commit -m "feat(tenant): TenantContext ThreadLocal + JWT claim tenantId + JwtFilter cleanup"
```

---

## Task 3: Migração da tabela users + User entity + AuthService tenant-aware

**Files:**
- Create: `backend/src/main/resources/db/migration/V30__add_tenant_to_users.sql`
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/user/User.java`
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/user/UserRepository.java`
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/user/Role.java`
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/auth/dto/LoginRequest.java`
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/auth/dto/LoginResponse.java`
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/auth/dto/RegisterRequest.java`
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/auth/AuthService.java`
- Test: `backend/src/test/java/com/github/mwacha/wachafit/auth/AuthServiceTest.java`

**Interfaces:**
- Consumes: `TenantRepository.findBySlug(String)` (Task 1), `TenantContext` (Task 2)
- Produces:
  - `User.getTenant(): Tenant`, `User.setTenant(Tenant)`
  - `LoginResponse` com campo `tenantId: String`
  - `AuthService.login()` resolve tenant via slug; emite JWT com tenantId

- [ ] **Step 1: Escrever o teste que falha**

```java
// backend/src/test/java/com/github/mwacha/wachafit/auth/AuthServiceTest.java
package com.github.mwacha.wachafit.auth;

import com.github.mwacha.wachafit.auth.dto.LoginRequest;
import com.github.mwacha.wachafit.auth.dto.LoginResponse;
import com.github.mwacha.wachafit.notification.EmailService;
import com.github.mwacha.wachafit.shared.exception.UnauthorizedException;
import com.github.mwacha.wachafit.shared.security.JwtUtil;
import com.github.mwacha.wachafit.tenant.Tenant;
import com.github.mwacha.wachafit.tenant.TenantRepository;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepo;
    private TenantRepository tenantRepo;
    private JwtUtil jwtUtil;
    private AuthService authService;

    @BeforeEach
    void setup() {
        userRepo = mock(UserRepository.class);
        tenantRepo = mock(TenantRepository.class);
        jwtUtil = new JwtUtil("super-secret-key-with-at-least-32-chars!!", 3600L);
        authService = new AuthService(
            userRepo,
            mock(PasswordResetTokenRepository.class),
            tenantRepo,
            jwtUtil,
            new BCryptPasswordEncoder(),
            mock(EmailService.class),
            "http://localhost:5173"
        );
    }

    @Test
    void login_returnsTokenWithTenantId() {
        Tenant tenant = new Tenant();
        tenant.setName("Academia Teste");
        tenant.setSlug("academia-teste");
        setId(tenant, UUID.fromString("00000000-0000-0000-0000-000000000099"));

        User user = new User();
        user.setEmail("admin@test.com");
        user.setPasswordHash(new BCryptPasswordEncoder().encode("senha123"));
        user.setRole(Role.ADMIN);
        user.setTenant(tenant);
        setId(user, UUID.randomUUID());

        when(tenantRepo.findBySlug("academia-teste")).thenReturn(Optional.of(tenant));
        when(userRepo.findByEmailAndTenantId("admin@test.com", tenant.getId()))
            .thenReturn(Optional.of(user));

        LoginResponse resp = authService.login(new LoginRequest("admin@test.com", "senha123", "academia-teste"));

        assertThat(resp.token()).isNotBlank();
        assertThat(resp.role()).isEqualTo("ADMIN");
        assertThat(resp.tenantId()).isEqualTo("00000000-0000-0000-0000-000000000099");
    }

    @Test
    void login_throwsUnauthorized_whenTenantNotFound() {
        when(tenantRepo.findBySlug("inexistente")).thenReturn(Optional.empty());
        assertThatThrownBy(() ->
            authService.login(new LoginRequest("x@y.com", "pass", "inexistente"))
        ).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_throwsUnauthorized_whenUserNotFoundInTenant() {
        Tenant tenant = new Tenant();
        tenant.setSlug("academia-teste");
        setId(tenant, UUID.randomUUID());

        when(tenantRepo.findBySlug("academia-teste")).thenReturn(Optional.of(tenant));
        when(userRepo.findByEmailAndTenantId(anyString(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            authService.login(new LoginRequest("x@y.com", "pass", "academia-teste"))
        ).isInstanceOf(UnauthorizedException.class);
    }

    private static void setId(Object entity, UUID id) {
        try {
            var f = entity.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
```

- [ ] **Step 2: Rodar para confirmar falha**

```bash
mvn test -pl . -Dtest=AuthServiceTest -q 2>&1 | tail -15
```

- [ ] **Step 3: Criar migration V30**

```sql
-- backend/src/main/resources/db/migration/V30__add_tenant_to_users.sql

-- 1. Adicionar coluna tenant_id (nullable inicialmente para backfill)
ALTER TABLE users ADD COLUMN tenant_id UUID REFERENCES tenants(id);

-- 2. Backfill: todos os usuários existentes vão para o tenant default
UPDATE users SET tenant_id = '00000000-0000-0000-0000-000000000001';

-- 3. Tornar NOT NULL após o backfill
ALTER TABLE users ALTER COLUMN tenant_id SET NOT NULL;

-- 4. Remover a constraint UNIQUE antiga em email
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;

-- 5. Adicionar nova constraint UNIQUE (email, tenant_id)
ALTER TABLE users ADD CONSTRAINT users_email_tenant_unique UNIQUE (email, tenant_id);

-- 6. Índice para busca de login
CREATE INDEX idx_users_email_tenant ON users(email, tenant_id);
```

- [ ] **Step 4: Atualizar Role.java — adicionar SUPER_ADMIN**

```java
// user/Role.java
public enum Role {
    SUPER_ADMIN, ADMIN, MANAGER, RECEPTIONIST, CASHIER, TRAINER, PROFESSOR, STUDENT
}
```

- [ ] **Step 5: Atualizar User.java — adicionar relacionamento com Tenant**

Adicione dentro da classe `User`, após os campos existentes:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "tenant_id", nullable = false)
private Tenant tenant;

// getter e setter:
public Tenant getTenant()              { return tenant; }
public void setTenant(Tenant tenant)   { this.tenant = tenant; }
```

- [ ] **Step 6: Atualizar UserRepository**

```java
// user/UserRepository.java
package com.github.mwacha.wachafit.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailAndTenantId(String email, UUID tenantId);
    boolean existsByEmailAndTenantId(String email, UUID tenantId);

    // Mantidos para uso do SUPER_ADMIN (sem filtro de tenant)
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

- [ ] **Step 7: Atualizar LoginRequest, LoginResponse e RegisterRequest**

```java
// auth/dto/LoginRequest.java
public record LoginRequest(
    @Email @NotBlank String email,
    @NotBlank String password,
    @NotBlank String tenantSlug
) {}
```

```java
// auth/dto/LoginResponse.java
public record LoginResponse(String token, String role, String userId, String tenantId) {}
```

```java
// auth/dto/RegisterRequest.java
public record RegisterRequest(
    @NotBlank(message = "Nome é obrigatório") String name,
    @Email(message = "E-mail inválido") @NotBlank String email,
    @NotBlank @Size(min = 8) String password,
    @NotBlank String tenantSlug
) {}
```

- [ ] **Step 8: Atualizar AuthService**

Adicionar `TenantRepository` ao construtor e alterar `login`, `register`:

```java
// auth/AuthService.java (construtor — adicionar TenantRepository)
private final TenantRepository tenantRepository;

public AuthService(
    UserRepository userRepository,
    PasswordResetTokenRepository tokenRepository,
    TenantRepository tenantRepository,          // NOVO
    JwtUtil jwtUtil,
    PasswordEncoder passwordEncoder,
    EmailService emailService,
    @Value("${app.frontend-url}") String frontendUrl
) {
    this.userRepository = userRepository;
    this.tokenRepository = tokenRepository;
    this.tenantRepository = tenantRepository;
    this.jwtUtil = jwtUtil;
    this.passwordEncoder = passwordEncoder;
    this.emailService = emailService;
    this.frontendUrl = frontendUrl;
}

// Método login atualizado:
public LoginResponse login(LoginRequest request) {
    Tenant tenant = tenantRepository.findBySlug(request.tenantSlug())
        .filter(Tenant::isActive)
        .orElseThrow(() -> new UnauthorizedException("Academia não encontrada"));
    User user = userRepository.findByEmailAndTenantId(request.email(), tenant.getId())
        .orElseThrow(() -> new UnauthorizedException("Credenciais inválidas"));
    if (!user.isActive()) throw new UnauthorizedException("Usuário inativo");
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash()))
        throw new UnauthorizedException("Credenciais inválidas");
    String token = jwtUtil.generateToken(user);
    return new LoginResponse(token, user.getRole().name(), user.getId().toString(),
                             tenant.getId().toString());
}

// Método register atualizado:
public LoginResponse register(RegisterRequest request) {
    Tenant tenant = tenantRepository.findBySlug(request.tenantSlug())
        .filter(Tenant::isActive)
        .orElseThrow(() -> new UnauthorizedException("Academia não encontrada"));
    if (userRepository.existsByEmailAndTenantId(request.email(), tenant.getId())) {
        throw new BusinessException("E-mail já cadastrado nesta academia");
    }
    User user = new User();
    user.setName(request.name());
    user.setEmail(request.email());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setRole(Role.STUDENT);
    user.setTenant(tenant);
    User saved = userRepository.save(user);
    emailService.sendHtml(saved.getEmail(), "Bem-vindo ao WachaFit!",
        "email/welcome", Map.of("name", saved.getName()));
    String token = jwtUtil.generateToken(saved);
    return new LoginResponse(token, saved.getRole().name(), saved.getId().toString(),
                             tenant.getId().toString());
}
```

- [ ] **Step 9: Atualizar UserService — criar usuário dentro do tenant corrente**

No método `createUser`, substituir `existsByEmail` por `existsByEmailAndTenantId` e associar o tenant:

```java
// userService.createUser — dentro do método:
UUID tenantId = TenantContext.get();
if (userRepository.existsByEmailAndTenantId(req.email(), tenantId)) {
    throw new BusinessException("E-mail já cadastrado");
}
Tenant tenant = tenantRepository.findById(tenantId)
    .orElseThrow(() -> new NotFoundException("Tenant não encontrado"));
// ... (cria user como antes)
user.setTenant(tenant);
```

Adicionar `TenantRepository tenantRepository` ao construtor de `UserService`. Adicionar import de `TenantContext` e `TenantRepository`.

Em `listUsers`, remover o `.stream().filter(...)` manual e usar query nativa ou deixar o Hibernate Filter (Task 4) cuidar do escopo. Por ora, adicionar:

```java
public List<UserResponse> listUsers(String role, Boolean active) {
    return userRepository.findAll().stream()   // Hibernate filter filtrará por tenant (Task 4)
        .filter(u -> role == null || u.getRole().name().equals(role))
        .filter(u -> active == null || u.isActive() == active)
        .map(this::toResponse)
        .toList();
}
```

- [ ] **Step 10: Rodar os testes**

```bash
mvn test -pl . -Dtest=AuthServiceTest,JwtUtilTest -q 2>&1 | tail -15
```
Esperado: PASS em todos (5 testes no total).

- [ ] **Step 11: Commit**

```bash
git add backend/src/main/resources/db/migration/V30__add_tenant_to_users.sql \
        backend/src/main/java/com/github/mwacha/wachafit/user/ \
        backend/src/main/java/com/github/mwacha/wachafit/auth/ \
        backend/src/test/java/com/github/mwacha/wachafit/auth/
git commit -m "feat(tenant): tenant_id em users + login com tenantSlug + SUPER_ADMIN role"
```

---

## Task 4: TenantAwareEntity + TenantFilterAspect

**Files:**
- Create: `backend/src/main/java/com/github/mwacha/wachafit/tenant/TenantAwareEntity.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/tenant/TenantFilterAspect.java`
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/config/SecurityConfig.java` — adicionar `aop.auto=true` já está no contexto
- Test: `backend/src/test/java/com/github/mwacha/wachafit/tenant/TenantFilterAspectTest.java`

**Interfaces:**
- Consumes: `TenantContext.get()` (Task 2)
- Produces: `TenantAwareEntity` — `@MappedSuperclass` com `tenantId` UUID + `@Filter` + `@PrePersist` que auto-seta `tenantId` do `TenantContext`

- [ ] **Step 1: Escrever o teste que falha**

```java
// backend/src/test/java/com/github/mwacha/wachafit/tenant/TenantFilterAspectTest.java
package com.github.mwacha.wachafit.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class TenantContextTest {

    @AfterEach
    void cleanup() { TenantContext.clear(); }

    @Test
    void setAndGet_returnsSameTenantId() {
        UUID id = UUID.randomUUID();
        TenantContext.set(id);
        assertThat(TenantContext.get()).isEqualTo(id);
    }

    @Test
    void clear_removesValue() {
        TenantContext.set(UUID.randomUUID());
        TenantContext.clear();
        assertThat(TenantContext.get()).isNull();
    }

    @Test
    void defaultValueIsNull() {
        assertThat(TenantContext.get()).isNull();
    }
}
```

- [ ] **Step 2: Rodar para confirmar que passa (TenantContext já existe)**

```bash
mvn test -pl . -Dtest=TenantContextTest -q 2>&1 | tail -5
```
Esperado: PASS (3 testes).

- [ ] **Step 3: Criar TenantAwareEntity**

```java
// backend/src/main/java/com/github/mwacha/wachafit/tenant/TenantAwareEntity.java
package com.github.mwacha.wachafit.tenant;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

import java.util.UUID;

@MappedSuperclass
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public abstract class TenantAwareEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @PrePersist
    protected void prePersist() {
        if (this.tenantId == null) {
            UUID ctx = TenantContext.get();
            if (ctx == null) throw new IllegalStateException(
                "TenantContext não definido ao persistir " + getClass().getSimpleName());
            this.tenantId = ctx;
        }
    }

    public UUID getTenantId()            { return tenantId; }
    public void setTenantId(UUID id)     { this.tenantId = id; }
}
```

- [ ] **Step 4: Criar TenantFilterAspect**

```java
// backend/src/main/java/com/github/mwacha/wachafit/tenant/TenantFilterAspect.java
package com.github.mwacha.wachafit.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
public class TenantFilterAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Before("within(@org.springframework.stereotype.Repository *)")
    public void enableTenantFilter() {
        UUID tenantId = TenantContext.get();
        if (tenantId != null) {
            Session session = entityManager.unwrap(Session.class);
            if (session.getEnabledFilter("tenantFilter") == null) {
                session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
            }
        }
    }
}
```

> **Nota:** O `@Before` intercepta qualquer método de qualquer classe anotada com `@Repository`. O filtro é ativado na sessão Hibernate corrente (aberta pelo `@Transactional` upstream). O check `getEnabledFilter == null` evita reativar se já estiver ativo no mesmo request.

- [ ] **Step 5: Garantir que AOP está habilitado**

Verificar que `backend/src/main/java/.../WachafitApplication.java` não desabilita AOP. A dependência `spring-boot-starter-aop` deve estar no `pom.xml`. Adicionar se não existir:

```xml
<!-- backend/pom.xml — dentro de <dependencies> -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

- [ ] **Step 6: Rodar todos os testes até aqui**

```bash
mvn test -pl . -Dtest="TenantContextTest,TenantRepositoryTest,AuthServiceTest,JwtUtilTest" -q 2>&1 | tail -10
```
Esperado: PASS (todos).

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/github/mwacha/wachafit/tenant/TenantAwareEntity.java \
        backend/src/main/java/com/github/mwacha/wachafit/tenant/TenantFilterAspect.java \
        backend/src/test/java/com/github/mwacha/wachafit/tenant/TenantContextTest.java \
        backend/pom.xml
git commit -m "feat(tenant): TenantAwareEntity MappedSuperclass + TenantFilterAspect @Before"
```

---

## Task 5: Migration V31 — adicionar tenant_id a todas as tabelas operacionais

**Files:**
- Create: `backend/src/main/resources/db/migration/V31__add_tenant_to_all_tables.sql`

**Interfaces:**
- Consumes: migration V29 (tenant padrão `00000000-0000-0000-0000-000000000001` já existe)
- Produces: coluna `tenant_id UUID NOT NULL` em todas as tabelas abaixo

- [ ] **Step 1: Criar a migration V31**

```sql
-- backend/src/main/resources/db/migration/V31__add_tenant_to_all_tables.sql
DO $$
DECLARE
    default_tenant UUID := '00000000-0000-0000-0000-000000000001';
    tables TEXT[] := ARRAY[
        'group_classes', 'class_enrollments',
        'schedules', 'bookings',
        'physical_assessments', 'assessment_measurements',
        'exercises',
        'workout_plans', 'workout_plan_items', 'workout_logs', 'personal_records',
        'progress_photos',
        'student_goals',
        'student_profiles', 'student_health',
        'trainer_profiles',
        'trainer_availability',
        'membership_plans', 'member_subscriptions', 'payment_charges'
    ];
    tbl TEXT;
BEGIN
    FOREACH tbl IN ARRAY tables LOOP
        EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS tenant_id UUID', tbl);
        EXECUTE format('UPDATE %I SET tenant_id = $1 WHERE tenant_id IS NULL', tbl)
            USING default_tenant;
        EXECUTE format('ALTER TABLE %I ALTER COLUMN tenant_id SET NOT NULL', tbl);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_tenant ON %I(tenant_id)', tbl, tbl);
    END LOOP;
END $$;
```

- [ ] **Step 2: Testar a migration contra o banco de dev**

```bash
# Suba o banco de dev (Docker) e rode as migrations
cd backend
mvn flyway:migrate -q 2>&1 | tail -10
```
Esperado: `Successfully applied 1 migration to schema "public"` (V31).

- [ ] **Step 3: Confirmar colunas criadas**

```bash
# Conecte ao banco e verifique uma das tabelas:
docker exec -it <container_postgres> psql -U postgres -d wachafit -c "\d group_classes"
```
Esperado: coluna `tenant_id uuid not null` presente.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/resources/db/migration/V31__add_tenant_to_all_tables.sql
git commit -m "feat(tenant): migration V31 — tenant_id em todas as tabelas operacionais"
```

---

## Task 6: Todas as entidades estendem TenantAwareEntity

**Files modificados** (todas as entidades tenant-scoped):

```
groupclass/GroupClass.java
schedule/Schedule.java
booking/Booking.java
assessment/PhysicalAssessment.java
assessment/AssessmentMeasurement.java
exercise/Exercise.java
workout/WorkoutPlan.java
workout/WorkoutPlanItem.java
workout/WorkoutLog.java
workout/PersonalRecord.java
progress/ProgressPhoto.java
goal/StudentGoal.java
profile/StudentProfile.java
profile/StudentHealth.java
profile/TrainerProfile.java
membership/MembershipPlan.java
membership/MemberSubscription.java
billing/PaymentCharge.java
```

- [ ] **Step 1: Para cada entidade acima, aplicar o seguinte padrão**

Substitua `public class XYZ {` por `public class XYZ extends TenantAwareEntity {` e adicione o import:

```java
import com.github.mwacha.wachafit.tenant.TenantAwareEntity;
```

Execute o seguinte script para fazer as 18 substituições de uma vez:

```bash
cd backend/src/main/java/com/github/mwacha/wachafit

ENTITIES=(
  "groupclass/GroupClass.java"
  "groupclass/ClassEnrollment.java"
  "schedule/Schedule.java"
  "booking/Booking.java"
  "assessment/PhysicalAssessment.java"
  "assessment/AssessmentMeasurement.java"
  "exercise/Exercise.java"
  "workout/WorkoutPlan.java"
  "workout/WorkoutPlanItem.java"
  "workout/WorkoutLog.java"
  "workout/PersonalRecord.java"
  "progress/ProgressPhoto.java"
  "goal/StudentGoal.java"
  "profile/StudentProfile.java"
  "profile/StudentHealth.java"
  "profile/TrainerProfile.java"
  "membership/MembershipPlan.java"
  "membership/MemberSubscription.java"
  "billing/PaymentCharge.java"
)

for f in "${ENTITIES[@]}"; do
  # Adicionar import após o último import existente
  sed -i '' '/^import /!b; $!{p;d}; /^import /a\
import com.github.mwacha.wachafit.tenant.TenantAwareEntity;' "$f" 2>/dev/null || true
  # Alterar declaração da classe
  sed -i '' 's/^public class \([A-Za-z]*\) {$/public class \1 extends TenantAwareEntity {/' "$f"
done
```

> **Atenção:** Verificar cada arquivo após o script para garantir que o `extends` foi inserido corretamente e que não há duplicação de imports. Se o script falhar em algum arquivo, faça a edição manualmente.

- [ ] **Step 2: Verificar que não há duplicação do campo `tenantId`**

Se alguma entidade já tiver um campo `tenantId` ou `tenant_id` explicitamente declarado, removê-lo — agora ele vem do `TenantAwareEntity`.

```bash
grep -rn "tenantId\|tenant_id" groupclass/ schedule/ booking/ assessment/ exercise/ workout/ progress/ goal/ profile/ membership/ billing/
```

Remova manualmente qualquer campo `private UUID tenantId` duplicado encontrado.

- [ ] **Step 3: Compilar para checar erros**

```bash
mvn compile -q 2>&1 | grep -i error
```
Esperado: zero erros de compilação.

- [ ] **Step 4: Rodar os testes de integração existentes**

```bash
mvn test -pl . -q 2>&1 | tail -20
```

Se algum teste falhar por `TenantContext` null durante `@PrePersist`, adicione no `@BeforeEach` do teste afetado:

```java
@BeforeEach
void setupTenant() {
    TenantContext.set(UUID.fromString("00000000-0000-0000-0000-000000000001"));
}
@AfterEach
void clearTenant() { TenantContext.clear(); }
```

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/github/mwacha/wachafit/
git commit -m "feat(tenant): todas as entidades extendem TenantAwareEntity + @Filter automático"
```

---

## Task 7: ReportRepository — adicionar tenant_id às queries nativas

**Files:**
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/report/ReportRepository.java`
- Test: `backend/src/test/java/com/github/mwacha/wachafit/report/ReportRepositoryTest.java`

**Interfaces:**
- Consumes: `TenantContext.get()` (Task 2)
- Produces: todas as queries retornam apenas dados do tenant corrente

As queries nativas do `ReportRepository` usam `EntityManager.createNativeQuery` e não passam pelo Hibernate Filter. É preciso adicionar `AND tenant_id = :tenantId` manualmente em cada uma.

- [ ] **Step 1: Escrever o teste que falha**

```java
// backend/src/test/java/com/github/mwacha/wachafit/report/ReportRepositoryTest.java
package com.github.mwacha.wachafit.report;

import com.github.mwacha.wachafit.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(ReportRepository.class)
class ReportRepositoryTest {

    @Autowired ReportRepository reportRepository;

    @AfterEach
    void cleanup() { TenantContext.clear(); }

    @Test
    void getSubscriptionStatusCounts_returnEmptyWithoutData() {
        TenantContext.set(UUID.randomUUID());
        // Não deve explodir; pode retornar lista vazia
        assertThat(reportRepository.getSubscriptionStatusCounts()).isNotNull();
    }
}
```

- [ ] **Step 2: Rodar para confirmar que passa**

```bash
mvn test -pl . -Dtest=ReportRepositoryTest -q 2>&1 | tail -5
```

- [ ] **Step 3: Atualizar todas as queries nativas para filtrar por tenant**

Reescreva o `ReportRepository.java` completo com `:tenantId` em todas as queries:

```java
// report/ReportRepository.java
package com.github.mwacha.wachafit.report;

import com.github.mwacha.wachafit.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public class ReportRepository {

    @PersistenceContext
    private EntityManager em;

    private UUID tenantId() { return TenantContext.get(); }

    @SuppressWarnings("unchecked")
    public List<Object[]> getRevenueRows(LocalDate from, LocalDate to) {
        return em.createNativeQuery("""
            SELECT DATE_TRUNC('month', paid_at AT TIME ZONE 'UTC')::date AS month,
                   SUM(amount) AS total, COUNT(*) AS charges_count
            FROM payment_charges
            WHERE status = 'PAID'
              AND tenant_id = :tenantId
              AND paid_at >= :from AND paid_at < :to
            GROUP BY 1 ORDER BY 1
            """)
            .setParameter("tenantId", tenantId())
            .setParameter("from", from.atStartOfDay())
            .setParameter("to", to.plusDays(1).atStartOfDay())
            .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getOverdueStudentRows() {
        return em.createNativeQuery("""
            SELECT c.student_id::text, u.name,
                   SUM(c.amount) AS total_due, MIN(c.due_date) AS oldest_due
            FROM payment_charges c
            JOIN users u ON u.id = c.student_id
            WHERE c.status = 'OVERDUE'
              AND c.tenant_id = :tenantId
            GROUP BY c.student_id, u.name
            ORDER BY MIN(c.due_date) ASC
            """)
            .setParameter("tenantId", tenantId())
            .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getSubscriptionStatusCounts() {
        return em.createQuery("""
            SELECT s.status, COUNT(s)
            FROM MemberSubscription s
            WHERE s.tenantId = :tenantId
            GROUP BY s.status
            """)
            .setParameter("tenantId", tenantId())
            .getResultList();
    }

    public int countExpiredActive() {
        Long count = (Long) em.createQuery("""
            SELECT COUNT(s) FROM MemberSubscription s
            WHERE s.status = 'ACTIVE' AND s.expiresAt < :today AND s.tenantId = :tenantId
            """)
            .setParameter("today", LocalDate.now())
            .setParameter("tenantId", tenantId())
            .getSingleResult();
        return count.intValue();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getTrainerCommissionRows(LocalDate from, LocalDate to) {
        return em.createNativeQuery("""
            SELECT u.id::text, u.name, tp.commission_type, tp.commission_value,
                   COUNT(b.id) AS classes_count
            FROM users u
            JOIN trainer_profiles tp ON tp.user_id = u.id AND tp.tenant_id = :tenantId
            LEFT JOIN schedules s ON s.trainer_id = u.id AND s.tenant_id = :tenantId
                AND s.starts_at::date BETWEEN :from AND :to
            LEFT JOIN bookings b ON b.schedule_id = s.id AND b.status = 'CONFIRMED'
                AND b.tenant_id = :tenantId
            WHERE u.role IN ('TRAINER','PROFESSOR') AND u.tenant_id = :tenantId
            GROUP BY u.id, u.name, tp.commission_type, tp.commission_value
            ORDER BY u.name
            """)
            .setParameter("tenantId", tenantId())
            .setParameter("from", from)
            .setParameter("to", to)
            .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getEnrollmentTrendRows(LocalDate from) {
        return em.createNativeQuery("""
            SELECT TO_CHAR(DATE_TRUNC('month', started_at), 'YYYY-MM') AS month,
                   COUNT(*) AS cnt
            FROM member_subscriptions
            WHERE started_at >= :from AND tenant_id = :tenantId
            GROUP BY 1 ORDER BY 1
            """)
            .setParameter("from", from)
            .setParameter("tenantId", tenantId())
            .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getAttendanceRankingRows(LocalDate from, int limit) {
        return em.createNativeQuery("""
            SELECT u.name, COUNT(b.id) AS cnt
            FROM bookings b
            JOIN users u ON u.id = b.student_id
            WHERE b.status = 'CONFIRMED'
              AND b.tenant_id = :tenantId
              AND b.booked_at >= :from
            GROUP BY u.id, u.name
            ORDER BY cnt DESC
            LIMIT :limit
            """)
            .setParameter("tenantId", tenantId())
            .setParameter("from", from.atStartOfDay())
            .setParameter("limit", limit)
            .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getCashFlowRows(LocalDate from, LocalDate to) {
        return em.createNativeQuery("""
            SELECT due_date,
                   SUM(CASE WHEN status = 'PAID'    THEN amount ELSE 0 END) AS received,
                   SUM(CASE WHEN status = 'PENDING' THEN amount ELSE 0 END) AS pending,
                   SUM(CASE WHEN status = 'OVERDUE' THEN amount ELSE 0 END) AS overdue
            FROM payment_charges
            WHERE due_date BETWEEN :from AND :to
              AND tenant_id = :tenantId
            GROUP BY due_date ORDER BY due_date
            """)
            .setParameter("tenantId", tenantId())
            .setParameter("from", from)
            .setParameter("to", to)
            .getResultList();
    }
}
```

- [ ] **Step 4: Rodar o teste**

```bash
mvn test -pl . -Dtest=ReportRepositoryTest -q 2>&1 | tail -5
```
Esperado: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/github/mwacha/wachafit/report/ReportRepository.java \
        backend/src/test/java/com/github/mwacha/wachafit/report/
git commit -m "feat(tenant): ReportRepository — todas queries nativas filtradas por tenant_id"
```

---

## Task 8: SUPER_ADMIN — TenantService + TenantController + bypass do filtro

**Files:**
- Create: `backend/src/main/java/com/github/mwacha/wachafit/tenant/TenantService.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/tenant/TenantController.java`
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/config/SecurityConfig.java`
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/tenant/TenantFilterAspect.java`
- Test: `backend/src/test/java/com/github/mwacha/wachafit/tenant/TenantControllerTest.java`

**Interfaces:**
- Consumes: `TenantRepository` (Task 1), `Role.SUPER_ADMIN` (Task 3)
- Produces: `POST /api/super/tenants`, `GET /api/super/tenants` — apenas SUPER_ADMIN

- [ ] **Step 1: Escrever o teste que falha**

```java
// backend/src/test/java/com/github/mwacha/wachafit/tenant/TenantControllerTest.java
package com.github.mwacha.wachafit.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mwacha.wachafit.tenant.dto.CreateTenantRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TenantControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void createTenant_returnsCreated() throws Exception {
        var req = new CreateTenantRequest("Academia Fitness", "academia-fitness");
        mockMvc.perform(post("/api/super/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.slug").value("academia-fitness"))
            .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createTenant_forbiddenForAdmin() throws Exception {
        var req = new CreateTenantRequest("Outra", "outra");
        mockMvc.perform(post("/api/super/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void listTenants_returnsList() throws Exception {
        mockMvc.perform(get("/api/super/tenants"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }
}
```

- [ ] **Step 2: Rodar para confirmar falha**

```bash
mvn test -pl . -Dtest=TenantControllerTest -q 2>&1 | tail -10
```

- [ ] **Step 3: Criar TenantService**

```java
// tenant/TenantService.java
package com.github.mwacha.wachafit.tenant;

import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.tenant.dto.CreateTenantRequest;
import com.github.mwacha.wachafit.tenant.dto.TenantResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TenantService {

    private final TenantRepository repo;

    public TenantService(TenantRepository repo) { this.repo = repo; }

    public TenantResponse create(CreateTenantRequest req) {
        if (repo.findBySlug(req.slug()).isPresent()) {
            throw new BusinessException("Slug já em uso: " + req.slug());
        }
        Tenant t = new Tenant();
        t.setName(req.name());
        t.setSlug(req.slug());
        Tenant saved = repo.save(t);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> list() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    private TenantResponse toResponse(Tenant t) {
        return new TenantResponse(
            t.getId().toString(), t.getName(), t.getSlug(),
            t.isActive(),
            t.getCreatedAt() != null ? t.getCreatedAt().toString() : null
        );
    }
}
```

- [ ] **Step 4: Criar TenantController**

```java
// tenant/TenantController.java
package com.github.mwacha.wachafit.tenant;

import com.github.mwacha.wachafit.tenant.dto.CreateTenantRequest;
import com.github.mwacha.wachafit.tenant.dto.TenantResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/super/tenants")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class TenantController {

    private final TenantService service;

    public TenantController(TenantService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TenantResponse create(@Valid @RequestBody CreateTenantRequest req) {
        return service.create(req);
    }

    @GetMapping
    public List<TenantResponse> list() {
        return service.list();
    }
}
```

- [ ] **Step 5: Atualizar TenantFilterAspect para ignorar SUPER_ADMIN**

```java
// TenantFilterAspect.java — atualizar o método enableTenantFilter:
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

@Before("within(@org.springframework.stereotype.Repository *)")
public void enableTenantFilter() {
    // SUPER_ADMIN bypassa o filtro de tenant
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
        return;
    }

    UUID tenantId = TenantContext.get();
    if (tenantId != null) {
        Session session = entityManager.unwrap(Session.class);
        if (session.getEnabledFilter("tenantFilter") == null) {
            session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
        }
    }
}
```

- [ ] **Step 6: Rodar os testes**

```bash
mvn test -pl . -Dtest=TenantControllerTest -q 2>&1 | tail -10
```
Esperado: PASS (3 testes).

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/github/mwacha/wachafit/tenant/TenantService.java \
        backend/src/main/java/com/github/mwacha/wachafit/tenant/TenantController.java \
        backend/src/main/java/com/github/mwacha/wachafit/tenant/TenantFilterAspect.java \
        backend/src/test/java/com/github/mwacha/wachafit/tenant/TenantControllerTest.java
git commit -m "feat(tenant): SUPER_ADMIN TenantController + bypass do Hibernate filter"
```

---

## Task 9: Frontend — login com tenantSlug + auth store com tenantId

**Files:**
- Modify: `frontend/src/types/api.ts`
- Modify: `frontend/src/stores/auth.store.ts`
- Modify: `frontend/src/views/auth/LoginView.vue`
- Modify: `frontend/src/views/auth/RegisterView.vue`

**Interfaces:**
- Consumes: `LoginResponse.tenantId` (Task 3 backend)
- Produces: `auth.tenantId` persistido no localStorage; `LoginView` envia `tenantSlug`

- [ ] **Step 1: Atualizar tipos em api.ts**

```typescript
// frontend/src/types/api.ts — alterar as interfaces:
export interface LoginRequest {
  email: string
  password: string
  tenantSlug: string
}

export interface LoginResponse {
  token: string
  role: Role
  userId: string
  tenantId: string
}
```

- [ ] **Step 2: Atualizar auth.store.ts**

```typescript
// frontend/src/stores/auth.store.ts
export const useAuthStore = defineStore('auth', () => {
  const token    = ref<string | null>(localStorage.getItem('token'))
  const userId   = ref<string | null>(localStorage.getItem('userId'))
  const role     = ref<Role | null>((localStorage.getItem('role') as Role) ?? null)
  const tenantId = ref<string | null>(localStorage.getItem('tenantId'))  // NOVO

  const isAuthenticated = computed(() => token.value !== null)

  function setSession(data: LoginResponse) {
    token.value    = data.token
    userId.value   = data.userId
    role.value     = data.role
    tenantId.value = data.tenantId  // NOVO
    localStorage.setItem('token',    data.token)
    localStorage.setItem('userId',   data.userId)
    localStorage.setItem('role',     data.role)
    localStorage.setItem('tenantId', data.tenantId)  // NOVO
  }

  function clearSession() {
    token.value = null; userId.value = null; role.value = null; tenantId.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('role')
    localStorage.removeItem('tenantId')  // NOVO
  }

  async function login(email: string, password: string, tenantSlug: string): Promise<LoginResponse> {
    const { data } = await api.post<LoginResponse>('/api/auth/login', { email, password, tenantSlug })
    setSession(data)
    return data
  }

  async function register(name: string, email: string, password: string, tenantSlug: string): Promise<LoginResponse> {
    const { data } = await api.post<LoginResponse>('/api/auth/register', { name, email, password, tenantSlug })
    setSession(data)
    return data
  }

  function logout() { clearSession() }

  if (token.value) {
    const payload = decodeJwtPayload(token.value)
    if (!payload) clearSession()
  }

  return { token, userId, role, tenantId, isAuthenticated, login, register, logout, clearSession }
})
```

- [ ] **Step 3: Atualizar LoginView.vue — adicionar campo tenantSlug**

Dentro do `<form>`, antes do campo de email, adicionar:

```vue
<!-- LoginView.vue — dentro do form, campo novo: -->
<div class="field">
  <label class="field-label">Slug da academia</label>
  <InputText v-model="tenantSlug" placeholder="ex: minha-academia"
             autocomplete="organization" class="field-input" required />
</div>
```

No `<script setup>`:

```typescript
const tenantSlug = ref('')

// Na função de submit, atualizar a chamada:
async function submit() {
  loading.value = true
  error.value = ''
  try {
    await auth.login(email.value, password.value, tenantSlug.value)
    router.push(roleDashboards[auth.role!])
  } catch (e: any) {
    error.value = e.response?.data?.message ?? 'Credenciais inválidas'
  } finally {
    loading.value = false
  }
}
```

- [ ] **Step 4: Atualizar RegisterView.vue — adicionar campo tenantSlug**

Mesma estrutura do LoginView: adicionar campo `tenantSlug` com `ref('')` e passar para `auth.register(name, email, password, tenantSlug)`.

- [ ] **Step 5: Verificar que o build não tem erros de tipo**

```bash
cd frontend
npx vue-tsc --noEmit 2>&1 | head -20
```
Esperado: zero erros.

- [ ] **Step 6: Testar manualmente o fluxo de login**

1. Iniciar o backend: `cd backend && mvn spring-boot:run`
2. Iniciar o frontend: `cd frontend && npm run dev`
3. Abrir `http://localhost:5173/login`
4. Preencher: slug = `personal-studio`, email e senha de um usuário existente
5. Verificar que redireciona para o dashboard correto
6. Abrir DevTools → Application → localStorage → confirmar `tenantId` gravado

- [ ] **Step 7: Commit**

```bash
git add frontend/src/types/api.ts \
        frontend/src/stores/auth.store.ts \
        frontend/src/views/auth/LoginView.vue \
        frontend/src/views/auth/RegisterView.vue
git commit -m "feat(tenant): frontend — login com tenantSlug + tenantId no auth store"
```

---

## Cadastro self-service de academia (tenant) com plano SaaS

> Spec completo em `docs/superpowers/specs/2026-07-22-tenant-signup-design.md`. Tasks 10-13 dependem das Tasks 1-9 (Tenant, TenantContext, User.tenant, JWT com tenantId, TenantAwareEntity) já estarem prontas.

## Task 10: SaasPlan entity + migration V32 + CRUD SUPER_ADMIN

**Files:**
- Create: `backend/src/main/resources/db/migration/V32__create_saas_plans.sql`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/saas/SaasPlan.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/saas/SaasPlanRepository.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/saas/SaasPlanService.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/saas/SaasPlanController.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/saas/dto/CreateSaasPlanRequest.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/saas/dto/SaasPlanResponse.java`
- Test: `backend/src/test/java/com/github/mwacha/wachafit/saas/SaasPlanControllerTest.java`

**Interfaces:**
- Consumes: `Role.SUPER_ADMIN` (Task 3)
- Produces: `SaasPlanRepository.findByActiveTrueOrderByPriceAsc(): List<SaasPlan>` — usado na Task 12 (endpoint público de planos)

- [ ] **Step 1: Escrever o teste que falha**

```java
// backend/src/test/java/com/github/mwacha/wachafit/saas/SaasPlanControllerTest.java
package com.github.mwacha.wachafit.saas;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mwacha.wachafit.saas.dto.CreateSaasPlanRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SaasPlanControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void createPlan_returnsCreated() throws Exception {
        var req = new CreateSaasPlanRequest("Starter", "Plano inicial", new BigDecimal("149.90"), 1, 5);
        mockMvc.perform(post("/api/super/saas-plans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Starter"))
            .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createPlan_forbiddenForAdmin() throws Exception {
        var req = new CreateSaasPlanRequest("Starter", "Plano inicial", new BigDecimal("149.90"), 1, 5);
        mockMvc.perform(post("/api/super/saas-plans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void listPlans_returnsList() throws Exception {
        mockMvc.perform(get("/api/super/saas-plans"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }
}
```

- [ ] **Step 2: Rodar para confirmar falha**

```bash
cd backend
mvn test -pl . -Dtest=SaasPlanControllerTest -q 2>&1 | tail -15
```
Esperado: FAIL — pacote `saas` e `CreateSaasPlanRequest` não existem ainda.

- [ ] **Step 3: Criar a migration V32**

```sql
-- backend/src/main/resources/db/migration/V32__create_saas_plans.sql
CREATE TABLE saas_plans (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                   VARCHAR(100) NOT NULL,
    description            TEXT,
    price                  NUMERIC(10,2) NOT NULL,
    billing_period_months  INT NOT NULL DEFAULT 1,
    max_users              INT,
    active                 BOOLEAN NOT NULL DEFAULT true,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Planos padrão disponíveis no cadastro self-service (Task 12).
-- IDs fixos para permitir referência determinística no seed da Task 11.
INSERT INTO saas_plans (id, name, description, price, billing_period_months, max_users, active) VALUES
    ('00000000-0000-0000-0000-000000000101', 'Starter', 'Ideal para academias pequenas', 149.90, 1, 5, true),
    ('00000000-0000-0000-0000-000000000102', 'Pro', 'Para academias em crescimento, sem limite de turmas', 299.90, 1, 20, true);
```

- [ ] **Step 4: Criar a entidade SaasPlan**

```java
// backend/src/main/java/com/github/mwacha/wachafit/saas/SaasPlan.java
package com.github.mwacha.wachafit.saas;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saas_plans")
public class SaasPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "billing_period_months", nullable = false)
    private int billingPeriodMonths = 1;

    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal v) { this.price = v; }
    public int getBillingPeriodMonths() { return billingPeriodMonths; }
    public void setBillingPeriodMonths(int v) { this.billingPeriodMonths = v; }
    public Integer getMaxUsers() { return maxUsers; }
    public void setMaxUsers(Integer v) { this.maxUsers = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }
    public Instant getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 5: Criar SaasPlanRepository, DTOs, Service e Controller**

```java
// backend/src/main/java/com/github/mwacha/wachafit/saas/SaasPlanRepository.java
package com.github.mwacha.wachafit.saas;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SaasPlanRepository extends JpaRepository<SaasPlan, UUID> {
    List<SaasPlan> findByActiveTrueOrderByPriceAsc();
}
```

```java
// backend/src/main/java/com/github/mwacha/wachafit/saas/dto/CreateSaasPlanRequest.java
package com.github.mwacha.wachafit.saas.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateSaasPlanRequest(
    @NotBlank String name,
    String description,
    @NotNull @DecimalMin("0.01") BigDecimal price,
    @Min(1) int billingPeriodMonths,
    Integer maxUsers
) {}
```

```java
// backend/src/main/java/com/github/mwacha/wachafit/saas/dto/SaasPlanResponse.java
package com.github.mwacha.wachafit.saas.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SaasPlanResponse(
    UUID id, String name, String description, BigDecimal price,
    int billingPeriodMonths, Integer maxUsers, boolean active, Instant createdAt
) {}
```

```java
// backend/src/main/java/com/github/mwacha/wachafit/saas/SaasPlanService.java
package com.github.mwacha.wachafit.saas;

import com.github.mwacha.wachafit.saas.dto.CreateSaasPlanRequest;
import com.github.mwacha.wachafit.saas.dto.SaasPlanResponse;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SaasPlanService {

    private final SaasPlanRepository repo;

    public SaasPlanService(SaasPlanRepository repo) { this.repo = repo; }

    public SaasPlanResponse create(CreateSaasPlanRequest req) {
        SaasPlan plan = new SaasPlan();
        applyRequest(plan, req);
        return toResponse(repo.save(plan));
    }

    public SaasPlanResponse update(UUID id, CreateSaasPlanRequest req) {
        SaasPlan plan = repo.findById(id).orElseThrow(() -> new NotFoundException("Plano não encontrado"));
        applyRequest(plan, req);
        return toResponse(repo.save(plan));
    }

    public void deactivate(UUID id) {
        SaasPlan plan = repo.findById(id).orElseThrow(() -> new NotFoundException("Plano não encontrado"));
        plan.setActive(false);
        repo.save(plan);
    }

    @Transactional(readOnly = true)
    public List<SaasPlanResponse> list() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    private void applyRequest(SaasPlan plan, CreateSaasPlanRequest req) {
        plan.setName(req.name());
        plan.setDescription(req.description());
        plan.setPrice(req.price());
        plan.setBillingPeriodMonths(req.billingPeriodMonths());
        plan.setMaxUsers(req.maxUsers());
    }

    private SaasPlanResponse toResponse(SaasPlan p) {
        return new SaasPlanResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice(),
            p.getBillingPeriodMonths(), p.getMaxUsers(), p.isActive(), p.getCreatedAt());
    }
}
```

```java
// backend/src/main/java/com/github/mwacha/wachafit/saas/SaasPlanController.java
package com.github.mwacha.wachafit.saas;

import com.github.mwacha.wachafit.saas.dto.CreateSaasPlanRequest;
import com.github.mwacha.wachafit.saas.dto.SaasPlanResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/super/saas-plans")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SaasPlanController {

    private final SaasPlanService service;

    public SaasPlanController(SaasPlanService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaasPlanResponse create(@Valid @RequestBody CreateSaasPlanRequest req) {
        return service.create(req);
    }

    @GetMapping
    public List<SaasPlanResponse> list() {
        return service.list();
    }

    @PutMapping("/{id}")
    public SaasPlanResponse update(@PathVariable UUID id, @Valid @RequestBody CreateSaasPlanRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID id) {
        service.deactivate(id);
    }
}
```

- [ ] **Step 6: Rodar os testes**

```bash
mvn test -pl . -Dtest=SaasPlanControllerTest -q 2>&1 | tail -15
```
Esperado: PASS (3 testes).

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/resources/db/migration/V32__create_saas_plans.sql \
        backend/src/main/java/com/github/mwacha/wachafit/saas/ \
        backend/src/test/java/com/github/mwacha/wachafit/saas/SaasPlanControllerTest.java
git commit -m "feat(saas): SaasPlan entity + migration V32 + CRUD SUPER_ADMIN"
```

---

## Task 11: TenantSubscription + TenantCharge + migration V33 + campos de empresa no Tenant

**Files:**
- Create: `backend/src/main/resources/db/migration/V33__create_tenant_subscriptions_and_charges.sql`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/saas/TenantSubscription.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/saas/TenantSubscriptionRepository.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/saas/TenantCharge.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/saas/TenantChargeRepository.java`
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/tenant/Tenant.java`
- Test: `backend/src/test/java/com/github/mwacha/wachafit/saas/TenantSubscriptionRepositoryTest.java`

**Interfaces:**
- Consumes: `SaasPlan` id `00000000-0000-0000-0000-000000000102` (Task 10, plano "Pro"), `Tenant` id `00000000-0000-0000-0000-000000000001` (Task 1, "Personal Studio")
- Produces:
  - `Tenant.getCnpj()/setCnpj()`, `Tenant.getPhone()/setPhone()` — usados na Task 12
  - `TenantSubscriptionRepository.save(TenantSubscription)` — usado na Task 12
  - `TenantChargeRepository.save(TenantCharge)` — usado na Task 12

- [ ] **Step 1: Escrever o teste que falha**

```java
// backend/src/test/java/com/github/mwacha/wachafit/saas/TenantSubscriptionRepositoryTest.java
package com.github.mwacha.wachafit.saas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TenantSubscriptionRepositoryTest {

    @Autowired TenantSubscriptionRepository subscriptionRepo;
    @Autowired TenantChargeRepository chargeRepo;

    @Test
    void savesSubscriptionAndCharge() {
        UUID tenantId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        TenantSubscription sub = new TenantSubscription();
        sub.setTenantId(tenantId);
        sub.setSaasPlanId(planId);
        sub.setStatus("TRIALING");
        sub.setTrialEndsAt(Instant.now().plus(14, ChronoUnit.DAYS));
        TenantSubscription savedSub = subscriptionRepo.save(sub);

        TenantCharge charge = new TenantCharge();
        charge.setTenantId(tenantId);
        charge.setSubscriptionId(savedSub.getId());
        charge.setAmount(new BigDecimal("299.90"));
        charge.setDueDate(LocalDate.now().plusDays(14));
        charge.setStatus("PENDING");
        charge.setPaymentMethod("PIX");
        TenantCharge savedCharge = chargeRepo.save(charge);

        var foundSub = subscriptionRepo.findById(savedSub.getId());
        assertThat(foundSub).isPresent();
        assertThat(foundSub.get().getStatus()).isEqualTo("TRIALING");

        var foundCharge = chargeRepo.findById(savedCharge.getId());
        assertThat(foundCharge).isPresent();
        assertThat(foundCharge.get().getSubscriptionId()).isEqualTo(savedSub.getId());
        assertThat(foundCharge.get().getPaymentMethod()).isEqualTo("PIX");
    }
}
```

- [ ] **Step 2: Rodar para confirmar falha**

```bash
cd backend
mvn test -pl . -Dtest=TenantSubscriptionRepositoryTest -q 2>&1 | tail -15
```
Esperado: FAIL — `TenantSubscription`/`TenantCharge` não existem ainda.

- [ ] **Step 3: Criar a migration V33**

```sql
-- backend/src/main/resources/db/migration/V33__create_tenant_subscriptions_and_charges.sql

-- 1. Dados de empresa no tenant (nullable — tenants criados pelo SUPER_ADMIN via
--    POST /api/super/tenants, Task 8, podem não preenchê-los de imediato)
ALTER TABLE tenants ADD COLUMN cnpj VARCHAR(14) UNIQUE;
ALTER TABLE tenants ADD COLUMN phone VARCHAR(20);

-- 2. Assinatura do tenant a um SaasPlan
CREATE TABLE tenant_subscriptions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    saas_plan_id        UUID NOT NULL REFERENCES saas_plans(id),
    status              VARCHAR(20) NOT NULL DEFAULT 'TRIALING',
    trial_ends_at       TIMESTAMPTZ,
    current_period_end  TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 3. Cobranças da assinatura do tenant
CREATE TABLE tenant_charges (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    subscription_id UUID NOT NULL REFERENCES tenant_subscriptions(id),
    amount          NUMERIC(10,2) NOT NULL,
    due_date        DATE NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method  VARCHAR(20),
    paid_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 4. Backfill: Personal Studio já é cliente ativo hoje — assinatura ACTIVE,
--    sem trial e sem cobrança retroativa (a próxima cobrança nasce do ciclo normal).
INSERT INTO tenant_subscriptions (tenant_id, saas_plan_id, status, trial_ends_at, current_period_end)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000102',
    'ACTIVE',
    NULL,
    now() + interval '1 month'
);
```

- [ ] **Step 4: Criar a entidade TenantSubscription**

```java
// backend/src/main/java/com/github/mwacha/wachafit/saas/TenantSubscription.java
package com.github.mwacha.wachafit.saas;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_subscriptions")
public class TenantSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "saas_plan_id", nullable = false)
    private UUID saasPlanId;

    @Column(nullable = false, length = 20)
    private String status = "TRIALING";

    @Column(name = "trial_ends_at")
    private Instant trialEndsAt;

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getSaasPlanId() { return saasPlanId; }
    public void setSaasPlanId(UUID v) { this.saasPlanId = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public Instant getTrialEndsAt() { return trialEndsAt; }
    public void setTrialEndsAt(Instant v) { this.trialEndsAt = v; }
    public Instant getCurrentPeriodEnd() { return currentPeriodEnd; }
    public void setCurrentPeriodEnd(Instant v) { this.currentPeriodEnd = v; }
    public Instant getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 5: Criar a entidade TenantCharge**

```java
// backend/src/main/java/com/github/mwacha/wachafit/saas/TenantCharge.java
package com.github.mwacha.wachafit.saas;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenant_charges")
public class TenantCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(UUID v) { this.subscriptionId = v; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { this.amount = v; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate v) { this.dueDate = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String v) { this.paymentMethod = v; }
    public OffsetDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(OffsetDateTime v) { this.paidAt = v; }
    public Instant getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 6: Criar os repositórios**

```java
// backend/src/main/java/com/github/mwacha/wachafit/saas/TenantSubscriptionRepository.java
package com.github.mwacha.wachafit.saas;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface TenantSubscriptionRepository extends JpaRepository<TenantSubscription, UUID> {
}
```

```java
// backend/src/main/java/com/github/mwacha/wachafit/saas/TenantChargeRepository.java
package com.github.mwacha.wachafit.saas;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface TenantChargeRepository extends JpaRepository<TenantCharge, UUID> {
}
```

- [ ] **Step 7: Atualizar Tenant.java — adicionar cnpj e phone**

Adicione dentro da classe `Tenant` (arquivo do Task 1), após o campo `active`:

```java
// tenant/Tenant.java — campos novos e seus getters/setters
@Column(length = 14, unique = true)
private String cnpj;

@Column(length = 20)
private String phone;

public String getCnpj()           { return cnpj; }
public void setCnpj(String v)     { this.cnpj = v; }
public String getPhone()          { return phone; }
public void setPhone(String v)    { this.phone = v; }
```

- [ ] **Step 8: Rodar os testes**

```bash
mvn test -pl . -Dtest=TenantSubscriptionRepositoryTest -q 2>&1 | tail -15
```
Esperado: PASS (1 teste).

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/resources/db/migration/V33__create_tenant_subscriptions_and_charges.sql \
        backend/src/main/java/com/github/mwacha/wachafit/saas/TenantSubscription.java \
        backend/src/main/java/com/github/mwacha/wachafit/saas/TenantSubscriptionRepository.java \
        backend/src/main/java/com/github/mwacha/wachafit/saas/TenantCharge.java \
        backend/src/main/java/com/github/mwacha/wachafit/saas/TenantChargeRepository.java \
        backend/src/main/java/com/github/mwacha/wachafit/tenant/Tenant.java \
        backend/src/test/java/com/github/mwacha/wachafit/saas/TenantSubscriptionRepositoryTest.java
git commit -m "feat(saas): TenantSubscription + TenantCharge + migration V33 + cnpj/phone no Tenant"
```

---

## Task 12: SignupService + endpoints públicos de cadastro

**Files:**
- Create: `backend/src/main/java/com/github/mwacha/wachafit/saas/PaymentMethod.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/saas/CnpjValidator.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/saas/dto/SignupRequest.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/saas/SignupService.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/saas/PublicSignupController.java`
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/tenant/TenantRepository.java`
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/config/SecurityConfig.java`
- Test: `backend/src/test/java/com/github/mwacha/wachafit/saas/CnpjValidatorTest.java`
- Test: `backend/src/test/java/com/github/mwacha/wachafit/saas/SignupServiceTest.java`
- Test: `backend/src/test/java/com/github/mwacha/wachafit/saas/PublicSignupControllerTest.java`

**Interfaces:**
- Consumes: `TenantRepository.findBySlug` (Task 1), `SaasPlanRepository.findByActiveTrueOrderByPriceAsc` (Task 10), `TenantSubscriptionRepository`/`TenantChargeRepository` (Task 11), `Tenant.setCnpj/setPhone` (Task 11), `JwtUtil.generateToken(User)` (Task 2), `LoginResponse` (Task 3)
- Produces: `POST /api/public/signup`, `GET /api/public/saas-plans`, `GET /api/public/check-slug` — todos sem autenticação

- [ ] **Step 1: Escrever o teste do CnpjValidator (falha)**

```java
// backend/src/test/java/com/github/mwacha/wachafit/saas/CnpjValidatorTest.java
package com.github.mwacha.wachafit.saas;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CnpjValidatorTest {

    @Test
    void validCnpj_returnsTrue() {
        assertThat(CnpjValidator.isValid("11222333000181")).isTrue();
    }

    @Test
    void invalidCheckDigit_returnsFalse() {
        assertThat(CnpjValidator.isValid("11222333000180")).isFalse();
    }

    @Test
    void wrongLength_returnsFalse() {
        assertThat(CnpjValidator.isValid("1122233300018")).isFalse();
    }

    @Test
    void allSameDigits_returnsFalse() {
        assertThat(CnpjValidator.isValid("11111111111111")).isFalse();
    }

    @Test
    void nonNumeric_returnsFalse() {
        assertThat(CnpjValidator.isValid("1122233300018a")).isFalse();
    }
}
```

- [ ] **Step 2: Rodar para confirmar falha**

```bash
cd backend
mvn test -pl . -Dtest=CnpjValidatorTest -q 2>&1 | tail -10
```
Esperado: FAIL — `CnpjValidator` não existe.

- [ ] **Step 3: Implementar CnpjValidator**

```java
// backend/src/main/java/com/github/mwacha/wachafit/saas/CnpjValidator.java
package com.github.mwacha.wachafit.saas;

public final class CnpjValidator {

    private static final int[] WEIGHTS_FIRST  = {5,4,3,2,9,8,7,6,5,4,3,2};
    private static final int[] WEIGHTS_SECOND = {6,5,4,3,2,9,8,7,6,5,4,3,2};

    private CnpjValidator() {}

    public static boolean isValid(String cnpj) {
        if (cnpj == null || !cnpj.matches("\\d{14}")) return false;
        if (cnpj.chars().distinct().count() == 1) return false;

        int[] digits = cnpj.chars().map(c -> c - '0').toArray();
        if (calculateCheckDigit(digits, WEIGHTS_FIRST) != digits[12]) return false;
        return calculateCheckDigit(digits, WEIGHTS_SECOND) == digits[13];
    }

    private static int calculateCheckDigit(int[] digits, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += digits[i] * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
```

- [ ] **Step 4: Rodar novamente**

```bash
mvn test -pl . -Dtest=CnpjValidatorTest -q 2>&1 | tail -10
```
Esperado: PASS (5 testes).

- [ ] **Step 5: Criar PaymentMethod e SignupRequest**

```java
// backend/src/main/java/com/github/mwacha/wachafit/saas/PaymentMethod.java
package com.github.mwacha.wachafit.saas;

public enum PaymentMethod {
    CREDIT_CARD, PIX, BOLETO
}
```

```java
// backend/src/main/java/com/github/mwacha/wachafit/saas/dto/SignupRequest.java
package com.github.mwacha.wachafit.saas.dto;

import com.github.mwacha.wachafit.saas.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;

public record SignupRequest(
    @Valid @NotNull AdminData admin,
    @Valid @NotNull CompanyData company,
    @Valid @NotNull PlanData plan
) {
    public record AdminData(
        @NotBlank(message = "Nome é obrigatório") String name,
        @Email(message = "E-mail inválido") @NotBlank String email,
        @NotBlank @Size(min = 8, message = "Senha deve ter ao menos 8 caracteres") String password
    ) {}

    public record CompanyData(
        @NotBlank(message = "Razão social é obrigatória") String name,
        @NotBlank @Pattern(regexp = "\\d{14}", message = "CNPJ deve ter 14 dígitos numéricos") String cnpj,
        @NotBlank(message = "Telefone é obrigatório") String phone,
        @NotBlank @Pattern(regexp = "^[a-z0-9][a-z0-9\\-]*[a-z0-9]$", message = "Slug inválido") String slug
    ) {}

    public record PlanData(
        @NotNull(message = "Selecione um plano") UUID saasPlanId,
        @NotNull(message = "Selecione uma forma de pagamento") PaymentMethod paymentMethod
    ) {}
}
```

- [ ] **Step 6: Escrever o teste do SignupService (falha)**

```java
// backend/src/test/java/com/github/mwacha/wachafit/saas/SignupServiceTest.java
package com.github.mwacha.wachafit.saas;

import com.github.mwacha.wachafit.auth.dto.LoginResponse;
import com.github.mwacha.wachafit.saas.dto.SignupRequest;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.shared.security.JwtUtil;
import com.github.mwacha.wachafit.tenant.Tenant;
import com.github.mwacha.wachafit.tenant.TenantRepository;
import com.github.mwacha.wachafit.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SignupServiceTest {

    private TenantRepository tenantRepository;
    private UserRepository userRepository;
    private SaasPlanRepository saasPlanRepository;
    private TenantSubscriptionRepository subscriptionRepository;
    private TenantChargeRepository chargeRepository;
    private SignupService signupService;

    private static final String VALID_CNPJ = "11222333000181";

    @BeforeEach
    void setup() {
        tenantRepository = mock(TenantRepository.class);
        userRepository = mock(UserRepository.class);
        saasPlanRepository = mock(SaasPlanRepository.class);
        subscriptionRepository = mock(TenantSubscriptionRepository.class);
        chargeRepository = mock(TenantChargeRepository.class);
        JwtUtil jwtUtil = new JwtUtil("super-secret-key-with-at-least-32-chars!!", 3600L);

        signupService = new SignupService(
            tenantRepository, userRepository, saasPlanRepository,
            subscriptionRepository, chargeRepository,
            new BCryptPasswordEncoder(), jwtUtil
        );

        when(tenantRepository.findBySlug(any())).thenReturn(Optional.empty());
        when(tenantRepository.findByCnpj(any())).thenReturn(Optional.empty());
        when(tenantRepository.save(any())).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            setId(t, UUID.randomUUID());
            return t;
        });
        when(userRepository.save(any())).thenAnswer(inv -> {
            var u = inv.getArgument(0, com.github.mwacha.wachafit.user.User.class);
            setId(u, UUID.randomUUID());
            return u;
        });
        when(subscriptionRepository.save(any())).thenAnswer(inv -> {
            TenantSubscription s = inv.getArgument(0);
            setId(s, UUID.randomUUID());
            return s;
        });

        SaasPlan plan = new SaasPlan();
        plan.setPrice(new BigDecimal("299.90"));
        plan.setActive(true);
        setId(plan, UUID.fromString("00000000-0000-0000-0000-000000000102"));
        when(saasPlanRepository.findById(plan.getId())).thenReturn(Optional.of(plan));
    }

    private SignupRequest buildRequest() {
        return new SignupRequest(
            new SignupRequest.AdminData("Maria Admin", "maria@academia.com", "senha1234"),
            new SignupRequest.CompanyData("Academia Fitness Ltda", VALID_CNPJ, "11999998888", "academia-fitness"),
            new SignupRequest.PlanData(UUID.fromString("00000000-0000-0000-0000-000000000102"), PaymentMethod.PIX)
        );
    }

    @Test
    void signup_createsTenantAdminAndReturnsToken() {
        LoginResponse resp = signupService.signup(buildRequest());

        assertThat(resp.token()).isNotBlank();
        assertThat(resp.role()).isEqualTo("ADMIN");
        verify(tenantRepository).save(any());
        verify(userRepository).save(any());
        verify(subscriptionRepository).save(any());
        verify(chargeRepository).save(any());
    }

    @Test
    void signup_throwsBusinessException_whenSlugTaken() {
        Tenant existing = new Tenant();
        when(tenantRepository.findBySlug("academia-fitness")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> signupService.signup(buildRequest()))
            .isInstanceOf(BusinessException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void signup_throwsBusinessException_whenCnpjInvalid() {
        SignupRequest req = new SignupRequest(
            new SignupRequest.AdminData("Maria Admin", "maria@academia.com", "senha1234"),
            new SignupRequest.CompanyData("Academia Fitness Ltda", "11222333000199", "11999998888", "academia-fitness"),
            new SignupRequest.PlanData(UUID.fromString("00000000-0000-0000-0000-000000000102"), PaymentMethod.PIX)
        );

        assertThatThrownBy(() -> signupService.signup(req))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void signup_doesNotRollBack_whenChargeCreationFails() {
        when(chargeRepository.save(any())).thenThrow(new RuntimeException("falha simulada"));

        LoginResponse resp = signupService.signup(buildRequest());

        assertThat(resp.token()).isNotBlank();
        verify(tenantRepository).save(any());
        verify(subscriptionRepository).save(any());
    }

    private static void setId(Object entity, UUID id) {
        try {
            var f = entity.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
```

- [ ] **Step 7: Rodar para confirmar falha**

```bash
mvn test -pl . -Dtest=SignupServiceTest -q 2>&1 | tail -20
```
Esperado: FAIL — `SignupService` e `TenantRepository.findByCnpj` não existem.

- [ ] **Step 8: Adicionar findByCnpj ao TenantRepository**

```java
// tenant/TenantRepository.java — adicionar o método:
Optional<Tenant> findByCnpj(String cnpj);
```

- [ ] **Step 9: Implementar SignupService**

```java
// backend/src/main/java/com/github/mwacha/wachafit/saas/SignupService.java
package com.github.mwacha.wachafit.saas;

import com.github.mwacha.wachafit.auth.dto.LoginResponse;
import com.github.mwacha.wachafit.saas.dto.SignupRequest;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.shared.security.JwtUtil;
import com.github.mwacha.wachafit.tenant.Tenant;
import com.github.mwacha.wachafit.tenant.TenantRepository;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class SignupService {

    private static final Logger log = LoggerFactory.getLogger(SignupService.class);
    private static final int TRIAL_DAYS = 14;

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final SaasPlanRepository saasPlanRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final TenantChargeRepository chargeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public SignupService(
        TenantRepository tenantRepository,
        UserRepository userRepository,
        SaasPlanRepository saasPlanRepository,
        TenantSubscriptionRepository subscriptionRepository,
        TenantChargeRepository chargeRepository,
        PasswordEncoder passwordEncoder,
        JwtUtil jwtUtil
    ) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.saasPlanRepository = saasPlanRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.chargeRepository = chargeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public LoginResponse signup(SignupRequest req) {
        if (tenantRepository.findBySlug(req.company().slug()).isPresent()) {
            throw new BusinessException("Slug já em uso: " + req.company().slug());
        }
        if (tenantRepository.findByCnpj(req.company().cnpj()).isPresent()) {
            throw new BusinessException("CNPJ já cadastrado");
        }
        if (!CnpjValidator.isValid(req.company().cnpj())) {
            throw new BusinessException("CNPJ inválido");
        }
        SaasPlan plan = saasPlanRepository.findById(req.plan().saasPlanId())
            .filter(SaasPlan::isActive)
            .orElseThrow(() -> new BusinessException("Plano inválido ou inativo"));

        Tenant tenant = new Tenant();
        tenant.setName(req.company().name());
        tenant.setSlug(req.company().slug());
        tenant.setCnpj(req.company().cnpj());
        tenant.setPhone(req.company().phone());
        tenant.setActive(true);
        tenant = tenantRepository.save(tenant);

        User admin = new User();
        admin.setName(req.admin().name());
        admin.setEmail(req.admin().email());
        admin.setPasswordHash(passwordEncoder.encode(req.admin().password()));
        admin.setRole(Role.ADMIN);
        admin.setTenant(tenant);
        admin = userRepository.save(admin);

        Instant trialEndsAt = Instant.now().plus(TRIAL_DAYS, ChronoUnit.DAYS);
        TenantSubscription subscription = new TenantSubscription();
        subscription.setTenantId(tenant.getId());
        subscription.setSaasPlanId(plan.getId());
        subscription.setStatus("TRIALING");
        subscription.setTrialEndsAt(trialEndsAt);
        subscription = subscriptionRepository.save(subscription);

        // Best-effort: se a cobrança inicial falhar, a conta é criada mesmo assim.
        // A ausência de charge fica visível para o SUPER_ADMIN reconciliar manualmente depois.
        createFirstCharge(tenant.getId(), subscription.getId(), plan.getPrice(),
            trialEndsAt, req.plan().paymentMethod());

        String token = jwtUtil.generateToken(admin);
        return new LoginResponse(token, admin.getRole().name(), admin.getId().toString(),
            tenant.getId().toString());
    }

    private void createFirstCharge(UUID tenantId, UUID subscriptionId, BigDecimal amount,
                                    Instant dueInstant, PaymentMethod method) {
        try {
            TenantCharge charge = new TenantCharge();
            charge.setTenantId(tenantId);
            charge.setSubscriptionId(subscriptionId);
            charge.setAmount(amount);
            charge.setDueDate(LocalDate.ofInstant(dueInstant, ZoneOffset.UTC));
            charge.setStatus("PENDING");
            charge.setPaymentMethod(method.name());
            chargeRepository.save(charge);
        } catch (Exception e) {
            log.error("Falha ao criar cobrança inicial do tenant {}: {}", tenantId, e.getMessage(), e);
        }
    }
}
```

- [ ] **Step 10: Rodar os testes**

```bash
mvn test -pl . -Dtest=SignupServiceTest -q 2>&1 | tail -20
```
Esperado: PASS (4 testes).

- [ ] **Step 11: Escrever o teste do controller (falha)**

```java
// backend/src/test/java/com/github/mwacha/wachafit/saas/PublicSignupControllerTest.java
package com.github.mwacha.wachafit.saas;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mwacha.wachafit.saas.dto.SignupRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @SpringBootTest usa H2 com flyway desabilitado (application-test.yml) — os planos
// seed das migrations V32/V33 não existem nesse contexto, então o teste cria os seus.
@SpringBootTest
@AutoConfigureMockMvc
class PublicSignupControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired SaasPlanRepository saasPlanRepository;

    private UUID planId;

    @BeforeEach
    void setup() {
        SaasPlan plan = new SaasPlan();
        plan.setName("Pro Teste");
        plan.setDescription("Plano usado nos testes de integração");
        plan.setPrice(new BigDecimal("299.90"));
        plan.setBillingPeriodMonths(1);
        plan.setActive(true);
        planId = saasPlanRepository.save(plan).getId();
    }

    @Test
    void listPlans_isPublic() throws Exception {
        mockMvc.perform(get("/api/public/saas-plans"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void checkSlug_returnsAvailability() throws Exception {
        mockMvc.perform(get("/api/public/check-slug").param("slug", "academia-nova-" + UUID.randomUUID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void signup_returnsCreatedWithToken() throws Exception {
        var req = new SignupRequest(
            new SignupRequest.AdminData("Maria Admin", "maria" + UUID.randomUUID() + "@academia.com", "senha1234"),
            new SignupRequest.CompanyData("Academia Fitness Ltda", "11222333000181", "11999998888",
                "academia-" + UUID.randomUUID()),
            new SignupRequest.PlanData(planId, PaymentMethod.PIX)
        );
        mockMvc.perform(post("/api/public/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.tenantId").isNotEmpty());
    }

    @Test
    void signup_conflictWhenSlugDuplicated() throws Exception {
        String slug = "academia-duplicada-" + UUID.randomUUID();
        var req = new SignupRequest(
            new SignupRequest.AdminData("Maria Admin", "maria" + UUID.randomUUID() + "@academia.com", "senha1234"),
            new SignupRequest.CompanyData("Academia Fitness Ltda", "11222333000181", "11999998888", slug),
            new SignupRequest.PlanData(planId, PaymentMethod.PIX)
        );
        mockMvc.perform(post("/api/public/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated());

        var reqSameSlug = new SignupRequest(
            new SignupRequest.AdminData("Outro Admin", "outro" + UUID.randomUUID() + "@academia.com", "senha1234"),
            new SignupRequest.CompanyData("Outra Academia Ltda", "22333444000195", "11999998888", slug),
            new SignupRequest.PlanData(planId, PaymentMethod.PIX)
        );
        mockMvc.perform(post("/api/public/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(reqSameSlug)))
            .andExpect(status().isConflict());
    }
}
```

- [ ] **Step 12: Rodar para confirmar falha**

```bash
mvn test -pl . -Dtest=PublicSignupControllerTest -q 2>&1 | tail -20
```
Esperado: FAIL — `PublicSignupController` não existe (404) e/ou rotas não liberadas no `SecurityConfig`.

- [ ] **Step 13: Criar PublicSignupController**

```java
// backend/src/main/java/com/github/mwacha/wachafit/saas/PublicSignupController.java
package com.github.mwacha.wachafit.saas;

import com.github.mwacha.wachafit.auth.dto.LoginResponse;
import com.github.mwacha.wachafit.saas.dto.SignupRequest;
import com.github.mwacha.wachafit.tenant.TenantRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicSignupController {

    private final SignupService signupService;
    private final SaasPlanRepository saasPlanRepository;
    private final TenantRepository tenantRepository;

    public PublicSignupController(SignupService signupService, SaasPlanRepository saasPlanRepository,
                                   TenantRepository tenantRepository) {
        this.signupService = signupService;
        this.saasPlanRepository = saasPlanRepository;
        this.tenantRepository = tenantRepository;
    }

    @GetMapping("/saas-plans")
    public List<SaasPlan> listActivePlans() {
        return saasPlanRepository.findByActiveTrueOrderByPriceAsc();
    }

    @GetMapping("/check-slug")
    public Map<String, Boolean> checkSlug(@RequestParam String slug) {
        return Map.of("available", tenantRepository.findBySlug(slug).isEmpty());
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse signup(@Valid @RequestBody SignupRequest req) {
        return signupService.signup(req);
    }
}
```

Nota: `listActivePlans()` retorna a entidade `SaasPlan` diretamente (sem DTO) porque o público não autenticado só precisa dos mesmos campos já expostos — simplificação aceitável aqui; o CRUD administrativo (Task 10) continua usando `SaasPlanResponse`.

- [ ] **Step 14: Liberar as rotas públicas no SecurityConfig**

```java
// config/SecurityConfig.java — dentro de authorizeHttpRequests, adicionar antes de .anyRequest().authenticated():
.requestMatchers("/api/public/**").permitAll()
```

- [ ] **Step 15: Rodar os testes**

```bash
mvn test -pl . -Dtest=PublicSignupControllerTest -q 2>&1 | tail -20
```
Esperado: PASS (4 testes).

- [ ] **Step 16: Commit**

```bash
git add backend/src/main/java/com/github/mwacha/wachafit/saas/ \
        backend/src/main/java/com/github/mwacha/wachafit/tenant/TenantRepository.java \
        backend/src/main/java/com/github/mwacha/wachafit/config/SecurityConfig.java \
        backend/src/test/java/com/github/mwacha/wachafit/saas/
git commit -m "feat(saas): cadastro self-service de tenant — SignupService + endpoints públicos"
```

---

## Task 13: Frontend — wizard de cadastro de academia

**Files:**
- Create: `frontend/src/views/auth/SignupView.vue`
- Modify: `frontend/src/types/api.ts`
- Modify: `frontend/src/stores/auth.store.ts`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/utils/roleRoutes.ts`
- Modify: `frontend/src/views/auth/LoginView.vue`

**Interfaces:**
- Consumes: `GET /api/public/saas-plans`, `GET /api/public/check-slug`, `POST /api/public/signup` (Task 12), `LoginResponse` (já existente em `api.ts` após a Task 9)
- Produces: rota pública `/signup-academia`; `auth.signup(payload): Promise<LoginResponse>`

Este fluxo é **novo e adicional** ao `/register` já existente (Task 9) — `/register` continua sendo o autocadastro de um **aluno** dentro de uma academia já existente (via `tenantSlug`); `/signup-academia` cria uma **academia nova** (tenant + admin).

- [ ] **Step 1: Adicionar tipos em api.ts**

```typescript
// frontend/src/types/api.ts — adicionar ao final do arquivo:
export type PaymentMethod = 'CREDIT_CARD' | 'PIX' | 'BOLETO'

export interface SaasPlan {
  id: string
  name: string
  description: string
  price: number
  billingPeriodMonths: number
  maxUsers: number | null
  active: boolean
}

export interface SignupRequest {
  admin: { name: string; email: string; password: string }
  company: { name: string; cnpj: string; phone: string; slug: string }
  plan: { saasPlanId: string; paymentMethod: PaymentMethod }
}
```

- [ ] **Step 2: Adicionar a action signup ao auth.store.ts**

```typescript
// frontend/src/stores/auth.store.ts — adicionar import de SignupRequest:
import type { Role, LoginResponse, SignupRequest } from '@/types/api'

// e adicionar a função, próxima de `register`:
async function signup(payload: SignupRequest): Promise<LoginResponse> {
  const { data } = await api.post<LoginResponse>('/api/public/signup', payload)
  setSession(data)
  return data
}

// e incluir `signup` no objeto retornado pela store:
return { token, userId, role, tenantId, isAuthenticated, login, register, signup, logout, clearSession }
```

- [ ] **Step 3: Criar SignupView.vue**

```vue
<!-- frontend/src/views/auth/SignupView.vue -->
<template>
  <div class="auth-shell">
    <div class="brand-panel">
      <div class="brand-logo">
        <span class="brand-logo-w">W</span>
      </div>
      <div class="brand-copy">
        <h1 class="brand-title">Cadastre sua academia</h1>
        <p class="brand-sub">Leve o WachaFit para sua academia em poucos minutos.</p>
      </div>
      <div class="brand-dots" aria-hidden="true" />
    </div>

    <div class="form-panel">
      <div class="form-box">
        <p class="form-eyebrow">Passo {{ step }} de 3</p>
        <h2 class="form-title">{{ stepTitles[step - 1] }}</h2>

        <div v-if="step === 1" class="form-fields">
          <div class="field">
            <label class="field-label" for="adminName">Nome completo</label>
            <InputText id="adminName" v-model="admin.name" placeholder="Seu nome" autocomplete="name" />
          </div>
          <div class="field">
            <label class="field-label" for="adminEmail">E-mail</label>
            <InputText id="adminEmail" v-model="admin.email" type="email" placeholder="seu@email.com" autocomplete="email" />
          </div>
          <div class="field">
            <label class="field-label" for="adminPassword">Senha <span class="field-hint">(mín. 8 caracteres)</span></label>
            <Password id="adminPassword" v-model="admin.password" toggleMask :feedback="false" autocomplete="new-password" />
          </div>
          <div class="field">
            <label class="field-label" for="adminConfirm">Confirmar senha</label>
            <Password id="adminConfirm" v-model="adminConfirm" toggleMask :feedback="false"
                      :invalid="passwordMismatch" autocomplete="new-password" />
            <span v-if="passwordMismatch" class="field-error">Senhas não coincidem.</span>
          </div>
        </div>

        <div v-else-if="step === 2" class="form-fields">
          <div class="field">
            <label class="field-label" for="companyName">Razão social</label>
            <InputText id="companyName" v-model="company.name" placeholder="Academia Fitness Ltda" @input="suggestSlug" />
          </div>
          <div class="field">
            <label class="field-label" for="cnpj">CNPJ</label>
            <InputMask id="cnpj" v-model="company.cnpj" mask="99.999.999/9999-99" placeholder="00.000.000/0000-00" />
          </div>
          <div class="field">
            <label class="field-label" for="phone">Telefone</label>
            <InputMask id="phone" v-model="company.phone" mask="(99) 99999-9999" placeholder="(11) 99999-9999" />
          </div>
          <div class="field">
            <label class="field-label" for="slug">Slug da academia <span class="field-hint">(usado no login)</span></label>
            <InputText id="slug" v-model="company.slug" placeholder="minha-academia" @input="checkSlugAvailability" />
            <span v-if="slugStatus === 'taken'" class="field-error">Slug já em uso.</span>
            <span v-if="slugStatus === 'available'" class="field-success">Disponível!</span>
          </div>
        </div>

        <div v-else class="form-fields">
          <div class="plan-cards">
            <button
              v-for="p in plans" :key="p.id" type="button"
              class="plan-card" :class="{ selected: selectedPlanId === p.id }"
              @click="selectedPlanId = p.id"
            >
              <span class="plan-name">{{ p.name }}</span>
              <span class="plan-price">R$ {{ p.price.toFixed(2) }}/mês</span>
              <span class="plan-desc">{{ p.description }}</span>
            </button>
          </div>

          <div class="field">
            <label class="field-label">Forma de pagamento</label>
            <div class="payment-options">
              <label v-for="m in paymentMethods" :key="m.value" class="payment-option">
                <input type="radio" v-model="paymentMethod" :value="m.value" />
                {{ m.label }}
              </label>
            </div>
          </div>

          <div v-if="paymentMethod === 'CREDIT_CARD'" class="field">
            <label class="field-label" for="cardName">Nome no cartão</label>
            <InputText id="cardName" v-model="cardHolderName" placeholder="Como está impresso no cartão" />
          </div>
        </div>

        <div v-if="errorMessage" class="error-banner" role="alert">
          <i class="pi pi-exclamation-circle" />
          {{ errorMessage }}
        </div>

        <div class="wizard-actions">
          <Button v-if="step > 1" label="Voltar" severity="secondary" outlined @click="step--" />
          <Button v-if="step < 3" label="Próximo" class="submit-btn" @click="goNext" />
          <Button v-else label="Criar conta" :loading="loading" class="submit-btn" @click="handleSignup" />
        </div>

        <div class="form-footer">
          <span>Já tem conta? <RouterLink to="/login" class="link">Entrar</RouterLink></span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import { roleDashboards } from '@/utils/roleRoutes'
import api from '@/services/api'
import type { SaasPlan, PaymentMethod } from '@/types/api'
import InputText from 'primevue/inputtext'
import InputMask from 'primevue/inputmask'
import Password from 'primevue/password'
import Button from 'primevue/button'

const auth = useAuthStore()
const router = useRouter()

const step = ref(1)
const stepTitles = ['Crie sua conta', 'Dados da sua academia', 'Escolha o plano']

const admin = reactive({ name: '', email: '', password: '' })
const adminConfirm = ref('')
const company = reactive({ name: '', cnpj: '', phone: '', slug: '' })
const selectedPlanId = ref('')
const paymentMethod = ref<PaymentMethod>('PIX')
const cardHolderName = ref('')

const plans = ref<SaasPlan[]>([])
const slugStatus = ref<'idle' | 'checking' | 'available' | 'taken'>('idle')
const errorMessage = ref('')
const loading = ref(false)

const paymentMethods: { value: PaymentMethod; label: string }[] = [
  { value: 'CREDIT_CARD', label: 'Cartão de crédito' },
  { value: 'PIX', label: 'Pix' },
  { value: 'BOLETO', label: 'Boleto' },
]

const passwordMismatch = computed(() =>
  adminConfirm.value.length > 0 && admin.password !== adminConfirm.value
)

onMounted(async () => {
  const { data } = await api.get<SaasPlan[]>('/api/public/saas-plans')
  plans.value = data
  if (data.length > 0) selectedPlanId.value = data[0].id
})

function suggestSlug() {
  if (company.slug) return
  company.slug = company.name
    .toLowerCase()
    .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
}

let slugDebounce: ReturnType<typeof setTimeout> | undefined
function checkSlugAvailability() {
  slugStatus.value = 'checking'
  clearTimeout(slugDebounce)
  slugDebounce = setTimeout(async () => {
    if (!company.slug) { slugStatus.value = 'idle'; return }
    const { data } = await api.get<{ available: boolean }>('/api/public/check-slug', {
      params: { slug: company.slug },
    })
    slugStatus.value = data.available ? 'available' : 'taken'
  }, 400)
}

function goNext() {
  errorMessage.value = ''
  if (step.value === 1) {
    if (!admin.name || !admin.email || !admin.password) {
      errorMessage.value = 'Preencha todos os campos.'
      return
    }
    if (admin.password !== adminConfirm.value) {
      errorMessage.value = 'Senhas não coincidem.'
      return
    }
    if (admin.password.length < 8) {
      errorMessage.value = 'Senha deve ter ao menos 8 caracteres.'
      return
    }
  }
  if (step.value === 2) {
    if (!company.name || !company.cnpj || !company.phone || !company.slug) {
      errorMessage.value = 'Preencha todos os campos.'
      return
    }
    if (slugStatus.value === 'taken') {
      errorMessage.value = 'Escolha outro slug.'
      return
    }
  }
  step.value++
}

async function handleSignup() {
  errorMessage.value = ''
  if (!selectedPlanId.value) {
    errorMessage.value = 'Selecione um plano.'
    return
  }
  loading.value = true
  try {
    const result = await auth.signup({
      admin: { name: admin.name, email: admin.email, password: admin.password },
      company: {
        name: company.name,
        cnpj: company.cnpj.replace(/\D/g, ''),
        phone: company.phone.replace(/\D/g, ''),
        slug: company.slug,
      },
      plan: { saasPlanId: selectedPlanId.value, paymentMethod: paymentMethod.value },
    })
    router.push(roleDashboards[result.role])
  } catch (err: any) {
    errorMessage.value = err.response?.data?.message ?? 'Erro ao criar conta. Tente novamente.'
    step.value = 2
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-shell { display: flex; min-height: 100dvh; }
.brand-panel {
  display: none; flex-direction: column; justify-content: space-between;
  padding: 48px; background: var(--dark-surface); position: relative; overflow: hidden;
}
@media (min-width: 768px) { .brand-panel { display: flex; width: 44%; } }
.brand-logo {
  width: 48px; height: 48px; border-radius: 14px;
  background: linear-gradient(135deg, var(--blue-600), var(--blue-500));
  box-shadow: var(--shadow-logo); display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.brand-logo-w { font-family: var(--font-display); font-weight: 800; font-size: 22px; color: #fff; line-height: 1; }
.brand-copy { flex: 1; display: flex; flex-direction: column; justify-content: flex-end; padding-bottom: 40px; }
.brand-title { font-family: var(--font-display); font-size: 32px; font-weight: 700; color: #fff; line-height: 1.15; margin-bottom: 12px; }
.brand-sub { font-size: 15px; color: var(--neutral-500); line-height: 1.6; max-width: 280px; }
.brand-dots {
  position: absolute; inset: 0; pointer-events: none;
  background-image: radial-gradient(circle, rgba(255,255,255,0.06) 1px, transparent 1px);
  background-size: 22px 22px;
  mask-image: radial-gradient(ellipse at 60% 40%, black 30%, transparent 80%);
}
.form-panel { flex: 1; display: flex; align-items: center; justify-content: center; padding: 32px 24px; background: #fff; }
.form-box { width: 100%; max-width: 420px; }
.form-eyebrow {
  font-family: var(--font-mono); font-size: 11px; font-weight: 500; color: var(--blue-500);
  letter-spacing: 0.08em; text-transform: uppercase; margin-bottom: 8px;
}
.form-title { font-family: var(--font-display); font-size: 26px; font-weight: 600; color: var(--neutral-900); line-height: 1.25; margin-bottom: 28px; }
.form-fields { display: flex; flex-direction: column; gap: 16px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field-label { font-size: 13px; font-weight: 600; color: var(--neutral-800); }
.field-hint { font-weight: 400; color: var(--neutral-500); }
.field-error { font-size: 12px; color: var(--error-text); font-weight: 500; }
.field-success { font-size: 12px; color: #16a34a; font-weight: 500; }
.error-banner {
  display: flex; align-items: center; gap: 8px; background: var(--error-bg); border: 1px solid #FECACA;
  border-radius: var(--radius-md); color: var(--error-text); font-size: 13px; font-weight: 500; padding: 10px 14px;
}
.error-banner .pi { font-size: 14px; }
.wizard-actions { display: flex; gap: 12px; margin-top: 8px; }
.wizard-actions .submit-btn { flex: 1; width: 100% !important; justify-content: center; }
.plan-cards { display: flex; flex-direction: column; gap: 10px; margin-bottom: 8px; }
.plan-card {
  display: flex; flex-direction: column; gap: 2px; padding: 14px;
  border: 1px solid #ddd; border-radius: var(--radius-md); background: #fff; cursor: pointer; text-align: left;
}
.plan-card.selected { border-color: var(--blue-500); box-shadow: 0 0 0 2px var(--blue-500) inset; }
.plan-name { font-weight: 600; font-size: 14px; }
.plan-price { font-size: 13px; color: var(--blue-500); font-weight: 600; }
.plan-desc { font-size: 12px; color: var(--neutral-600); }
.payment-options { display: flex; gap: 16px; font-size: 13px; }
.payment-option { display: flex; align-items: center; gap: 6px; }
.form-footer { margin-top: 20px; font-size: 13px; color: var(--neutral-600); }
.link { color: var(--blue-500); text-decoration: none; font-weight: 500; }
.link:hover { color: var(--blue-700); }
</style>
```

- [ ] **Step 4: Adicionar a rota /signup-academia**

```typescript
// frontend/src/router/index.ts — adicionar junto às demais rotas de auth:
{ path: '/signup-academia', component: () => import('@/views/auth/SignupView.vue') },
```

- [ ] **Step 5: Adicionar a rota às públicas em roleRoutes.ts**

```typescript
// frontend/src/utils/roleRoutes.ts
export const publicAuthPaths = ['/login', '/register', '/signup-academia', '/forgot-password', '/reset-password']
```

- [ ] **Step 6: Adicionar link no LoginView.vue**

```vue
<!-- LoginView.vue — substituir o form-footer existente por: -->
<div class="form-footer">
  <RouterLink to="/forgot-password" class="link">Esqueci minha senha</RouterLink>
  <span class="sep">·</span>
  <span>Não tem conta? <RouterLink to="/register" class="link">Cadastre-se</RouterLink></span>
  <span class="sep">·</span>
  <span>É dono de uma academia? <RouterLink to="/signup-academia" class="link">Cadastre sua academia</RouterLink></span>
</div>
```

- [ ] **Step 7: Verificar que o build não tem erros de tipo**

```bash
cd frontend
npx vue-tsc --noEmit 2>&1 | head -20
```
Esperado: zero erros.

- [ ] **Step 8: Testar manualmente o fluxo completo**

1. Iniciar o backend: `cd backend && mvn spring-boot:run`
2. Iniciar o frontend: `cd frontend && npm run dev`
3. Abrir `http://localhost:5173/signup-academia`
4. Passo 1: preencher nome, e-mail, senha (8+ caracteres) → clicar "Próximo"
5. Passo 2: preencher razão social (verificar que o slug é sugerido automaticamente), CNPJ válido (ex: `11.222.333/0001-81`), telefone → clicar "Próximo"
6. Passo 3: escolher um plano, escolher forma de pagamento "Pix" → clicar "Criar conta"
7. Verificar redirecionamento para `/admin` (dashboard do ADMIN) já autenticado
8. Abrir DevTools → Application → localStorage → confirmar `tenantId` gravado
9. Repetir o cadastro com o mesmo slug → verificar mensagem de erro "Slug já em uso" no passo 2 (ou banner de erro após tentar submeter)

- [ ] **Step 9: Commit**

```bash
git add frontend/src/views/auth/SignupView.vue \
        frontend/src/types/api.ts \
        frontend/src/stores/auth.store.ts \
        frontend/src/router/index.ts \
        frontend/src/utils/roleRoutes.ts \
        frontend/src/views/auth/LoginView.vue
git commit -m "feat(saas): frontend — wizard de cadastro de academia (tenant) em 3 passos"
```

---

## Considerações finais

### O que NÃO está neste plano (escopo futuro)

1. **Bloqueio de acesso por inadimplência** — os status `PAST_DUE`/`CANCELED` da `TenantSubscription` existem no modelo, mas nenhuma rotina hoje verifica esse status para suspender o acesso do tenant (paralelo ao `OverdueJobService` que já existe para inadimplência de alunos). Fica para uma task futura.
2. **Portal de onboarding** — tela para o tenant admin convidar membros da equipe via email.
3. **Identificação de tenant via subdomínio** — `academia.wachafit.com` → resolve `tenantSlug` do subdomínio via nginx e envia como header, sem precisar digitar no login. Mudança de infraestrutura.
4. **Isolamento de uploads** — `progress_photos` guarda arquivos em disco; em multi-tenant produção, cada tenant deve ter seu próprio prefixo de path ou bucket S3 separado.
5. **Gateway de pagamento real** — o cadastro só registra a forma de pagamento escolhida; nenhuma cobrança é de fato processada (cartão não é tokenizado, Pix/Boleto não geram cobrança real). Requer integração futura com um gateway (ex: Stripe, Mercado Pago).

### Ordem de execução obrigatória

Tasks 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10 → 11 → 12 → 13 (todas sequenciais — sem paralelismo possível).
