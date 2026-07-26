# Conta Única com Seleção de Academia — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Substituir o login por `tenantSlug` por um login único (e-mail+senha) onde a pessoa escolhe entre as academias às quais está associada — via popup pós-login (quando há mais de uma) e via um seletor sempre visível no header.

**Architecture:** Nova entidade `Account` (identidade global de login: e-mail único, senha) separada de `User` (que passa a ser só "o vínculo desta pessoa com uma academia", com seu próprio `role` — nenhuma outra tabela do sistema muda, pois todas as referências existentes a `User.id` já significavam exatamente isso). Login resolve a conta e, se houver mais de um vínculo ativo, devolve um token temporário de seleção em vez do token completo; endpoints dedicados completam a seleção ou trocam de vínculo sem repetir a senha.

**Tech Stack:** Spring Boot 3.x, Hibernate 6, PostgreSQL 14+, Flyway, jjwt-api, Vue 3 + Pinia + PrimeVue (frontend).

## Global Constraints

- Spec completo: `docs/superpowers/specs/2026-07-25-account-tenant-membership-design.md`
- Flyway: próxima migration disponível é `V34`; nunca reusar números existentes
- Testes: não há wrapper `./mvnw` neste projeto — usar o `mvn` global (ex: `cd backend && mvn test -Dtest=Classe`)
- Colunas `created_at` (`nullable=false, insertable=false`, valor vindo do DEFAULT do banco): sempre incluir `columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP"` no `@Column` da entidade JPA — sem isso, o Hibernate `create-drop` usado pelos testes `@DataJpaTest`/`@SpringBootTest` com H2 puro (sem Testcontainers) cria a coluna sem DEFAULT e todo insert falha com `NULL not allowed`. Isso não afeta produção — lá `ddl-auto` é `validate` e o schema real vem do Flyway. NUNCA usar `TIMESTAMPTZ` no columnDefinition (sintaxe Postgres, H2 puro não entende).
- Testes `@SpringBootTest`/`@DataJpaTest` que rodam contra H2 puro (sem Testcontainers) precisam de `@ActiveProfiles("test")` explícito na classe — sem isso o perfil `test` (que define `mail.host`, datasource H2, etc.) não é ativado e o contexto Spring falha ao subir.
- Package base: `com.github.mwacha.wachafit`
- `LoginResponse` (`auth/dto/LoginResponse.java`) já é consumida hoje por `SignupService.java` (fora do escopo deste plano) construindo `new LoginResponse(token, role, userId, tenantId)` — 4 argumentos. Qualquer mudança nesse record PRECISA manter esse construtor de 4 argumentos funcionando sem tocar em `SignupService.java`.
- `RegisterView.vue` e `SignupView.vue` (fora do escopo deste plano) consomem `LoginResponse` do frontend (`frontend/src/types/api.ts`) com campos `token`/`role`/`userId`/`tenantId` sempre não-nulos. Não mudar o tipo `LoginResponse` existente — o novo formato ambíguo do `/login` usa um tipo adicional (`LoginResult` como union), não uma alteração do tipo já existente.
- Migração de dados: `accounts.email` é único **globalmente**. Se hoje já existem `User`s com o mesmo e-mail em tenants diferentes, a migration precisa DEDUPLICAR por e-mail (um `Account` por e-mail distinto, não um por `User`) — inserir dois `Account`s com o mesmo e-mail violaria a constraint e quebraria a migration inteira.

---

## Arquitetura de Arquivos

### Novos arquivos
```
backend/src/main/java/com/github/mwacha/wachafit/
  account/
    Account.java                          — Entity JPA (tabela accounts)
    AccountRepository.java                 — JpaRepository<Account, UUID>
  auth/dto/
    SelectTenantRequest.java
    SwitchTenantRequest.java
    TenantMembershipSummary.java

backend/src/main/resources/db/migration/
  V34__create_accounts.sql
  V35__add_account_id_to_users.sql
  V36__password_reset_tokens_use_account.sql

frontend/src/
  views/auth/components/TenantSelectModal.vue  — popup pós-login (Task 10)
```

### Arquivos modificados
```
backend:
  user/User.java                       — remove email/passwordHash; ganha account (ManyToOne)
  user/UserRepository.java             — remove métodos por email; ganha métodos por account_id
  user/UserService.java                — createUser() vincula a Account existente por e-mail
  saas/SignupService.java              — cria/vincula Account ao invés de User.setEmail/setPasswordHash (Task 3)
  auth/PasswordResetToken.java         — user (ManyToOne User) vira account (ManyToOne Account)
  auth/AuthService.java                — reescrito: login/selectTenant/switchTenant/myTenants/register/forgotPassword/resetPassword
  auth/AuthController.java             — novos endpoints select-tenant, switch-tenant, my-tenants
  auth/dto/LoginRequest.java           — remove tenantSlug
  auth/dto/LoginResponse.java          — ganha selectTenantToken/memberships (nulos no caso comum)
  shared/security/JwtUtil.java         — claim accountId; generateSelectTenantToken/isSelectTenantToken
  shared/security/JwtFilter.java       — extrai e valida accountId (fail-closed, mesmo padrão do tenantId)

frontend:
  src/types/api.ts                     — LoginRequest sem tenantSlug; LoginResponse ganha campos opcionais; novos tipos TenantMembershipSummary/LoginResult
  src/stores/auth.store.ts             — login() sem tenantSlug, retorna LoginResult; novos selectTenant()/switchTenant()/myTenants()
  src/views/auth/LoginView.vue         — remove campo de slug; trata resultado ambíguo do login
  src/components/AppLayout.vue         — seletor de academia sempre visível no header
```

---

## Task 1: Account entity + AccountRepository + migration V34

**Files:**
- Create: `backend/src/main/resources/db/migration/V34__create_accounts.sql`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/account/Account.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/account/AccountRepository.java`
- Test: `backend/src/test/java/com/github/mwacha/wachafit/account/AccountRepositoryTest.java`

**Interfaces:**
- Produces: `AccountRepository.findByEmail(String email): Optional<Account>` — usado nas Tasks 2, 5, 6

- [ ] **Step 1: Escrever o teste que falha**

```java
// backend/src/test/java/com/github/mwacha/wachafit/account/AccountRepositoryTest.java
package com.github.mwacha.wachafit.account;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AccountRepositoryTest {

    @Autowired AccountRepository repo;

    @Test
    void savesAndFindsByEmail() {
        Account a = new Account();
        a.setName("Maria Admin");
        a.setEmail("maria@teste.com");
        a.setPasswordHash("hash");
        repo.save(a);

        var found = repo.findByEmail("maria@teste.com");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Maria Admin");
        assertThat(found.get().isActive()).isTrue();
    }

    @Test
    void findByEmailReturnsEmpty_whenNotFound() {
        assertThat(repo.findByEmail("nao-existe@teste.com")).isEmpty();
    }
}
```

- [ ] **Step 2: Rodar o teste para confirmar que falha**

```bash
cd backend
mvn test -Dtest=AccountRepositoryTest -q 2>&1 | tail -20
```
Esperado: FAIL — `Account` não existe ainda.

- [ ] **Step 3: Criar a migration V34**

```sql
-- backend/src/main/resources/db/migration/V34__create_accounts.sql
CREATE TABLE accounts (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(160) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Um Account por e-mail DISTINTO já existente em users. Dedup necessário: accounts.email é
-- único globalmente, mas o mesmo e-mail pode hoje aparecer em mais de um User (tenants
-- diferentes). Nesse caso (raro), os dois Users passam a compartilhar a mesma conta/senha
-- após a Task 2 — ver Global Constraints do plano e a seção 1 do spec.
INSERT INTO accounts (name, email, password_hash)
SELECT DISTINCT ON (email) name, email, password_hash
FROM users
ORDER BY email, created_at ASC;
```

- [ ] **Step 4: Criar a entidade Account**

```java
// backend/src/main/java/com/github/mwacha/wachafit/account/Account.java
package com.github.mwacha.wachafit.account;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(unique = true, nullable = false, length = 160)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false,
        columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String v) { this.passwordHash = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }
    public Instant getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 5: Criar o AccountRepository**

```java
// backend/src/main/java/com/github/mwacha/wachafit/account/AccountRepository.java
package com.github.mwacha.wachafit.account;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByEmail(String email);
}
```

- [ ] **Step 6: Rodar o teste novamente**

```bash
mvn test -Dtest=AccountRepositoryTest -q 2>&1 | tail -10
```
Esperado: PASS (2 testes).

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/resources/db/migration/V34__create_accounts.sql \
        backend/src/main/java/com/github/mwacha/wachafit/account/ \
        backend/src/test/java/com/github/mwacha/wachafit/account/
git commit -m "feat(account): entidade Account + migration V34 (seed deduplicado a partir de users)"
```

---

## Task 2: User ganha account_id (perde email/passwordHash) + migration V35

**Files:**
- Create: `backend/src/main/resources/db/migration/V35__add_account_id_to_users.sql`
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/user/User.java`
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/user/UserRepository.java`
- Test: `backend/src/test/java/com/github/mwacha/wachafit/user/UserRepositoryTest.java`

**Interfaces:**
- Consumes: `Account` (Task 1)
- Produces:
  - `User.getAccount(): Account`, `User.setAccount(Account)`
  - `User.getEmail(): String` (agora delega para `account.getEmail()`, sem setter)
  - `UserRepository.findByAccountIdAndTenantId(UUID accountId, UUID tenantId): Optional<User>` — usado nas Tasks 5 e 6
  - `UserRepository.existsByAccountIdAndTenantId(UUID accountId, UUID tenantId): boolean` — usado na Task 7
  - `UserRepository.findByAccountIdAndActiveTrue(UUID accountId): List<User>` — usado na Task 6

- [ ] **Step 1: Escrever o teste que falha**

O `UserRepositoryTest` já existe neste projeto (usa Testcontainers real, não H2 puro — ver `backend/src/test/java/com/github/mwacha/wachafit/user/UserRepositoryTest.java`). Adicione este teste ao final da classe existente, dentro do `class UserRepositoryTest { ... }` (mantenha os testes já existentes intactos, apenas adicione):

```java
// backend/src/test/java/com/github/mwacha/wachafit/user/UserRepositoryTest.java — adicionar dentro da classe:
@Test
void findByAccountIdAndTenantId_returnsMembership() {
    var tenant = new com.github.mwacha.wachafit.tenant.Tenant();
    tenant.setName("Academia Teste");
    tenant.setSlug("academia-teste-" + java.util.UUID.randomUUID());
    tenant = tenantRepository.save(tenant);

    var account = new com.github.mwacha.wachafit.account.Account();
    account.setName("Pessoa Teste");
    account.setEmail("pessoa" + java.util.UUID.randomUUID() + "@teste.com");
    account.setPasswordHash("hash");
    account = accountRepository.save(account);

    User user = new User();
    user.setAccount(account);
    user.setRole(Role.STUDENT);
    user.setTenant(tenant);
    User saved = userRepository.save(user);

    var found = userRepository.findByAccountIdAndTenantId(account.getId(), tenant.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getId()).isEqualTo(saved.getId());
    assertThat(found.get().getEmail()).isEqualTo(account.getEmail());

    assertThat(userRepository.existsByAccountIdAndTenantId(account.getId(), tenant.getId())).isTrue();
    assertThat(userRepository.findByAccountIdAndActiveTrue(account.getId())).hasSize(1);
}
```

Adicione também os imports/autowires necessários no topo da classe, se ainda não existirem:

```java
// no topo da classe UserRepositoryTest, junto aos outros @Autowired:
@Autowired com.github.mwacha.wachafit.tenant.TenantRepository tenantRepository;
@Autowired com.github.mwacha.wachafit.account.AccountRepository accountRepository;
```

- [ ] **Step 2: Rodar o teste para confirmar que falha**

```bash
cd backend
mvn test -Dtest=UserRepositoryTest -q 2>&1 | tail -30
```
Esperado: FAIL — `User.setAccount`/`UserRepository.findByAccountIdAndTenantId` não existem ainda. (Este teste específico usa Testcontainers — requer Docker disponível no ambiente para rodar; se não houver Docker, confirme a falha via erro de compilação, que já demonstra que os métodos não existem.)

- [ ] **Step 3: Criar a migration V35**

```sql
-- backend/src/main/resources/db/migration/V35__add_account_id_to_users.sql

-- 1. Adicionar coluna account_id (nullable inicialmente para backfill)
ALTER TABLE users ADD COLUMN account_id UUID REFERENCES accounts(id);

