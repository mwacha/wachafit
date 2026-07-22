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
- Todos os dados existentes migram para o tenant default: `slug = 'default'`, `name = 'Academia Padrão'`, `id = '00000000-0000-0000-0000-000000000001'`
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
./mvnw test -pl . -Dtest=TenantRepositoryTest -q 2>&1 | tail -20
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

-- Tenant padrão para dados existentes
INSERT INTO tenants (id, name, slug)
VALUES ('00000000-0000-0000-0000-000000000001', 'Academia Padrão', 'default');
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
./mvnw test -pl . -Dtest=TenantRepositoryTest -q 2>&1 | tail -10
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
./mvnw test -pl . -Dtest=JwtUtilTest -q 2>&1 | tail -10
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
./mvnw test -pl . -Dtest=AuthServiceTest -q 2>&1 | tail -15
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
./mvnw test -pl . -Dtest=AuthServiceTest,JwtUtilTest -q 2>&1 | tail -15
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
./mvnw test -pl . -Dtest=TenantContextTest -q 2>&1 | tail -5
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
./mvnw test -pl . -Dtest="TenantContextTest,TenantRepositoryTest,AuthServiceTest,JwtUtilTest" -q 2>&1 | tail -10
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
./mvnw flyway:migrate -q 2>&1 | tail -10
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
./mvnw compile -q 2>&1 | grep -i error
```
Esperado: zero erros de compilação.

- [ ] **Step 4: Rodar os testes de integração existentes**

```bash
./mvnw test -pl . -q 2>&1 | tail -20
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
./mvnw test -pl . -Dtest=ReportRepositoryTest -q 2>&1 | tail -5
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
./mvnw test -pl . -Dtest=ReportRepositoryTest -q 2>&1 | tail -5
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
./mvnw test -pl . -Dtest=TenantControllerTest -q 2>&1 | tail -10
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
./mvnw test -pl . -Dtest=TenantControllerTest -q 2>&1 | tail -10
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

1. Iniciar o backend: `cd backend && ./mvnw spring-boot:run`
2. Iniciar o frontend: `cd frontend && npm run dev`
3. Abrir `http://localhost:5173/login`
4. Preencher: slug = `default`, email e senha de um usuário existente
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

## Considerações finais

### O que NÃO está neste plano (escopo futuro)

1. **Assinatura SaaS da academia** — tabela `tenant_subscriptions` (plano pago, status, vencimento) com bloqueio de acesso quando inadimplente. Essa camada fica acima do tenant e controla se o tenant pode fazer login.
2. **Portal de onboarding** — tela para o tenant admin convidar membros da equipe via email.
3. **Identificação de tenant via subdomínio** — `academia.wachafit.com` → resolve `tenantSlug` do subdomínio via nginx e envia como header, sem precisar digitar no login. Mudança de infraestrutura.
4. **Isolamento de uploads** — `progress_photos` guarda arquivos em disco; em multi-tenant produção, cada tenant deve ter seu próprio prefixo de path ou bucket S3 separado.

### Ordem de execução obrigatória

Tasks 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 (todas sequenciais — sem paralelismo possível).