-- 2. Backfill: liga cada User ao Account do mesmo e-mail (criado na V34)
UPDATE users u SET account_id = a.id
FROM accounts a
WHERE a.email = u.email;

-- 3. Tornar NOT NULL após o backfill
ALTER TABLE users ALTER COLUMN account_id SET NOT NULL;

-- 4. Remover name, email e password_hash de users (migraram para accounts).
--    Postgres remove automaticamente a constraint users_email_tenant_unique (V30) e
--    qualquer índice que dependa exclusivamente da coluna email ao dropá-la — não é
--    necessário (nem seguro, sem saber o nome exato gerado) fazer DROP CONSTRAINT antes.
--    DROP COLUMN name é obrigatório aqui: User.java (Step 4 abaixo) não mapeia mais
--    nenhum campo "name" (getName() delega para account.getName()), então o Hibernate
--    nunca inclui essa coluna no INSERT — a coluna antiga, ainda NOT NULL, quebraria
--    toda inserção de User se não for removida.
ALTER TABLE users DROP COLUMN name;
ALTER TABLE users DROP COLUMN email;
ALTER TABLE users DROP COLUMN password_hash;
```

- [ ] **Step 4: Atualizar User.java**

Substitua os campos `email`/`passwordHash` e seus getters/setters pelo relacionamento com `Account`, mantendo tudo o mais como está:

```java
// backend/src/main/java/com/github/mwacha/wachafit/user/User.java
package com.github.mwacha.wachafit.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.mwacha.wachafit.account.Account;
import com.github.mwacha.wachafit.tenant.Tenant;
import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false,
        columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

    @Override public String getUsername() { return account.getEmail(); }
    @Override @JsonIgnore public String getPassword() { return account.getPasswordHash(); }
    @Override public boolean isEnabled() { return active; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null) return List.of();
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public UUID getId() { return id; }
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    public String getName() { return account.getName(); }
    public String getEmail() { return account.getEmail(); }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
}
```

> **Nota:** `getName()` também passou a delegar para `account.getName()` — `User` não tem mais seu próprio `name` (o nome da pessoa é um atributo da conta, não do vínculo). Confirme, ao rodar a suíte completa no final deste plano, que nenhum lugar do código faz `user.setName(...)` diretamente (o campo `name` deixou de existir em `User`) — caso encontre, ajuste para setar em `Account` antes de associar. Isso será validado nas Tasks 5 e 6, que reescrevem os únicos pontos hoje conhecidos que criam `User`s (`AuthService`, `UserService`).

- [ ] **Step 5: Atualizar UserRepository**

```java
// backend/src/main/java/com/github/mwacha/wachafit/user/UserRepository.java
package com.github.mwacha.wachafit.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByAccountIdAndTenantId(UUID accountId, UUID tenantId);
    boolean existsByAccountIdAndTenantId(UUID accountId, UUID tenantId);
    List<User> findByAccountIdAndActiveTrue(UUID accountId);

    boolean existsByIdAndTenantId(UUID id, UUID tenantId);
    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);
}
```

> **Atenção:** os métodos `findByEmailAndTenantId`, `existsByEmailAndTenantId`, `findByEmail`, `existsByEmail` foram REMOVIDOS (não fazem mais sentido — `email` não existe mais em `User`). Isso vai quebrar a compilação de `AuthService.java`, `UserService.java`, `SignupService.java` e cerca de 15 arquivos de teste pré-existentes que constroem `User` diretamente — **isso é esperado nesta task**. A Task 3 (a seguir) corrige `SignupService.java` e os arquivos de teste; `AuthService`/`UserService` continuam quebrados até as Tasks 6 e 7. Não tente corrigir `AuthService`/`UserService` nesta task.

- [ ] **Step 6: Confirmar que o teste específico desta task compila e passa (ignorando o resto do projeto)**

```bash
mvn test-compile -q 2>&1 | tail -100
```
Esperado: vários erros de compilação, TODOS relacionados a `setEmail`/`setPasswordHash`/`setName`/`findByEmail`/`existsByEmail` removidos de `User`/`UserRepository` — isso é esperado, cobre `AuthService.java`, `UserService.java`, `SignupService.java`, e um número grande de arquivos de teste pré-existentes. A Task 3 (a seguir) corrige `SignupService.java` e os testes; `AuthService`/`UserService` continuam quebrados até as Tasks 6/7. Não tente corrigir nada disso nesta task — apenas confirme que o teste desta task específica (`UserRepositoryTest`) passa isoladamente com `mvn test -Dtest=UserRepositoryTest`.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/resources/db/migration/V35__add_account_id_to_users.sql \
        backend/src/main/java/com/github/mwacha/wachafit/user/User.java \
        backend/src/main/java/com/github/mwacha/wachafit/user/UserRepository.java \
        backend/src/test/java/com/github/mwacha/wachafit/user/UserRepositoryTest.java
git commit -m "feat(account): User.account_id substitui email/passwordHash (migration V35)"
```

---

## Task 3: Corrigir SignupService.java + ~15 testes que constroem User diretamente

> **Origem:** achado durante a implementação da Task 2. Remover `email`/`passwordHash`/`setName` de `User` e `findByEmail`/`existsByEmail` de `UserRepository` quebra a compilação de `SignupService.java` (produção, não previsto no arquivo de arquitetura original) e de ~15 arquivos de teste pré-existentes que constroem `new User()` diretamente com esses campos, em vez de passar pela API real.
>
> **Bug pré-existente encontrado junto:** nenhum desses ~15 testes chama `user.setTenant(...)` — e `User.tenant` é `NOT NULL` desde a migração multi-tenant já mergeada (`V30__add_tenant_to_users.sql`). Isso significa que esses testes já estão quebrados hoje, independente deste plano — como todos usam `@Testcontainers`/Postgres real, e não há Docker neste ambiente de desenvolvimento, ninguém percebeu (os erros aparecem misturados com os erros genéricos de "Docker indisponível"). Como cada um desses arquivos já vai ser editado nesta task para trocar `email`/`passwordHash` por `Account`, aproveitamos para também adicionar o `setTenant(...)` que faltava — mesmo esforço de edição, corrige os dois problemas de uma vez.

**Files:**
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/saas/SignupService.java`
- Modify: `backend/src/test/java/com/github/mwacha/wachafit/notification/ReminderSchedulerTest.java`
- Modify: `backend/src/test/java/com/github/mwacha/wachafit/groupclass/GroupClassServiceTest.java`
- Modify: `backend/src/test/java/com/github/mwacha/wachafit/goal/GoalServiceTest.java`
- Modify: `backend/src/test/java/com/github/mwacha/wachafit/schedule/ScheduleControllerIntegrationTest.java`
- Modify: `backend/src/test/java/com/github/mwacha/wachafit/groupclass/GroupClassControllerIntegrationTest.java`
- Modify: `backend/src/test/java/com/github/mwacha/wachafit/progress/ProgressControllerIntegrationTest.java`
- Modify: `backend/src/test/java/com/github/mwacha/wachafit/booking/BookingConcurrencyTest.java`
- Modify: `backend/src/test/java/com/github/mwacha/wachafit/assessment/AssessmentControllerIntegrationTest.java`
- Modify: `backend/src/test/java/com/github/mwacha/wachafit/profile/StudentProfileControllerIntegrationTest.java`
- Modify: `backend/src/test/java/com/github/mwacha/wachafit/exercise/ExerciseControllerIntegrationTest.java`
- Modify: `backend/src/test/java/com/github/mwacha/wachafit/report/ReportControllerIntegrationTest.java`
- Modify: `backend/src/test/java/com/github/mwacha/wachafit/workout/WorkoutControllerIntegrationTest.java`
- Modify: `backend/src/test/java/com/github/mwacha/wachafit/billing/BillingControllerIntegrationTest.java`
- Modify: `backend/src/test/java/com/github/mwacha/wachafit/membership/MembershipControllerIntegrationTest.java`
- Modify: `backend/src/test/java/com/github/mwacha/wachafit/membership/MembershipPlanControllerIntegrationTest.java`
- Modify: `backend/src/test/java/com/github/mwacha/wachafit/goal/GoalControllerIntegrationTest.java`
- Modify: `backend/src/test/java/com/github/mwacha/wachafit/user/UserRepositoryTest.java`

**Interfaces:**
- Consumes: `Account`/`AccountRepository` (Task 1), `User.setAccount()` (Task 2)

Nenhum destes é um "teste novo" no sentido de TDD (não há comportamento novo sendo introduzido) — é uma correção mecânica de compilação + o bug de tenant ausente. Não há um passo de "escrever teste que falha"; a "falha" já existe (erro de compilação, causado pela Task 2).

- [ ] **Step 1: Corrigir SignupService.java**

`SignupService` já tem `TenantRepository`/`PasswordEncoder` injetados. Adicione `AccountRepository` ao construtor e troque a construção direta do `User admin` por criar (ou reaproveitar, seguindo a mesma regra de "vincular a Account existente" usada no resto do plano) a `Account` primeiro:

```java
// backend/src/main/java/com/github/mwacha/wachafit/saas/SignupService.java
// Adicionar o import:
import com.github.mwacha.wachafit.account.Account;
import com.github.mwacha.wachafit.account.AccountRepository;

// Adicionar o campo e o parâmetro do construtor (junto aos já existentes tenantRepository/passwordEncoder/etc.):
private final AccountRepository accountRepository;

// No construtor, adicionar o parâmetro e a atribuição:
public SignupService(
    TenantRepository tenantRepository,
    UserRepository userRepository,
    AccountRepository accountRepository,
    SaasPlanRepository saasPlanRepository,
    TenantSubscriptionRepository subscriptionRepository,
    TenantChargeRepository chargeRepository,
    PasswordEncoder passwordEncoder,
    JwtUtil jwtUtil
) {
    this.tenantRepository = tenantRepository;
    this.userRepository = userRepository;
    this.accountRepository = accountRepository;
    this.saasPlanRepository = saasPlanRepository;
    this.subscriptionRepository = subscriptionRepository;
    this.chargeRepository = chargeRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtUtil = jwtUtil;
}
```

Substitua o bloco de criação do `User admin` (o que hoje faz `admin.setName(...)`/`admin.setEmail(...)`/`admin.setPasswordHash(...)`):

```java
// SignupService.java — dentro do método signup(), substituir:
User admin = new User();
admin.setName(req.admin().name());
admin.setEmail(req.admin().email());
admin.setPasswordHash(passwordEncoder.encode(req.admin().password()));
admin.setRole(Role.ADMIN);
admin.setTenant(tenant);
admin = userRepository.save(admin);

// por:
Account adminAccount = accountRepository.findByEmail(req.admin().email())
    .orElseGet(() -> {
        Account a = new Account();
        a.setName(req.admin().name());
        a.setEmail(req.admin().email());
        a.setPasswordHash(passwordEncoder.encode(req.admin().password()));
        return accountRepository.save(a);
    });
User admin = new User();
admin.setAccount(adminAccount);
admin.setRole(Role.ADMIN);
admin.setTenant(tenant);
admin = userRepository.save(admin);
```

Note que `PublicSignupControllerTest.java` (teste existente da Task 12 do plano multi-tenant, já mergeado) usa `@SpringBootTest` com H2 puro/Flyway desabilitado e cria seu próprio `SaasPlan` de teste — verifique, ao rodar a suíte no Step 8 deste plano, que ele continua passando; não deve precisar de nenhuma edição (não constrói `User` diretamente, só chama o endpoint `/api/public/signup`).

- [ ] **Step 2: Corrigir os 3 testes puramente Mockito (nunca persistem o User — não precisam de tenant)**

`ReminderSchedulerTest.java`, `GroupClassServiceTest.java` e `GoalServiceTest.java` usam `@Mock`/`@InjectMocks`, nunca gravam no banco de verdade — o `User` construído é só um objeto em memória devolvido por um mock. Não precisam de `Account` salva nem de `Tenant` — só precisam que `user.getEmail()` continue funcionando via um `Account` em memória (não persistido).

Em `ReminderSchedulerTest.java`, localize (dentro de `sendReminders_shouldSendEmail_forBookingsIn4hWindow()`):
```java
// ANTES:
User student = new User();
student.setName("Maria");
student.setEmail("maria@test.com");

User trainer = new User();
trainer.setName("João Personal");
```
Substitua por:
```java
// DEPOIS:
Account studentAccount = new Account();
studentAccount.setName("Maria");
studentAccount.setEmail("maria@test.com");
User student = new User();
student.setAccount(studentAccount);

Account trainerAccount = new Account();
trainerAccount.setName("João Personal");
User trainer = new User();
trainer.setAccount(trainerAccount);
```
Adicione o import `com.github.mwacha.wachafit.account.Account`.

Em `GroupClassServiceTest.java`, localize o helper `buildTrainer` (usado por praticamente todos os testes da classe):
```java
// ANTES:
private User buildTrainer(UUID id, String name, String email) {
    User u = new User();
    u.setName(name);
    u.setEmail(email);
    try {
        var f = User.class.getDeclaredField("id");
        f.setAccessible(true);
        f.set(u, id);
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
    return u;
}
```
Substitua por:
```java
// DEPOIS:
private User buildTrainer(UUID id, String name, String email) {
    Account account = new Account();
    account.setName(name);
    account.setEmail(email);
    User u = new User();
    u.setAccount(account);
    try {
        var f = User.class.getDeclaredField("id");
        f.setAccessible(true);
        f.set(u, id);
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
    return u;
}
```
Adicione o import `com.github.mwacha.wachafit.account.Account`.

Em `GoalServiceTest.java`, localize o `@BeforeEach setUp()`:
```java
// ANTES:
trainer = new User();
trainer.setName("Trainer");
trainer.setEmail("trainer@test.com");
trainer.setRole(Role.TRAINER);

student = new User();
student.setName("Student");
student.setEmail("student@test.com");
student.setRole(Role.STUDENT);
```
Substitua por:
```java
// DEPOIS:
Account trainerAccount = new Account();
trainerAccount.setName("Trainer");
trainerAccount.setEmail("trainer@test.com");
trainer = new User();
trainer.setAccount(trainerAccount);
trainer.setRole(Role.TRAINER);

Account studentAccount = new Account();
studentAccount.setName("Student");
studentAccount.setEmail("student@test.com");
student = new User();
student.setAccount(studentAccount);
student.setRole(Role.STUDENT);
```
Adicione o import `com.github.mwacha.wachafit.account.Account`. O `otherStudent` (em `list_asStudent_shouldOnlySeeOwnGoals_forbidden_whenOtherStudent()`) não usa `email`/`name` — não precisa de nenhuma mudança.

- [ ] **Step 3: Corrigir os 2 testes que usam o padrão "findByEmail após registro real"**

`ScheduleControllerIntegrationTest.java` e `GroupClassControllerIntegrationTest.java` registram um usuário via chamada HTTP real a `/api/auth/register` (que, após a Task 7 deste plano, já cria a `Account` corretamente), depois usam `userRepository.findByEmail(...)` para buscar o `User` criado e promovê-lo a `TRAINER`. Como `findByEmail` sai de `UserRepository`, troque para buscar a `Account` primeiro e depois o vínculo (`User`) daquela conta no tenant "personal-studio".

Ambos os arquivos precisam de dois novos `@Autowired` (adicione junto aos já existentes `MockMvc`/`ObjectMapper`/`UserRepository`):
```java
@Autowired com.github.mwacha.wachafit.account.AccountRepository accountRepository;
@Autowired com.github.mwacha.wachafit.tenant.TenantRepository tenantRepository;
```

Em `ScheduleControllerIntegrationTest.java`, dentro de `setup()`:
```java
// ANTES:
var trainerUser = userRepository.findByEmail(trainerEmail).orElseThrow();
trainerUser.setRole(Role.TRAINER);
userRepository.save(trainerUser);
trainerId = trainerUser.getId();
```
Substitua por:
```java
// DEPOIS:
var trainerTenant = tenantRepository.findBySlug("personal-studio").orElseThrow();
var trainerAccount = accountRepository.findByEmail(trainerEmail).orElseThrow();
var trainerUser = userRepository.findByAccountIdAndTenantId(trainerAccount.getId(), trainerTenant.getId()).orElseThrow();
trainerUser.setRole(Role.TRAINER);
userRepository.save(trainerUser);
trainerId = trainerUser.getId();
```

Em `GroupClassControllerIntegrationTest.java`, aplique a MESMA substituição nos dois lugares onde aparece (dentro de `setup()`, e dentro do `@Test create_withTrainerToken_shouldReturn201()`):
```java
// ANTES (aparece 2x no arquivo):
var trainerUser = userRepository.findByEmail(trainerEmail).orElseThrow();
trainerUser.setRole(Role.TRAINER);
userRepository.save(trainerUser);
```
```java
// DEPOIS (nas 2 ocorrências):
var trainerTenant = tenantRepository.findBySlug("personal-studio").orElseThrow();
var trainerAccount = accountRepository.findByEmail(trainerEmail).orElseThrow();
var trainerUser = userRepository.findByAccountIdAndTenantId(trainerAccount.getId(), trainerTenant.getId()).orElseThrow();
trainerUser.setRole(Role.TRAINER);
userRepository.save(trainerUser);
```

- [ ] **Step 4: Corrigir os 10 testes de integração que constroem User diretamente (precisam de Account + Tenant)**

Todos os arquivos abaixo seguem o mesmo padrão: `new User(); setName/setEmail/setPasswordHash/setRole/setActive; save()`, sem nunca chamar `setTenant(...)`. A correção, igual em todos: criar uma `Account` (salva) com `name`/`email`/`passwordHash`, e no `User` chamar `setAccount(account)` + `setTenant(tenant)` (o `tenant` resolvido uma vez via `tenantRepository.findBySlug("personal-studio").orElseThrow()`).

> **Atenção — limpeza entre testes:** cada um destes arquivos já tem um `userRepo.deleteAll()` (ou `userRepository.deleteAll()`) no `@BeforeEach`, para começar cada teste com a tabela `users` vazia. Adicione **logo depois** dessa linha (ordem FK-safe: `users` referencia `accounts`) um `accountRepository.deleteAll();`. Sem isso, como `accounts.email` é `UNIQUE` e cada teste recria a `Account` com o mesmo e-mail fixo (ex: `"t@t.com"`), o primeiro `@Test` da classe passa e todos os seguintes falham no `@BeforeEach` por violação de constraint única — só `userRepo.deleteAll()` limpa `users`, nunca `accounts`.

Cada um destes arquivos precisa de dois novos `@Autowired` (junto aos campos já existentes no topo da classe):
```java
@Autowired com.github.mwacha.wachafit.account.AccountRepository accountRepository;
@Autowired com.github.mwacha.wachafit.tenant.TenantRepository tenantRepository;
```

**`ProgressControllerIntegrationTest.java`** — dentro de `setUp()`:
```java
// ANTES:
User trainer = new User(); trainer.setName("T"); trainer.setEmail("t@t.com");
trainer.setPasswordHash(passwordEncoder.encode("pass")); trainer.setRole(Role.TRAINER); trainer.setActive(true);
userRepo.save(trainer);
User student = new User(); student.setName("S"); student.setEmail("s@t.com");
student.setPasswordHash(passwordEncoder.encode("pass")); student.setRole(Role.STUDENT); student.setActive(true);
userRepo.save(student);
```
```java
// DEPOIS:
var tenant = tenantRepository.findBySlug("personal-studio").orElseThrow();
Account trainerAccount = new Account();
trainerAccount.setName("T"); trainerAccount.setEmail("t@t.com");
trainerAccount.setPasswordHash(passwordEncoder.encode("pass"));
accountRepository.save(trainerAccount);
User trainer = new User();
trainer.setAccount(trainerAccount); trainer.setRole(Role.TRAINER);
trainer.setTenant(tenant); trainer.setActive(true);
userRepo.save(trainer);
Account studentAccount = new Account();
studentAccount.setName("S"); studentAccount.setEmail("s@t.com");
studentAccount.setPasswordHash(passwordEncoder.encode("pass"));
accountRepository.save(studentAccount);
User student = new User();
student.setAccount(studentAccount); student.setRole(Role.STUDENT);
student.setTenant(tenant); student.setActive(true);
userRepo.save(student);
```
Adicione o import `com.github.mwacha.wachafit.account.Account`.

**`AssessmentControllerIntegrationTest.java`** — dentro de `setUp()`, aplique o mesmo padrão de tradução (trainer com `"Trainer"`/`"trainer@test.com"`/`"pass123"`, student com `"Student"`/`"student@test.com"`/`"pass123"`), adicionando `var tenant = tenantRepository.findBySlug("personal-studio").orElseThrow();` antes das duas construções e `trainer.setTenant(tenant);`/`student.setTenant(tenant);` em cada uma, seguindo exatamente a mesma transformação do exemplo de `ProgressControllerIntegrationTest.java` acima (criar `Account`, salvar, `setAccount`, `setTenant`). Adicione o import `com.github.mwacha.wachafit.account.Account`.

**`StudentProfileControllerIntegrationTest.java`** — dentro de `setUp()`, mesmo padrão (admin `"Admin"`/`"admin@t.com"`/role `ADMIN`; student `"Student"`/`"student@t.com"`/role `STUDENT`, ambos senha `"pass"`). Adicione o import `com.github.mwacha.wachafit.account.Account`.

**`ExerciseControllerIntegrationTest.java`** — dentro de `setUp()`, mesmo padrão (apenas um trainer: `"T"`/`"t@t.com"`/senha `"pass"`/role `TRAINER`). Adicione o import `com.github.mwacha.wachafit.account.Account`.

**`WorkoutControllerIntegrationTest.java`** — dentro de `setUp()`, mesmo padrão (trainer `"T"`/`"t@t.com"` role `TRAINER`; student `"S"`/`"s@t.com"` role `STUDENT`, ambos senha `"pass"`). Adicione o import `com.github.mwacha.wachafit.account.Account`.

**`BillingControllerIntegrationTest.java`** — dentro de `setUp()`, mesmo padrão (admin `"Admin"`/`"admin@t.com"` role `ADMIN`; student `"Student"`/`"student@t.com"` role `STUDENT`, ambos senha `"pass"`). Adicione o import `com.github.mwacha.wachafit.account.Account`.

**`MembershipControllerIntegrationTest.java`** — dentro de `setUp()`, mesmo padrão (admin `"Admin"`/`"admin@t.com"` role `ADMIN`; student `"Student"`/`"student@t.com"` role `STUDENT`, ambos senha `"pass"`). Adicione o import `com.github.mwacha.wachafit.account.Account`.

**`MembershipPlanControllerIntegrationTest.java`** — dentro de `setUp()`, mesmo padrão (apenas admin: `"Admin"`/`"admin@t.com"`/senha `"pass"`/role `ADMIN`). Adicione o import `com.github.mwacha.wachafit.account.Account`.

**`GoalControllerIntegrationTest.java`** — dentro de `setUp()`, mesmo padrão (trainer `"T"`/`"t@t.com"` role `TRAINER`; student `"S"`/`"s@t.com"` role `STUDENT`, ambos senha `"pass"`). Adicione o import `com.github.mwacha.wachafit.account.Account`.

**`ReportControllerIntegrationTest.java`** — dentro de `setUp()`, MESMO padrão, mas com 3 usuários (admin `"Admin"`/`"admin@r.com"` role `ADMIN`; cashier `"Caixa"`/`"cashier@r.com"` role `CASHIER`; student `"Aluno"`/`"student@r.com"` role `STUDENT`, todos senha `"pass"`). Aplique a mesma transformação (Account + Tenant) às 3 construções. Adicione o import `com.github.mwacha.wachafit.account.Account`.

**`BookingConcurrencyTest.java`** — dentro do único `@Test rn03_onlyOneBookingSucceeds_whenTwoStudentsRaceForLastSlot()` (não há `@BeforeEach` neste arquivo), aplique o mesmo padrão às 3 construções (`trainer`, `s1`, `s2`), com `var tenant = tenantRepository.findBySlug("personal-studio").orElseThrow();` uma vez no início do método. Adicione os 2 `@Autowired` (`AccountRepository`, `TenantRepository`) e o import `com.github.mwacha.wachafit.account.Account`.

- [ ] **Step 5: Corrigir UserRepositoryTest.java — remover os 2 testes que cobriam funcionalidade removida**

`shouldSaveAndFindUserByEmail` e `existsByEmail_shouldReturnTrueIfExists` testam `userRepository.findByEmail`/`existsByEmail`, que não existem mais (a Task 1 já cobre o equivalente em `AccountRepositoryTest.savesAndFindsByEmail`/`findByEmailReturnsEmpty_whenNotFound`). Delete os dois métodos de teste inteiros de `UserRepositoryTest.java` (mantenha o `findByAccountIdAndTenantId_returnsMembership` adicionado na Task 2, e qualquer outro teste pré-existente do arquivo que não use `setEmail`/`setPasswordHash`/`findByEmail`/`existsByEmail`).

- [ ] **Step 6: Confirmar que a compilação de testes agora só falha em AuthService/UserService**

```bash
cd backend
mvn test-compile -q 2>&1 | tail -100
```
Esperado: os únicos erros restantes mencionam `AuthService.java` ou `UserService.java` (`setEmail`, `setPasswordHash`, `findByEmailAndTenantId`, `resetToken.setUser`, etc.) — nenhum outro arquivo deve aparecer. Se aparecer qualquer outro arquivo, ele ficou de fora desta correção e precisa ser adicionado antes de prosseguir.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/github/mwacha/wachafit/saas/SignupService.java \
        backend/src/test/java/com/github/mwacha/wachafit/notification/ReminderSchedulerTest.java \
        backend/src/test/java/com/github/mwacha/wachafit/groupclass/GroupClassServiceTest.java \
        backend/src/test/java/com/github/mwacha/wachafit/goal/GoalServiceTest.java \
        backend/src/test/java/com/github/mwacha/wachafit/schedule/ScheduleControllerIntegrationTest.java \
        backend/src/test/java/com/github/mwacha/wachafit/groupclass/GroupClassControllerIntegrationTest.java \
        backend/src/test/java/com/github/mwacha/wachafit/progress/ProgressControllerIntegrationTest.java \
        backend/src/test/java/com/github/mwacha/wachafit/booking/BookingConcurrencyTest.java \
        backend/src/test/java/com/github/mwacha/wachafit/assessment/AssessmentControllerIntegrationTest.java \
        backend/src/test/java/com/github/mwacha/wachafit/profile/StudentProfileControllerIntegrationTest.java \
        backend/src/test/java/com/github/mwacha/wachafit/exercise/ExerciseControllerIntegrationTest.java \
        backend/src/test/java/com/github/mwacha/wachafit/report/ReportControllerIntegrationTest.java \
        backend/src/test/java/com/github/mwacha/wachafit/workout/WorkoutControllerIntegrationTest.java \
        backend/src/test/java/com/github/mwacha/wachafit/billing/BillingControllerIntegrationTest.java \
        backend/src/test/java/com/github/mwacha/wachafit/membership/MembershipControllerIntegrationTest.java \
        backend/src/test/java/com/github/mwacha/wachafit/membership/MembershipPlanControllerIntegrationTest.java \
        backend/src/test/java/com/github/mwacha/wachafit/goal/GoalControllerIntegrationTest.java \
        backend/src/test/java/com/github/mwacha/wachafit/user/UserRepositoryTest.java
git commit -m "fix(account): SignupService + testes pre-existentes usam Account; corrige tenant ausente (bug pre-existente)"
```

---

## Task 4: PasswordResetToken passa a apontar para Account + migration V36

**Files:**
- Create: `backend/src/main/resources/db/migration/V36__password_reset_tokens_use_account.sql`
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/auth/PasswordResetToken.java`
- Test: `backend/src/test/java/com/github/mwacha/wachafit/auth/PasswordResetTokenRepositoryTest.java`

**Interfaces:**
- Consumes: `Account` (Task 1)
- Produces: `PasswordResetToken.getAccount(): Account`, `PasswordResetToken.setAccount(Account)`

- [ ] **Step 1: Escrever o teste que falha**

```java
// backend/src/test/java/com/github/mwacha/wachafit/auth/PasswordResetTokenRepositoryTest.java
package com.github.mwacha.wachafit.auth;

import com.github.mwacha.wachafit.account.Account;
import com.github.mwacha.wachafit.account.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PasswordResetTokenRepositoryTest {

    @Autowired PasswordResetTokenRepository repo;
    @Autowired AccountRepository accountRepository;

    @Test
    void savesAndFindsByToken() {
        Account account = new Account();
        account.setName("Pessoa Teste");
        account.setEmail("reset" + UUID.randomUUID() + "@teste.com");
        account.setPasswordHash("hash");
        account = accountRepository.save(account);

        PasswordResetToken t = new PasswordResetToken();
        t.setAccount(account);
        t.setToken(UUID.randomUUID().toString());
        t.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        repo.save(t);

        var found = repo.findByToken(t.getToken());
        assertThat(found).isPresent();
        assertThat(found.get().getAccount().getId()).isEqualTo(account.getId());
        assertThat(found.get().isUsed()).isFalse();
    }
}
```

- [ ] **Step 2: Rodar o teste para confirmar que falha**

```bash
cd backend
mvn test -Dtest=PasswordResetTokenRepositoryTest -q 2>&1 | tail -20
```
Esperado: FAIL — `PasswordResetToken.setAccount` não existe ainda.

- [ ] **Step 3: Criar a migration V36**

```sql
-- backend/src/main/resources/db/migration/V36__password_reset_tokens_use_account.sql

ALTER TABLE password_reset_tokens ADD COLUMN account_id UUID REFERENCES accounts(id);

UPDATE password_reset_tokens t SET account_id = u.account_id
FROM users u
WHERE t.user_id = u.id;

ALTER TABLE password_reset_tokens ALTER COLUMN account_id SET NOT NULL;
ALTER TABLE password_reset_tokens DROP COLUMN user_id;
```

- [ ] **Step 4: Atualizar PasswordResetToken.java**

```java
// backend/src/main/java/com/github/mwacha/wachafit/auth/PasswordResetToken.java
package com.github.mwacha.wachafit.auth;

import com.github.mwacha.wachafit.account.Account;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false, unique = true, length = 255)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    public UUID getId() { return id; }
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
}
```

- [ ] **Step 5: Rodar o teste novamente**

```bash
mvn test -Dtest=PasswordResetTokenRepositoryTest -q 2>&1 | tail -10
```
Esperado: PASS (1 teste). Compilação de `AuthService.java` continua quebrada nesta task (usa `resetToken.setUser(user)`/`resetToken.getUser()`) — será corrigida na Task 6.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/resources/db/migration/V36__password_reset_tokens_use_account.sql \
        backend/src/main/java/com/github/mwacha/wachafit/auth/PasswordResetToken.java \
        backend/src/test/java/com/github/mwacha/wachafit/auth/PasswordResetTokenRepositoryTest.java
git commit -m "feat(account): PasswordResetToken aponta para Account (migration V36)"
```

---

## Task 5: JwtUtil ganha claim accountId + JwtFilter valida (fail-closed)

**Files:**
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/shared/security/JwtUtil.java`
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/shared/security/JwtFilter.java`
- Test: `backend/src/test/java/com/github/mwacha/wachafit/shared/security/JwtUtilTest.java`
- Test: `backend/src/test/java/com/github/mwacha/wachafit/shared/security/JwtFilterTest.java`

**Interfaces:**
- Consumes: `User.getAccount()` (Task 2)
- Produces:
  - `JwtUtil.generateToken(User)` — agora inclui claim `"accountId"` além de `role`/`tenantId`
  - `JwtUtil.extractAccountId(String token): UUID`
  - `JwtUtil.generateSelectTenantToken(Account account): String` — token de curta duração (5 min), `subject = account.getId()`, claim `"purpose" = "select-tenant"`, sem `role`/`tenantId`/`accountId`
  - `JwtUtil.isSelectTenantToken(String token): boolean`
  - `JwtUtil.extractUserId(String token)` — reaproveitado também para ler o `accountId` de dentro de um `selectTenantToken` (o `subject` desse token é o id da conta, não de um vínculo — o método já genericamente faz parse do `subject` como UUID, então funciona igual para os dois casos)

- [ ] **Step 1: Escrever os testes que falham**

```java
// backend/src/test/java/com/github/mwacha/wachafit/shared/security/JwtUtilTest.java
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
    void setup() {
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
```

- [ ] **Step 2: Rodar para confirmar falha**

```bash
cd backend
mvn test -Dtest=JwtUtilTest -q 2>&1 | tail -20
```
Esperado: FAIL — `extractAccountId`/`generateSelectTenantToken`/`isSelectTenantToken` não existem.

- [ ] **Step 3: Atualizar JwtUtil**

```java
// backend/src/main/java/com/github/mwacha/wachafit/shared/security/JwtUtil.java
package com.github.mwacha.wachafit.shared.security;

import com.github.mwacha.wachafit.account.Account;
import com.github.mwacha.wachafit.user.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private static final long SELECT_TENANT_TOKEN_TTL_SECONDS = 5 * 60;

    private final SecretKey key;
    private final long expirationSeconds;

    public JwtUtil(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.expiration}") long expirationSeconds
    ) {
        if (secret.length() < 32) {
            throw new IllegalStateException("jwt.secret must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(User user) {
        return Jwts.builder()
            .subject(user.getId().toString())
            .claim("role", user.getRole().name())
            .claim("tenantId", user.getTenant().getId().toString())
            .claim("accountId", user.getAccount().getId().toString())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationSeconds * 1000))
            .signWith(key)
            .compact();
    }

    public String generateSelectTenantToken(Account account) {
        return Jwts.builder()
            .subject(account.getId().toString())
            .claim("purpose", "select-tenant")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + SELECT_TENANT_TOKEN_TTL_SECONDS * 1000))
            .signWith(key)
            .compact();
    }

    public boolean isSelectTenantToken(String token) {
        try {
            return "select-tenant".equals(parseClaims(token).get("purpose", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public UUID extractTenantId(String token) {
        String raw = parseClaims(token).get("tenantId", String.class);
        return raw != null ? UUID.fromString(raw) : null;
    }

    public UUID extractAccountId(String token) {
        String raw = parseClaims(token).get("accountId", String.class);
        return raw != null ? UUID.fromString(raw) : null;
    }

    public boolean isTokenValid(String token) {
        try {
            String subject = parseClaims(token).getSubject();
            UUID.fromString(subject); // validate subject is a UUID
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
```

- [ ] **Step 4: Rodar o teste novamente**

```bash
mvn test -Dtest=JwtUtilTest -q 2>&1 | tail -10
```
Esperado: PASS (3 testes).

- [ ] **Step 5: Escrever o teste do JwtFilter (fail-closed para accountId ausente)**

```java
// backend/src/test/java/com/github/mwacha/wachafit/shared/security/JwtFilterTest.java
package com.github.mwacha.wachafit.shared.security;

import com.github.mwacha.wachafit.account.Account;
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
    void validTokenWithAccountId_authenticates() throws Exception {
        User user = buildUser();
        String token = jwtUtil.generateToken(user);
        when(userDetailsService.loadUserByUsername(user.getId().toString())).thenReturn(user);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        jwtFilter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(userDetailsService).loadUserByUsername(user.getId().toString());
    }

    @Test
    void validTokenWithoutAccountId_doesNotAuthenticate() throws Exception {
        // Token assinado com a mesma chave, com tenantId mas SEM accountId -- simula um token
        // emitido antes desta migração (antes da claim accountId existir).
        String legacyToken = Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .claim("role", "ADMIN")
            .claim("tenantId", UUID.randomUUID().toString())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 3600_000))
            .signWith(key)
            .compact();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + legacyToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        jwtFilter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(userDetailsService);
        verify(chain).doFilter(request, response);
    }

    private User buildUser() throws Exception {
        Tenant tenant = new Tenant();
        setId(tenant, UUID.randomUUID());

        Account account = new Account();
        setId(account, UUID.randomUUID());

        User user = new User();
        setId(user, UUID.randomUUID());
        user.setRole(Role.ADMIN);
        user.setTenant(tenant);
        user.setAccount(account);
        user.setActive(true);
        return user;
    }

    private static void setId(Object entity, UUID id) throws Exception {
        Field f = entity.getClass().getDeclaredField("id");
        f.setAccessible(true);
        f.set(entity, id);
    }
}
```

> **Nota:** este arquivo `JwtFilterTest.java` já existe no projeto (criado numa correção de segurança anterior, que valida o mesmo tipo de fail-closed para `tenantId`). Se ele já existir com testes equivalentes para `tenantId`, ADICIONE os 2 testes acima como métodos novos na classe já existente, sem duplicar a estrutura (`buildUser`/`setId` já devem existir — reaproveite-os, apenas garanta que `buildUser()` seta `account` no `User`).

- [ ] **Step 6: Rodar para confirmar falha**

```bash
mvn test -Dtest=JwtFilterTest -q 2>&1 | tail -20
```
Esperado: FAIL no teste `validTokenWithoutAccountId_doesNotAuthenticate` — hoje o `JwtFilter` só rejeita ausência de `tenantId`, não de `accountId`.

- [ ] **Step 7: Atualizar JwtFilter — rejeitar também ausência de accountId**

```java
// backend/src/main/java/com/github/mwacha/wachafit/shared/security/JwtFilter.java
package com.github.mwacha.wachafit.shared.security;

import com.github.mwacha.wachafit.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public JwtFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null && jwtUtil.isTokenValid(token)) {
            try {
                UUID tenantId = jwtUtil.extractTenantId(token);
                UUID accountId = jwtUtil.extractAccountId(token);
                if (tenantId == null || accountId == null) {
                    // Token sem claim tenantId ou accountId: emitido antes de uma das migrações
                    // multi-tenant/conta-única. Não autenticar -- do contrário o
                    // TenantFilterAspect/fluxo de troca de academia não teriam o que precisam.
                    log.warn("JWT sem claim tenantId/accountId rejeitado (token de versão anterior)");
                } else {
                    String userId = jwtUtil.extractUserId(token).toString();
                    UserDetails user = userDetailsService.loadUserByUsername(userId);
                    if (user.isEnabled()) {
                        UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        TenantContext.set(tenantId);
                    }
                }
            } catch (Exception e) {
                // Token was valid but user lookup failed (deleted user, DB error, malformed claim).
                // Leave SecurityContext empty — Spring Security will return 401 via AuthenticationEntryPoint.
                log.debug("Could not authenticate from JWT token: {}", e.getMessage());
            }
        }
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
```

- [ ] **Step 8: Rodar os testes novamente**

```bash
mvn test -Dtest=JwtUtilTest,JwtFilterTest -q 2>&1 | tail -20
```
Esperado: PASS em todos.

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/com/github/mwacha/wachafit/shared/security/JwtUtil.java \
        backend/src/main/java/com/github/mwacha/wachafit/shared/security/JwtFilter.java \
        backend/src/test/java/com/github/mwacha/wachafit/shared/security/JwtUtilTest.java \
        backend/src/test/java/com/github/mwacha/wachafit/shared/security/JwtFilterTest.java
git commit -m "feat(account): JWT ganha claim accountId + token de selecao de tenant + fail-closed no JwtFilter"
```

---

## Task 6: Novos DTOs + AuthService reescrito + AuthController

**Files:**
- Create: `backend/src/main/java/com/github/mwacha/wachafit/auth/dto/SelectTenantRequest.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/auth/dto/SwitchTenantRequest.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/auth/dto/TenantMembershipSummary.java`
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/auth/dto/LoginRequest.java`
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/auth/dto/LoginResponse.java`
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/auth/AuthService.java`
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/auth/AuthController.java`
- Test: `backend/src/test/java/com/github/mwacha/wachafit/auth/AuthServiceTest.java`
- Modify (mecânico, ver Step 7): 14 arquivos de teste de integração pré-existentes que constroem `new LoginRequest(email, password, "personal-studio")` e precisam perder o 3º argumento — lista completa no Step 7.

**Interfaces:**
- Consumes: `AccountRepository` (Task 1), `User.getAccount()`/`UserRepository.findByAccountIdAndTenantId`/`findByAccountIdAndActiveTrue` (Task 2), `PasswordResetToken.getAccount()`/`setAccount()` (Task 4), `JwtUtil.generateSelectTenantToken`/`isSelectTenantToken`/`extractAccountId` (Task 5)
- Produces:
  - `POST /api/auth/login` — sem `tenantSlug`; devolve token completo OU `{selectTenantToken, memberships}`
  - `POST /api/auth/select-tenant`, `POST /api/auth/switch-tenant`, `GET /api/auth/my-tenants`
  - `LoginResponse(String token, String role, String userId, String tenantId)` — construtor de 4 argumentos preservado (usado por `SignupService.java`, fora de escopo)

- [ ] **Step 1: Escrever o teste que falha**

Substitua o conteúdo de `AuthServiceTest.java` inteiro (o arquivo atual testa o fluxo antigo com `tenantSlug`, que deixa de existir):

```java
// backend/src/test/java/com/github/mwacha/wachafit/auth/AuthServiceTest.java
package com.github.mwacha.wachafit.auth;

import com.github.mwacha.wachafit.account.Account;
import com.github.mwacha.wachafit.account.AccountRepository;
import com.github.mwacha.wachafit.auth.dto.*;
import com.github.mwacha.wachafit.notification.EmailService;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
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

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepo;
    private AccountRepository accountRepo;
    private PasswordResetTokenRepository tokenRepo;
    private TenantRepository tenantRepo;
    private JwtUtil jwtUtil;
    private AuthService authService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setup() {
        userRepo = mock(UserRepository.class);
        accountRepo = mock(AccountRepository.class);
        tokenRepo = mock(PasswordResetTokenRepository.class);
        tenantRepo = mock(TenantRepository.class);
        jwtUtil = new JwtUtil("super-secret-key-with-at-least-32-chars!!", 3600L);
        authService = new AuthService(
            userRepo, accountRepo, tokenRepo, tenantRepo, jwtUtil, encoder,
            mock(EmailService.class), "http://localhost:5173"
        );
    }

    private Account buildAccount(String email, String rawPassword) throws Exception {
        Account a = new Account();
        a.setEmail(email);
        a.setPasswordHash(encoder.encode(rawPassword));
        a.setActive(true);
        setId(a, UUID.randomUUID());
        return a;
    }

    private User buildMembership(Account account, Tenant tenant, Role role) throws Exception {
        User u = new User();
        u.setAccount(account);
        u.setTenant(tenant);
        u.setRole(role);
        u.setActive(true);
        setId(u, UUID.randomUUID());
        return u;
    }

    private Tenant buildTenant(String name, String slug) throws Exception {
        Tenant t = new Tenant();
        t.setName(name);
        t.setSlug(slug);
        setId(t, UUID.randomUUID());
        return t;
    }

    @Test
    void login_returnsFullToken_whenSingleMembership() throws Exception {
        Account account = buildAccount("admin@teste.com", "senha123");
        Tenant tenant = buildTenant("Academia Teste", "academia-teste");
        User membership = buildMembership(account, tenant, Role.ADMIN);

        when(accountRepo.findByEmail("admin@teste.com")).thenReturn(Optional.of(account));
        when(userRepo.findByAccountIdAndActiveTrue(account.getId())).thenReturn(List.of(membership));

        LoginResponse resp = authService.login(new LoginRequest("admin@teste.com", "senha123"));

        assertThat(resp.token()).isNotBlank();
        assertThat(resp.role()).isEqualTo("ADMIN");
        assertThat(resp.tenantId()).isEqualTo(tenant.getId().toString());
        assertThat(resp.selectTenantToken()).isNull();
        assertThat(resp.memberships()).isNull();
    }

    @Test
    void login_returnsSelectTenantToken_whenMultipleMemberships() throws Exception {
        Account account = buildAccount("multi@teste.com", "senha123");
        Tenant tenantA = buildTenant("Academia A", "academia-a");
        Tenant tenantB = buildTenant("Academia B", "academia-b");
        User membershipA = buildMembership(account, tenantA, Role.TRAINER);
        User membershipB = buildMembership(account, tenantB, Role.ADMIN);

        when(accountRepo.findByEmail("multi@teste.com")).thenReturn(Optional.of(account));
        when(userRepo.findByAccountIdAndActiveTrue(account.getId()))
            .thenReturn(List.of(membershipA, membershipB));

        LoginResponse resp = authService.login(new LoginRequest("multi@teste.com", "senha123"));

        assertThat(resp.token()).isNull();
        assertThat(resp.selectTenantToken()).isNotBlank();
        assertThat(resp.memberships()).hasSize(2);
        assertThat(resp.memberships().stream().map(TenantMembershipSummary::tenantSlug))
            .containsExactlyInAnyOrder("academia-a", "academia-b");
    }

    @Test
    void login_throwsUnauthorized_whenPasswordWrong() throws Exception {
        Account account = buildAccount("admin@teste.com", "senha123");
        when(accountRepo.findByEmail("admin@teste.com")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin@teste.com", "errada")))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_throwsUnauthorized_whenAccountNotFound() {
        when(accountRepo.findByEmail("ghost@teste.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@teste.com", "pass")))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_throwsUnauthorized_whenNoActiveMemberships() throws Exception {
        Account account = buildAccount("semvinculo@teste.com", "senha123");
        when(accountRepo.findByEmail("semvinculo@teste.com")).thenReturn(Optional.of(account));
        when(userRepo.findByAccountIdAndActiveTrue(account.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> authService.login(new LoginRequest("semvinculo@teste.com", "senha123")))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void selectTenant_issuesFullToken_whenTenantBelongsToAccount() throws Exception {
        Account account = buildAccount("multi@teste.com", "senha123");
        Tenant tenant = buildTenant("Academia A", "academia-a");
        User membership = buildMembership(account, tenant, Role.TRAINER);
        String selectToken = jwtUtil.generateSelectTenantToken(account);

        when(userRepo.findByAccountIdAndTenantId(account.getId(), tenant.getId()))
            .thenReturn(Optional.of(membership));

        LoginResponse resp = authService.selectTenant(
            new SelectTenantRequest(selectToken, tenant.getId().toString()));

        assertThat(resp.token()).isNotBlank();
        assertThat(resp.role()).isEqualTo("TRAINER");
    }

    @Test
    void selectTenant_throwsUnauthorized_whenTokenIsNotSelectTenantPurpose() throws Exception {
        Account account = buildAccount("x@teste.com", "senha123");
        Tenant tenant = buildTenant("Academia A", "academia-a");
        User membership = buildMembership(account, tenant, Role.TRAINER);
        String normalToken = jwtUtil.generateToken(membership);

        assertThatThrownBy(() -> authService.selectTenant(
            new SelectTenantRequest(normalToken, tenant.getId().toString())))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void switchTenant_issuesTokenForOtherMembership() throws Exception {
        Account account = buildAccount("multi@teste.com", "senha123");
        Tenant currentTenant = buildTenant("Academia A", "academia-a");
        Tenant otherTenant = buildTenant("Academia B", "academia-b");
        User currentMembership = buildMembership(account, currentTenant, Role.TRAINER);
        User otherMembership = buildMembership(account, otherTenant, Role.ADMIN);

        when(userRepo.findByAccountIdAndTenantId(account.getId(), otherTenant.getId()))
            .thenReturn(Optional.of(otherMembership));

        LoginResponse resp = authService.switchTenant(
            new SwitchTenantRequest(otherTenant.getId().toString()), currentMembership);

        assertThat(resp.role()).isEqualTo("ADMIN");
        assertThat(resp.tenantId()).isEqualTo(otherTenant.getId().toString());
    }

    @Test
    void myTenants_listsAllActiveMemberships() throws Exception {
        Account account = buildAccount("multi@teste.com", "senha123");
        Tenant tenantA = buildTenant("Academia A", "academia-a");
        User membershipA = buildMembership(account, tenantA, Role.TRAINER);

        when(userRepo.findByAccountIdAndActiveTrue(account.getId())).thenReturn(List.of(membershipA));

        List<TenantMembershipSummary> result = authService.myTenants(membershipA);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).tenantSlug()).isEqualTo("academia-a");
    }

    private static void setId(Object entity, UUID id) throws Exception {
        Field f = entity.getClass().getDeclaredField("id");
        f.setAccessible(true);
        f.set(entity, id);
    }
}
```

- [ ] **Step 2: Rodar para confirmar falha**

```bash
cd backend
mvn test -Dtest=AuthServiceTest -q 2>&1 | tail -30
```
Esperado: FAIL (não compila ainda — `AuthService` não tem esses métodos/construtor).

- [ ] **Step 3: Criar os DTOs novos**

```java
// backend/src/main/java/com/github/mwacha/wachafit/auth/dto/SelectTenantRequest.java
package com.github.mwacha.wachafit.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SelectTenantRequest(
    @NotBlank String selectTenantToken,
    @NotBlank String tenantId
) {}
```

```java
// backend/src/main/java/com/github/mwacha/wachafit/auth/dto/SwitchTenantRequest.java
package com.github.mwacha.wachafit.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SwitchTenantRequest(
    @NotBlank String tenantId
) {}
```

```java
// backend/src/main/java/com/github/mwacha/wachafit/auth/dto/TenantMembershipSummary.java
package com.github.mwacha.wachafit.auth.dto;

public record TenantMembershipSummary(
    String tenantId,
    String tenantName,
    String tenantSlug,
    String role
) {}
```

- [ ] **Step 4: Atualizar LoginRequest e LoginResponse**

```java
// backend/src/main/java/com/github/mwacha/wachafit/auth/dto/LoginRequest.java
package com.github.mwacha.wachafit.auth.dto;

import jakarta.validation.constraints.*;

public record LoginRequest(
    @Email @NotBlank String email,
    @NotBlank String password
) {}
```

```java
// backend/src/main/java/com/github/mwacha/wachafit/auth/dto/LoginResponse.java
package com.github.mwacha.wachafit.auth.dto;

import java.util.List;

public record LoginResponse(
    String token,
    String role,
    String userId,
    String tenantId,
    String selectTenantToken,
    List<TenantMembershipSummary> memberships
) {
    // Construtor de 4 argumentos preservado: SignupService.java (fora de escopo) constrói
    // LoginResponse assim e não deve precisar de nenhuma alteração.
    public LoginResponse(String token, String role, String userId, String tenantId) {
        this(token, role, userId, tenantId, null, null);
    }
}
```

- [ ] **Step 5: Reescrever AuthService**

```java
// backend/src/main/java/com/github/mwacha/wachafit/auth/AuthService.java
package com.github.mwacha.wachafit.auth;

import com.github.mwacha.wachafit.account.Account;
import com.github.mwacha.wachafit.account.AccountRepository;
import com.github.mwacha.wachafit.auth.dto.*;
import com.github.mwacha.wachafit.notification.EmailService;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.shared.exception.UnauthorizedException;
import com.github.mwacha.wachafit.shared.security.JwtUtil;
import com.github.mwacha.wachafit.tenant.Tenant;
import com.github.mwacha.wachafit.tenant.TenantRepository;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final TenantRepository tenantRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final String frontendUrl;

    public AuthService(
        UserRepository userRepository,
        AccountRepository accountRepository,
        PasswordResetTokenRepository tokenRepository,
        TenantRepository tenantRepository,
        JwtUtil jwtUtil,
        PasswordEncoder passwordEncoder,
        EmailService emailService,
        @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.tokenRepository = tokenRepository;
        this.tenantRepository = tenantRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    public LoginResponse register(RegisterRequest request) {
        Tenant tenant = tenantRepository.findBySlug(request.tenantSlug())
            .filter(Tenant::isActive)
            .orElseThrow(() -> new UnauthorizedException("Academia não encontrada"));

        Account account = accountRepository.findByEmail(request.email())
            .orElseGet(() -> createAccount(request.name(), request.email(), request.password()));

        if (userRepository.existsByAccountIdAndTenantId(account.getId(), tenant.getId())) {
            throw new BusinessException("E-mail já cadastrado nesta academia");
        }

        User user = new User();
        user.setAccount(account);
        user.setRole(Role.STUDENT);
        user.setTenant(tenant);
        User saved = userRepository.save(user);
        emailService.sendHtml(
            account.getEmail(),
            "Bem-vindo ao WachaFit!",
            "email/welcome",
            Map.of("name", account.getName())
        );
        return issueFullLogin(saved);
    }

    public LoginResponse login(LoginRequest request) {
        Account account = accountRepository.findByEmail(request.email())
            .filter(Account::isActive)
            .orElseThrow(() -> new UnauthorizedException("Credenciais inválidas"));
        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new UnauthorizedException("Credenciais inválidas");
        }

        List<User> memberships = userRepository.findByAccountIdAndActiveTrue(account.getId());
        if (memberships.isEmpty()) {
            throw new UnauthorizedException("Credenciais inválidas");
        }
        if (memberships.size() == 1) {
            return issueFullLogin(memberships.get(0));
        }

        String selectToken = jwtUtil.generateSelectTenantToken(account);
        List<TenantMembershipSummary> summaries = memberships.stream()
            .map(this::toSummary)
            .toList();
        return new LoginResponse(null, null, null, null, selectToken, summaries);
    }

    public LoginResponse selectTenant(SelectTenantRequest request) {
        if (!jwtUtil.isSelectTenantToken(request.selectTenantToken())) {
            throw new UnauthorizedException("Token inválido");
        }
        UUID accountId = jwtUtil.extractUserId(request.selectTenantToken());
        UUID tenantId = UUID.fromString(request.tenantId());
        User membership = userRepository.findByAccountIdAndTenantId(accountId, tenantId)
            .filter(User::isActive)
            .orElseThrow(() -> new UnauthorizedException("Academia inválida para esta conta"));
        return issueFullLogin(membership);
    }

    public LoginResponse switchTenant(SwitchTenantRequest request, User currentUser) {
        UUID accountId = currentUser.getAccount().getId();
        UUID tenantId = UUID.fromString(request.tenantId());
        User membership = userRepository.findByAccountIdAndTenantId(accountId, tenantId)
            .filter(User::isActive)
            .orElseThrow(() -> new UnauthorizedException("Academia inválida para esta conta"));
        return issueFullLogin(membership);
    }

    @Transactional(readOnly = true)
    public List<TenantMembershipSummary> myTenants(User currentUser) {
        return userRepository.findByAccountIdAndActiveTrue(currentUser.getAccount().getId()).stream()
            .map(this::toSummary)
            .toList();
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        accountRepository.findByEmail(request.email()).ifPresent(account -> {
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setAccount(account);
            resetToken.setToken(UUID.randomUUID().toString());
            resetToken.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
            tokenRepository.save(resetToken);
            String resetLink = frontendUrl + "/reset-password?token=" + resetToken.getToken();
            emailService.sendHtml(
                account.getEmail(),
                "Redefinição de senha — WachaFit",
                "email/password-reset",
                Map.of("name", account.getName(), "resetLink", resetLink)
            );
        });
    }

    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = tokenRepository.findByToken(request.token())
            .orElseThrow(() -> new BusinessException("Token inválido"));
        if (resetToken.isUsed()) {
            throw new BusinessException("Token já utilizado");
        }
        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Token expirado");
        }
        Account account = resetToken.getAccount();
        account.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        resetToken.setUsed(true);
        // Both entities are managed within this transaction; Hibernate dirty-checks flush at commit.
    }

    private Account createAccount(String name, String email, String rawPassword) {
        Account a = new Account();
        a.setName(name);
        a.setEmail(email);
        a.setPasswordHash(passwordEncoder.encode(rawPassword));
        return accountRepository.save(a);
    }

    private LoginResponse issueFullLogin(User membership) {
        String token = jwtUtil.generateToken(membership);
        return new LoginResponse(token, membership.getRole().name(), membership.getId().toString(),
            membership.getTenant().getId().toString());
    }

    private TenantMembershipSummary toSummary(User membership) {
        Tenant tenant = membership.getTenant();
        return new TenantMembershipSummary(
            tenant.getId().toString(), tenant.getName(), tenant.getSlug(), membership.getRole().name());
    }
}
```

- [ ] **Step 6: Atualizar AuthController**

```java
// backend/src/main/java/com/github/mwacha/wachafit/auth/AuthController.java
package com.github.mwacha.wachafit.auth;

import com.github.mwacha.wachafit.auth.dto.*;
import com.github.mwacha.wachafit.user.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/select-tenant")
    public ResponseEntity<LoginResponse> selectTenant(@Valid @RequestBody SelectTenantRequest request) {
        return ResponseEntity.ok(authService.selectTenant(request));
    }

    @PostMapping("/switch-tenant")
    public ResponseEntity<LoginResponse> switchTenant(
        @Valid @RequestBody SwitchTenantRequest request,
        @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(authService.switchTenant(request, currentUser));
    }

    @GetMapping("/my-tenants")
    public ResponseEntity<List<TenantMembershipSummary>> myTenants(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(authService.myTenants(currentUser));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 7: Corrigir os outros 14 arquivos de teste que constroem `LoginRequest` com o 3º argumento antigo**

`LoginRequest` deixou de ter `tenantSlug` (Step 4). Os arquivos abaixo (todos testes de integração pré-existentes, fora do escopo funcional desta task, mas que quebram de compilar por causa da mudança de assinatura) constroem `new LoginRequest(email, password, "personal-studio")` — remova o terceiro argumento (a string do slug) de cada chamada, mantendo os dois primeiros exatamente como estão. É uma edição puramente mecânica: `new LoginRequest(x, y, "personal-studio")` vira `new LoginRequest(x, y)` em cada uma das ocorrências abaixo.

- `backend/src/test/java/com/github/mwacha/wachafit/progress/ProgressControllerIntegrationTest.java:68`
- `backend/src/test/java/com/github/mwacha/wachafit/schedule/ScheduleControllerIntegrationTest.java:78,93`
- `backend/src/test/java/com/github/mwacha/wachafit/user/UserControllerIntegrationTest.java:63`
- `backend/src/test/java/com/github/mwacha/wachafit/auth/AuthControllerIntegrationTest.java:71,87`
- `backend/src/test/java/com/github/mwacha/wachafit/assessment/AssessmentControllerIntegrationTest.java:89`
- `backend/src/test/java/com/github/mwacha/wachafit/groupclass/GroupClassControllerIntegrationTest.java:86,132`
- `backend/src/test/java/com/github/mwacha/wachafit/profile/StudentProfileControllerIntegrationTest.java:69`
- `backend/src/test/java/com/github/mwacha/wachafit/report/ReportControllerIntegrationTest.java:119`
- `backend/src/test/java/com/github/mwacha/wachafit/exercise/ExerciseControllerIntegrationTest.java:70`
- `backend/src/test/java/com/github/mwacha/wachafit/workout/WorkoutControllerIntegrationTest.java:104,110`
- `backend/src/test/java/com/github/mwacha/wachafit/billing/BillingControllerIntegrationTest.java:95`
- `backend/src/test/java/com/github/mwacha/wachafit/membership/MembershipControllerIntegrationTest.java:86,153`
- `backend/src/test/java/com/github/mwacha/wachafit/membership/MembershipPlanControllerIntegrationTest.java:69`
- `backend/src/test/java/com/github/mwacha/wachafit/goal/GoalControllerIntegrationTest.java:84,90`

Exemplo da edição em `AuthControllerIntegrationTest.java:71`:
```java
// antes:
new LoginRequest("bob@test.com", "password123", "personal-studio"))))
// depois:
new LoginRequest("bob@test.com", "password123"))))
```

`AuthControllerIntegrationTest.java` também constrói `new RegisterRequest(...)` com 4 argumentos incluindo `"personal-studio"` (ex: linha 53, 65, 81) — **isso continua igual, não mexa** (`RegisterRequest` mantém `tenantSlug`, ver Global Constraints).

> Estes 14 arquivos usam `@Testcontainers`/Postgres real — não é possível rodá-los sem Docker neste ambiente. A verificação aqui é `mvn test-compile`, não a execução real dos testes (Step 8 confirma a compilação).

- [ ] **Step 8: Confirmar que o projeto compila (incluindo os testes) com as correções acima**

```bash
cd backend
mvn test-compile -q 2>&1 | tail -60
```
Esperado: zero erros relacionados a `LoginRequest`. (Erros relacionados a `UserService`/`setEmail`/`setPasswordHash` ainda são esperados até a Task 7 — ver nota da Task 2, Step 6.)

- [ ] **Step 9: Rodar os testes**

```bash
mvn test -Dtest=AuthServiceTest -q 2>&1 | tail -30
```
Esperado: PASS (9 testes).

- [ ] **Step 10: Commit**

```bash
git add backend/src/main/java/com/github/mwacha/wachafit/auth/ \
        backend/src/test/java/com/github/mwacha/wachafit/progress/ProgressControllerIntegrationTest.java \
        backend/src/test/java/com/github/mwacha/wachafit/schedule/ScheduleControllerIntegrationTest.java \
        backend/src/test/java/com/github/mwacha/wachafit/user/UserControllerIntegrationTest.java \
        backend/src/test/java/com/github/mwacha/wachafit/assessment/AssessmentControllerIntegrationTest.java \
        backend/src/test/java/com/github/mwacha/wachafit/groupclass/GroupClassControllerIntegrationTest.java \
        backend/src/test/java/com/github/mwacha/wachafit/profile/StudentProfileControllerIntegrationTest.java \
        backend/src/test/java/com/github/mwacha/wachafit/report/ReportControllerIntegrationTest.java \
        backend/src/test/java/com/github/mwacha/wachafit/exercise/ExerciseControllerIntegrationTest.java \
        backend/src/test/java/com/github/mwacha/wachafit/workout/WorkoutControllerIntegrationTest.java \
        backend/src/test/java/com/github/mwacha/wachafit/billing/BillingControllerIntegrationTest.java \
        backend/src/test/java/com/github/mwacha/wachafit/membership/MembershipControllerIntegrationTest.java \
        backend/src/test/java/com/github/mwacha/wachafit/membership/MembershipPlanControllerIntegrationTest.java \
        backend/src/test/java/com/github/mwacha/wachafit/goal/GoalControllerIntegrationTest.java
git commit -m "feat(account): login sem tenantSlug + selecao/troca de academia (select-tenant, switch-tenant, my-tenants)"
```

---

## Task 7: UserService.createUser() vincula a Account existente

**Files:**
- Modify: `backend/src/main/java/com/github/mwacha/wachafit/user/UserService.java`
- Test: `backend/src/test/java/com/github/mwacha/wachafit/user/UserServiceTest.java`

**Interfaces:**
- Consumes: `AccountRepository` (Task 1), `UserRepository.existsByAccountIdAndTenantId` (Task 2)

- [ ] **Step 1: Escrever os testes que falham**

Substitua o conteúdo de `UserServiceTest.java` (o arquivo atual mocka `userRepository.existsByEmailAndTenantId`, que deixou de existir):

```java
// backend/src/test/java/com/github/mwacha/wachafit/user/UserServiceTest.java
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
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        userService.deactivateUser(userId, currentUserId);

        verify(userRepository).save(argThat(u -> !u.isActive()));
    }

    @Test
    void deactivateUser_shouldRejectSelfDeactivation() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, Role.ADMIN, true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.deactivateUser(userId, userId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("própria conta");
    }

    @Test
    void deactivateUser_shouldRejectStudentRole() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        User user = buildUser(userId, Role.STUDENT, true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.deactivateUser(userId, currentUserId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Cannot deactivate a student user");
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
```

- [ ] **Step 2: Rodar para confirmar falha**

```bash
cd backend
mvn test -Dtest=UserServiceTest -q 2>&1 | tail -30
```
Esperado: FAIL (não compila — `UserService` ainda usa `existsByEmailAndTenantId`/`setEmail`/`setPasswordHash`).

- [ ] **Step 3: Reescrever UserService**

```java
// backend/src/main/java/com/github/mwacha/wachafit/user/UserService.java
package com.github.mwacha.wachafit.user;

import com.github.mwacha.wachafit.account.Account;
import com.github.mwacha.wachafit.account.AccountRepository;
import com.github.mwacha.wachafit.notification.EmailService;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import com.github.mwacha.wachafit.tenant.Tenant;
import com.github.mwacha.wachafit.tenant.TenantContext;
import com.github.mwacha.wachafit.tenant.TenantRepository;
import com.github.mwacha.wachafit.user.dto.CreateUserRequest;
import com.github.mwacha.wachafit.user.dto.UpdateUserRequest;
import com.github.mwacha.wachafit.user.dto.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserService(UserRepository userRepository, AccountRepository accountRepository,
                       TenantRepository tenantRepository, PasswordEncoder passwordEncoder,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers(String role, Boolean active) {
        return userRepository.findAll().stream()   // Hibernate filter filtrará por tenant (Task 4 do plano multi-tenant)
            .filter(u -> role == null || u.getRole().name().equals(role))
            .filter(u -> active == null || u.isActive() == active)
            .map(this::toResponse)
            .toList();
    }

    public UserResponse createUser(CreateUserRequest req) {
        if (req.role() == Role.STUDENT) {
            throw new BusinessException("Não é permitido criar usuário com role STUDENT por este endpoint");
        }
        UUID tenantId = TenantContext.get();
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new NotFoundException("Tenant não encontrado"));

        // Se o e-mail já tiver conta, ignora a senha digitada e só cria o vínculo — a pessoa
        // continua usando a senha que já tinha em outra academia.
        Account account = accountRepository.findByEmail(req.email())
            .orElseGet(() -> {
                Account a = new Account();
                a.setName(req.name());
                a.setEmail(req.email());
                a.setPasswordHash(passwordEncoder.encode(req.password()));
                return accountRepository.save(a);
            });

        if (userRepository.existsByAccountIdAndTenantId(account.getId(), tenantId)) {
            throw new BusinessException("E-mail já cadastrado");
        }

        User user = new User();
        user.setAccount(account);
        user.setRole(req.role());
        user.setTenant(tenant);
        User saved = userRepository.save(user);
        emailService.sendHtml(
            account.getEmail(),
            "Bem-vindo ao WachaFit!",
            "email/welcome",
            Map.of("name", account.getName())
        );
        return toResponse(saved);
    }

    public UserResponse updateUser(UUID id, UpdateUserRequest req) {
        User user = findOrThrow(id);
        user.setRole(req.role());
        return toResponse(userRepository.save(user));
    }

    public void deactivateUser(UUID id, UUID currentUserId) {
        User user = findOrThrow(id);
        if (user.getRole() == Role.STUDENT) {
            throw new BusinessException("Cannot deactivate a student user");
        }
        if (id.equals(currentUserId)) {
            throw new BusinessException("Não é possível desativar a própria conta");
        }
        user.setActive(false);
        userRepository.save(user);
    }

    public void activateUser(UUID id) {
        User user = findOrThrow(id);
        user.setActive(true);
        userRepository.save(user);
    }

    private User findOrThrow(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + id));
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(
            u.getId().toString(),
            u.getName(),
            u.getEmail(),
            u.getRole().name(),
            u.isActive(),
            u.getCreatedAt() != null ? u.getCreatedAt().toString() : null
        );
    }
}
```

> **Nota:** `updateUser` não seta mais `user.setName(...)` — `UpdateUserRequest.name()` continua existindo no DTO, mas `User` não tem mais campo `name` próprio (delega para `account.getName()`). Se o requisito de "editar nome" continuar necessário por este endpoint, isso exigiria atualizar `user.getAccount().setName(...)` em vez de `user.setName(...)` — **fora do escopo deste plano** (o spec não cobre edição de nome/e-mail da conta a partir de dentro de uma academia — ver seção 5 do spec). Por ora, o parâmetro `req.name()` fica sem uso em `updateUser` (apenas `role` é aplicado); isso é uma limitação conhecida, não um bug a corrigir aqui.

- [ ] **Step 4: Rodar os testes**

```bash
mvn test -Dtest=UserServiceTest -q 2>&1 | tail -30
```
Esperado: PASS (7 testes).

- [ ] **Step 5: Compilar o projeto inteiro para confirmar que não sobrou nenhuma referência quebrada**

```bash
mvn compile test-compile -q 2>&1 | tail -60
```
Esperado: zero erros — esta é a primeira vez, desde a Task 2, que o projeto volta a compilar por completo.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/github/mwacha/wachafit/user/UserService.java \
        backend/src/test/java/com/github/mwacha/wachafit/user/UserServiceTest.java
git commit -m "feat(account): UserService.createUser vincula a Account existente por e-mail"
```

---

## Task 8: Rodar a suíte completa e confirmar ausência de regressão

**Files:** nenhum arquivo novo — apenas validação.

- [ ] **Step 1: Rodar a suíte completa do backend**

```bash
cd backend
mvn test 2>&1 | tail -40
```
Esperado: mesmo baseline pré-existente já aceito neste projeto (falhas em `BookingServiceTest` e erros de Testcontainers por falta de Docker no ambiente, se aplicável) — **nenhuma falha nova relacionada a `Account`/`User`/`AuthService`/`UserService`/JWT**. Se aparecer qualquer falha nova nesses pacotes, ela precisa ser investigada e corrigida antes de prosseguir — não é esperada.

- [ ] **Step 2: Rodar manualmente um fluxo de login via HTTP (opcional, requer Postgres real via Docker)**

```bash
# Com o backend rodando (mvn spring-boot:run) e o Postgres do docker-compose ativo:
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"wachafit@gmail.com","password":"<senha real>"}' | head -c 500
```
Esperado: um JSON com `token`/`role`/`userId`/`tenantId` preenchidos (conta com um único vínculo) — sem precisar de `tenantSlug`.

- [ ] **Step 3: Commit (se necessário — só se este passo revelar algo a corrigir)**

Nenhum commit esperado neste passo isoladamente — é apenas checkpoint de verificação antes das tasks de frontend.

---

## Task 9: Frontend — types/api.ts + auth.store.ts

**Files:**
- Modify: `frontend/src/types/api.ts`
- Modify: `frontend/src/stores/auth.store.ts`

**Interfaces:**
- Consumes: `POST /api/auth/login` (sem tenantSlug), `POST /api/auth/select-tenant`, `POST /api/auth/switch-tenant`, `GET /api/auth/my-tenants` (Task 6)
- Produces:
  - `LoginRequest { email, password }` — SEM `tenantSlug` (tipo já existe no arquivo, apenas remover o campo)
  - `TenantMembershipSummary { tenantId, tenantName, tenantSlug, role }`
  - `LoginNeedsTenantSelection { selectTenantToken, memberships }`
  - `LoginResult = LoginResponse | LoginNeedsTenantSelection`
  - `auth.login(email, password): Promise<LoginResult>`
  - `auth.selectTenant(selectTenantToken, tenantId): Promise<LoginResponse>`
  - `auth.switchTenant(tenantId): Promise<LoginResponse>`
  - `auth.myTenants(): Promise<TenantMembershipSummary[]>`

> **Atenção:** o tipo `LoginResponse` já existente em `api.ts` (campos `token`/`role`/`userId`/`tenantId`, todos `string` não-nulos) **NÃO muda** — é consumido por `RegisterView.vue`/`SignupView.vue` (fora de escopo) esperando esses 4 campos sempre presentes, o que continua verdade nesses dois fluxos. O novo tipo `LoginNeedsTenantSelection` é adicionado ao lado, e `LoginResult` é a união dos dois — usado apenas pelo retorno de `auth.login(...)`.

- [ ] **Step 1: Atualizar types/api.ts**

Localize a interface `LoginResponse` existente e o campo `tenantSlug` seria adicionado a `LoginRequest` (mas essa interface ainda não existe explicitamente no arquivo hoje — o login é feito com argumentos posicionais). Adicione ao final do bloco relacionado a auth (perto de onde `LoginResponse`/`SaasPlan` já estão declarados):

```typescript
// frontend/src/types/api.ts — adicionar (NÃO remover/alterar a interface LoginResponse já existente):

export interface TenantMembershipSummary {
  tenantId: string
  tenantName: string
  tenantSlug: string
  role: Role
}

export interface LoginNeedsTenantSelection {
  selectTenantToken: string
  memberships: TenantMembershipSummary[]
}

export type LoginResult = LoginResponse | LoginNeedsTenantSelection
```

- [ ] **Step 2: Atualizar auth.store.ts**

```typescript
// frontend/src/stores/auth.store.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/services/api'
import type { Role, LoginResponse, LoginResult, LoginNeedsTenantSelection, TenantMembershipSummary, SignupRequest } from '@/types/api'

function decodeJwtPayload(token: string): { sub: string; role: Role } | null {
  try {
    const payload = token.split('.')[1]
    return JSON.parse(atob(payload))
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const userId = ref<string | null>(localStorage.getItem('userId'))
  const role = ref<Role | null>((localStorage.getItem('role') as Role) ?? null)
  const tenantId = ref<string | null>(localStorage.getItem('tenantId'))

  const isAuthenticated = computed(() => token.value !== null)
  const userRole = computed(() => role.value)

  function setSession(data: LoginResponse) {
    token.value = data.token
    userId.value = data.userId
    role.value = data.role
    tenantId.value = data.tenantId
    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', data.userId)
    localStorage.setItem('role', data.role)
    localStorage.setItem('tenantId', data.tenantId)
  }

  function clearSession() {
    token.value = null
    userId.value = null
    role.value = null
    tenantId.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('role')
    localStorage.removeItem('tenantId')
  }

  async function login(email: string, password: string): Promise<LoginResult> {
    const { data } = await api.post<any>('/api/auth/login', { email, password })
    if (data.token) {
      setSession(data as LoginResponse)
      return data as LoginResponse
    }
    return data as LoginNeedsTenantSelection
  }

  async function selectTenant(selectTenantToken: string, tenantId: string): Promise<LoginResponse> {
    const { data } = await api.post<LoginResponse>('/api/auth/select-tenant', { selectTenantToken, tenantId })
    setSession(data)
    return data
  }

  async function switchTenant(tenantId: string): Promise<LoginResponse> {
    const { data } = await api.post<LoginResponse>('/api/auth/switch-tenant', { tenantId })
    setSession(data)
    return data
  }

  async function myTenants(): Promise<TenantMembershipSummary[]> {
    const { data } = await api.get<TenantMembershipSummary[]>('/api/auth/my-tenants')
    return data
  }

  async function register(name: string, email: string, password: string, tenantSlug: string): Promise<LoginResponse> {
    const { data } = await api.post<LoginResponse>('/api/auth/register', { name, email, password, tenantSlug })
    setSession(data)
    return data
  }

  async function signup(payload: SignupRequest): Promise<LoginResponse> {
    const { data } = await api.post<LoginResponse>('/api/public/signup', payload)
    setSession(data)
    return data
  }

  function logout() {
    clearSession()
  }

  // Validate stored token on store init: clear if malformed
  if (token.value) {
    const payload = decodeJwtPayload(token.value)
    if (!payload) {
      clearSession()
    }
  }

  return {
    token, userId, role, tenantId, isAuthenticated, userRole,
    login, selectTenant, switchTenant, myTenants, register, signup, logout, clearSession,
  }
})
```

> **Nota:** `register()` e `signup()` continuam retornando `Promise<LoginResponse>` (não `LoginResult`) sem qualquer alteração de assinatura — o backend garante que esses dois fluxos sempre criam exatamente um vínculo novo, nunca a resposta ambígua. Isso é o que mantém `RegisterView.vue`/`SignupView.vue` compilando sem tocar neles.

- [ ] **Step 3: Verificar que o build não tem erros de tipo**

```bash
cd frontend
npx vue-tsc -b 2>&1 | head -30
```
Esperado: zero erros.

> **Atenção (achado no Task 9):** use sempre `vue-tsc -b`, nunca `vue-tsc --noEmit` sozinho —
> o `tsconfig.json` raiz deste projeto usa o padrão "solution style" (`"files": []` +
> `references`), e `--noEmit` sem `-b` não resolve as referências, checando ZERO arquivos e
> retornando exit code 0 mesmo com erros reais no projeto. `-b` é o que o próprio script
> `"build"` do `package.json` usa e é o único que efetivamente verifica os `.vue`/`.ts`.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/types/api.ts frontend/src/stores/auth.store.ts
git commit -m "feat(account): frontend — auth store sem tenantSlug no login + selectTenant/switchTenant/myTenants"
```

---

## Task 10: Frontend — LoginView.vue sem slug + popup de seleção de academia

**Files:**
- Modify: `frontend/src/views/auth/LoginView.vue`
- Create: `frontend/src/views/auth/components/TenantSelectModal.vue`

**Interfaces:**
- Consumes: `auth.login()`/`auth.selectTenant()` retornando `LoginResult` (Task 9)

- [ ] **Step 1: Criar o componente TenantSelectModal.vue**

```vue
<!-- frontend/src/views/auth/components/TenantSelectModal.vue -->
<template>
  <Dialog
    :visible="true"
    modal
    :closable="false"
    header="Escolha a academia"
    :style="{ width: '380px' }"
  >
    <p class="modal-hint">Sua conta está associada a mais de uma academia. Escolha qual você quer acessar agora.</p>

    <div class="tenant-list">
      <button
        v-for="m in memberships" :key="m.tenantId"
        type="button"
        class="tenant-option"
        :class="{ selected: selectedTenantId === m.tenantId }"
        @click="selectedTenantId = m.tenantId"
      >
        <span class="tenant-name">{{ m.tenantName }}</span>
        <span class="tenant-role">{{ m.role }}</span>
      </button>
    </div>

    <div v-if="errorMessage" class="error-banner" role="alert">
      <i class="pi pi-exclamation-circle" />
      {{ errorMessage }}
    </div>

    <Button label="Entrar" :loading="loading" class="submit-btn" :disabled="!selectedTenantId" @click="confirm" />
  </Dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import type { TenantMembershipSummary } from '@/types/api'

const props = defineProps<{
  selectTenantToken: string
  memberships: TenantMembershipSummary[]
}>()

const emit = defineEmits<{ selected: [role: string] }>()

const selectedTenantId = ref('')
const errorMessage = ref('')
const loading = ref(false)

import { useAuthStore } from '@/stores/auth.store'
const auth = useAuthStore()

async function confirm() {
  if (!selectedTenantId.value) return
  errorMessage.value = ''
  loading.value = true
  try {
    const result = await auth.selectTenant(props.selectTenantToken, selectedTenantId.value)
    emit('selected', result.role)
  } catch (err: any) {
    errorMessage.value = err.response?.data?.message ?? 'Erro ao selecionar academia. Tente novamente.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.modal-hint {
  font-size: 13px;
  color: var(--neutral-600);
  margin-bottom: 16px;
}
.tenant-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}
.tenant-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  border: 1px solid #ddd;
  border-radius: var(--radius-md);
  background: #fff;
  cursor: pointer;
  text-align: left;
}
.tenant-option.selected {
  border-color: var(--blue-500);
  box-shadow: 0 0 0 2px var(--blue-500) inset;
}
.tenant-name { font-weight: 600; font-size: 14px; }
.tenant-role { font-size: 12px; color: var(--neutral-600); }
.error-banner {
  display: flex; align-items: center; gap: 8px;
  background: var(--error-bg); border: 1px solid #FECACA;
  border-radius: var(--radius-md); color: var(--error-text);
  font-size: 13px; font-weight: 500; padding: 10px 14px; margin-bottom: 12px;
}
.error-banner .pi { font-size: 14px; }
.submit-btn { width: 100% !important; justify-content: center; }
</style>
```

- [ ] **Step 2: Atualizar LoginView.vue — remover slug, tratar resultado ambíguo**

```vue
<!-- frontend/src/views/auth/LoginView.vue — template: remover o bloco inteiro do campo tenantSlug (o <div class="field"> com id="tenantSlug"), e adicionar o modal condicional logo antes do </template> final: -->
<TenantSelectModal
  v-if="pendingSelection"
  :select-tenant-token="pendingSelection.selectTenantToken"
  :memberships="pendingSelection.memberships"
  @selected="onTenantSelected"
/>
```

```typescript
// frontend/src/views/auth/LoginView.vue — <script setup>, substituir o conteúdo por:
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import { roleDashboards } from '@/utils/roleRoutes'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Button from 'primevue/button'
import TenantSelectModal from './components/TenantSelectModal.vue'
import type { LoginNeedsTenantSelection } from '@/types/api'

const auth = useAuthStore()
const router = useRouter()
const email = ref('')
const password = ref('')
const errorMessage = ref('')
const loading = ref(false)
const pendingSelection = ref<LoginNeedsTenantSelection | null>(null)

async function handleLogin() {
  errorMessage.value = ''
  if (!email.value || !password.value) {
    errorMessage.value = 'Preencha todos os campos.'
    return
  }
  loading.value = true
  try {
    const result = await auth.login(email.value, password.value)
    if ('selectTenantToken' in result) {
      pendingSelection.value = result
      return
    }
    router.push(roleDashboards[result.role])
  } catch (err: any) {
    errorMessage.value = err.response?.data?.message ?? 'Erro ao fazer login. Tente novamente.'
  } finally {
    loading.value = false
  }
}

function onTenantSelected(role: string) {
  router.push(roleDashboards[role as keyof typeof roleDashboards])
}
```

- [ ] **Step 3: Verificar que o build não tem erros de tipo**

```bash
cd frontend
npx vue-tsc -b 2>&1 | head -30
```
Esperado: zero erros.

- [ ] **Step 4: Testar manualmente o fluxo (requer backend + frontend rodando e Postgres real)**

1. `cd backend && mvn spring-boot:run`
2. `cd frontend && npm run dev`
3. Abrir `http://localhost:5173/login`
4. Logar com `wachafit@gmail.com` (conta com um único vínculo hoje) — deve ir direto para o dashboard, sem popup.
5. (Se houver tempo/dados de teste) Criar uma segunda conta com vínculos em 2 tenants diferentes e confirmar que o popup aparece, lista as duas academias, e a escolha leva ao dashboard certo.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/auth/LoginView.vue frontend/src/views/auth/components/TenantSelectModal.vue
git commit -m "feat(account): frontend — login sem slug + popup de selecao de academia"
```

---

## Task 11: Frontend — seletor de academia sempre visível no header

**Files:**
- Modify: `frontend/src/components/AppLayout.vue`

**Interfaces:**
- Consumes: `auth.myTenants()`/`auth.switchTenant()` (Task 9)

- [ ] **Step 1: Adicionar o seletor ao template, entre a busca e o botão de sair**

```vue
<!-- frontend/src/components/AppLayout.vue — dentro de <div class="topbar-right">, ANTES do <button class="logout-btn">: -->
<Select
  v-if="myTenants.length > 0"
  v-model="selectedTenantId"
  :options="myTenants"
  optionLabel="tenantName"
  optionValue="tenantId"
  class="tenant-switcher"
  :disabled="myTenants.length <= 1"
  @change="handleTenantSwitch"
/>
```

- [ ] **Step 2: Adicionar a lógica no script setup**

```typescript
// frontend/src/components/AppLayout.vue — <script setup>, adicionar aos imports existentes:
import Select from 'primevue/select'
import type { TenantMembershipSummary } from '@/types/api'

// adicionar junto às outras refs/computed já existentes:
const myTenants = ref<TenantMembershipSummary[]>([])
const selectedTenantId = ref(auth.tenantId ?? '')

async function loadMyTenants() {
  try {
    myTenants.value = await auth.myTenants()
    selectedTenantId.value = auth.tenantId ?? ''
  } catch {
    myTenants.value = []
  }
}

async function handleTenantSwitch() {
  if (!selectedTenantId.value || selectedTenantId.value === auth.tenantId) return
  try {
    const result = await auth.switchTenant(selectedTenantId.value)
    // Recarrega a página inteira: várias stores (billing, etc.) guardam estado da
    // academia anterior e não há um jeito centralizado de resetá-las todas hoje —
    // uma navegação dura garante que tudo recarregue do zero para o tenant novo.
    window.location.href = roleDashboards[result.role]
  } catch {
    selectedTenantId.value = auth.tenantId ?? ''
  }
}
```

Adicione a chamada de `loadMyTenants()` dentro do `onMounted` já existente (junto ao bloco que já busca `billing.fetchPaymentStatus()`):

```typescript
// frontend/src/components/AppLayout.vue — dentro do onMounted já existente, adicionar a chamada:
onMounted(async () => {
  tick()
  timer = setInterval(tick, 60_000)
  await loadMyTenants()
  if (auth.role === 'STUDENT') {
    await billing.fetchPaymentStatus()
    if (billing.hasOverduePayment && router.currentRoute.value.path !== '/student/charges') {
      router.replace('/student/charges')
    }
  }
})
```

- [ ] **Step 3: Adicionar o estilo do seletor**

```css
/* frontend/src/components/AppLayout.vue — dentro do <style scoped>, junto às regras de .topbar-right/.search-wrap já existentes: */
.tenant-switcher {
  max-width: 180px;
}
@media (max-width: 640px) {
  .tenant-switcher { display: none; }
}
```

- [ ] **Step 4: Verificar que o build não tem erros de tipo**

```bash
cd frontend
npx vue-tsc -b 2>&1 | head -30
```
Esperado: zero erros.

- [ ] **Step 5: Testar manualmente**

1. Com backend+frontend rodando, logar normalmente.
2. Confirmar que o seletor aparece no header mostrando a academia atual, mesmo havendo só uma (desabilitado — sem dropdown funcional).
3. (Se houver uma segunda academia de teste disponível) Trocar de academia pelo seletor e confirmar que a página recarrega e mostra os dados da academia escolhida.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/components/AppLayout.vue
git commit -m "feat(account): frontend — seletor de academia sempre visivel no header"
```

---

## Considerações finais

### O que NÃO está neste plano (escopo futuro)

Ver seção 5 do spec (`docs/superpowers/specs/2026-07-25-account-tenant-membership-design.md`):
1. Convite por e-mail para auto-associação a uma academia.
2. Tela de trocar e-mail/senha da conta a partir de dentro de uma academia específica.
3. Aviso manual para os casos raros em que dois `User`s hoje já compartilhavam e-mail entre tenants (a migração os une automaticamente, mas não avisa a pessoa).
4. `UserService.updateUser()` não atualiza mais o nome (ver nota na Task 7) — editar nome/e-mail da conta fica para um design futuro.

### Ordem de execução obrigatória

Tasks 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10 → 11 (todas sequenciais — sem paralelismo possível; Tasks 2-7 deixam o backend temporariamente sem compilar por completo até a Task 7 terminar, isso é esperado e documentado em cada task).
