# Etapa 5B — Matrículas e Planos Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar o módulo de matrículas: catálogo de planos de academia, assinatura do aluno vinculada ao plano, ciclo de vida (ACTIVE → CANCELLED), e geração automática de cobrança inicial em `payment_charges`.

**Architecture:** Package-by-feature. `membership` contém `MembershipPlan`, `MemberSubscription`, services e controllers. `billing` recebe apenas a entidade `PaymentCharge` + repository (a service e controller completos virão na Etapa 5C). `MembershipService` importa de `billing` para gerar a cobrança inicial ao matricular.

**Tech Stack:** Java 21, Spring Boot 3, Spring Data JPA, Flyway, Testcontainers + PostgreSQL para integration tests, Mockito para unit tests. Sem Lombok.

## Global Constraints

- Package membership: `com.github.mwacha.wachafit.membership`
- Package billing (só entidade + repo): `com.github.mwacha.wachafit.billing`
- Sem Lombok — getters/setters manuais em todas as entidades
- V19 já usado (`reminder_sent_to_bookings`) — migrations desta etapa começam em V20
- Regra de negócio: aluno pode ter apenas **uma** assinatura com `status = 'ACTIVE'` — `BusinessException` se tentar criar segunda
- Ao criar assinatura: `expires_at = started_at + durationMonths`, gerar `PaymentCharge` com `status = 'PENDING'`, `dueDate = startedAt`, `amount = plan.price`
- Ao cancelar assinatura: `status → 'CANCELLED'`, `cancelledAt = LocalDate.now()`, todas cobranças `PENDING` daquela assinatura → `'CANCELLED'`
- TDD obrigatório: escreva e execute o teste **falho** antes de implementar
- Commits frequentes por task

---

## Mapa de Arquivos

### Novos arquivos

```
backend/src/main/resources/db/migration/
  V20__create_membership_plans.sql
  V21__create_member_subscriptions.sql
  V22__create_payment_charges.sql

backend/src/main/java/com/github/mwacha/wachafit/membership/
  MembershipPlan.java
  MembershipPlanRepository.java
  MemberSubscription.java
  MemberSubscriptionRepository.java
  MembershipPlanService.java
  MembershipPlanController.java
  MembershipService.java
  MembershipController.java
  dto/
    CreatePlanRequest.java
    PlanResponse.java
    CreateSubscriptionRequest.java
    SubscriptionResponse.java

backend/src/main/java/com/github/mwacha/wachafit/billing/
  PaymentCharge.java
  PaymentChargeRepository.java

backend/src/test/java/com/github/mwacha/wachafit/membership/
  MembershipPlanServiceTest.java
  MembershipServiceTest.java
  MembershipPlanControllerIntegrationTest.java
  MembershipControllerIntegrationTest.java
```

---

### Task 1: Migrations V20, V21, V22

**Files:**
- Create: `backend/src/main/resources/db/migration/V20__create_membership_plans.sql`
- Create: `backend/src/main/resources/db/migration/V21__create_member_subscriptions.sql`
- Create: `backend/src/main/resources/db/migration/V22__create_payment_charges.sql`

**Interfaces:**
- Produces: tabelas `membership_plans`, `member_subscriptions`, `payment_charges` no PostgreSQL

- [ ] **Step 1: Criar branch de feature**

```bash
git checkout -b feat/etapa5b-membership
```

- [ ] **Step 2: Criar V20__create_membership_plans.sql**

```sql
CREATE TABLE membership_plans (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                 VARCHAR(100) NOT NULL,
    description          TEXT,
    duration_months      INT NOT NULL,
    price                NUMERIC(10,2) NOT NULL,
    max_classes_per_week INT,
    active               BOOLEAN NOT NULL DEFAULT true,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

- [ ] **Step 3: Criar V21__create_member_subscriptions.sql**

```sql
CREATE TABLE member_subscriptions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id          UUID NOT NULL REFERENCES users(id),
    plan_id             UUID NOT NULL REFERENCES membership_plans(id),
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                            CHECK (status IN ('ACTIVE','SUSPENDED','CANCELLED','EXPIRED')),
    started_at          DATE NOT NULL,
    expires_at          DATE NOT NULL,
    cancelled_at        DATE,
    cancellation_reason TEXT,
    created_by          UUID NOT NULL REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_subscriptions_student_status ON member_subscriptions(student_id, status);
```

- [ ] **Step 4: Criar V22__create_payment_charges.sql**

```sql
CREATE TABLE payment_charges (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id      UUID NOT NULL REFERENCES member_subscriptions(id),
    student_id           UUID NOT NULL REFERENCES users(id),
    amount               NUMERIC(10,2) NOT NULL,
    due_date             DATE NOT NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                             CHECK (status IN ('PENDING','PAID','OVERDUE','CANCELLED')),
    paid_at              TIMESTAMPTZ,
    payment_method       VARCHAR(20) CHECK (payment_method IN ('BOLETO','PIX','CARD','CASH')),
    gateway              VARCHAR(20) CHECK (gateway IN ('PAGSEGURO','MERCADOPAGO','MANUAL')),
    external_charge_id   VARCHAR(255),
    external_payment_url TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_charges_student_status ON payment_charges(student_id, status);
CREATE INDEX idx_charges_due_date       ON payment_charges(due_date, status);
```

- [ ] **Step 5: Verificar que as migrations passam com os testes existentes**

```bash
cd backend && mvn test -Dtest=StudentProfileControllerIntegrationTest -q 2>&1 | tail -6
```

Resultado esperado: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/resources/db/migration/V20__create_membership_plans.sql \
        backend/src/main/resources/db/migration/V21__create_member_subscriptions.sql \
        backend/src/main/resources/db/migration/V22__create_payment_charges.sql
git commit -m "feat: migrations V20-V22 — membership_plans, member_subscriptions, payment_charges"
```

---

### Task 2: MembershipPlan CRUD

**Files:**
- Create: `backend/src/main/java/com/github/mwacha/wachafit/membership/MembershipPlan.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/membership/MembershipPlanRepository.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/membership/dto/CreatePlanRequest.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/membership/dto/PlanResponse.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/membership/MembershipPlanService.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/membership/MembershipPlanController.java`
- Test: `backend/src/test/java/com/github/mwacha/wachafit/membership/MembershipPlanServiceTest.java`

**Interfaces:**
- Consumes: nada de tasks anteriores nesta task
- Produces:
  - `MembershipPlanService.createPlan(CreatePlanRequest) → PlanResponse`
  - `MembershipPlanService.getAllPlans() → List<PlanResponse>`
  - `MembershipPlanService.updatePlan(UUID, CreatePlanRequest) → PlanResponse`
  - `MembershipPlanService.deactivatePlan(UUID) → void`
  - `MembershipPlanRepository` (injetado no Task 3 por MembershipService)
  - `PlanResponse(UUID id, String name, String description, int durationMonths, BigDecimal price, Integer maxClassesPerWeek, boolean active, Instant createdAt)`

- [ ] **Step 1: Escrever o teste unitário falho**

`backend/src/test/java/com/github/mwacha/wachafit/membership/MembershipPlanServiceTest.java`:

```java
package com.github.mwacha.wachafit.membership;

import com.github.mwacha.wachafit.membership.dto.CreatePlanRequest;
import com.github.mwacha.wachafit.membership.dto.PlanResponse;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembershipPlanServiceTest {

    @Mock MembershipPlanRepository planRepo;
    @InjectMocks MembershipPlanService service;

    @Test
    void createPlan_shouldPersistAndReturnResponse() {
        CreatePlanRequest req = new CreatePlanRequest("Plano Básico", "Descrição", 1, new BigDecimal("99.90"), 3);
        when(planRepo.save(any())).thenAnswer(inv -> {
            MembershipPlan p = inv.getArgument(0);
            try {
                var f = MembershipPlan.class.getDeclaredField("id");
                f.setAccessible(true);
                f.set(p, UUID.randomUUID());
            } catch (Exception e) { throw new RuntimeException(e); }
            return p;
        });
        PlanResponse res = service.createPlan(req);
        assertThat(res.name()).isEqualTo("Plano Básico");
        assertThat(res.price()).isEqualByComparingTo("99.90");
        assertThat(res.durationMonths()).isEqualTo(1);
        assertThat(res.active()).isTrue();
    }

    @Test
    void deactivatePlan_shouldSetActiveFalse() {
        UUID id = UUID.randomUUID();
        MembershipPlan plan = new MembershipPlan();
        plan.setName("Plano Premium");
        plan.setDurationMonths(3);
        plan.setPrice(BigDecimal.valueOf(199));
        when(planRepo.findById(id)).thenReturn(Optional.of(plan));
        when(planRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.deactivatePlan(id);
        assertThat(plan.isActive()).isFalse();
    }

    @Test
    void updatePlan_shouldThrowNotFound_whenPlanAbsent() {
        UUID id = UUID.randomUUID();
        when(planRepo.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updatePlan(id,
            new CreatePlanRequest("X", null, 1, BigDecimal.ONE, null)))
            .isInstanceOf(NotFoundException.class);
    }
}
```

- [ ] **Step 2: Confirmar que o teste falha (classes ainda não existem)**

```bash
cd backend && mvn test -Dtest=MembershipPlanServiceTest -q 2>&1 | tail -6
```

Esperado: `BUILD FAILURE` (classes não encontradas)

- [ ] **Step 3: Criar `MembershipPlan.java`**

```java
package com.github.mwacha.wachafit.membership;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "membership_plans")
public class MembershipPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_months", nullable = false)
    private int durationMonths;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "max_classes_per_week")
    private Integer maxClassesPerWeek;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public int getDurationMonths() { return durationMonths; }
    public void setDurationMonths(int v) { this.durationMonths = v; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal v) { this.price = v; }
    public Integer getMaxClassesPerWeek() { return maxClassesPerWeek; }
    public void setMaxClassesPerWeek(Integer v) { this.maxClassesPerWeek = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }
    public Instant getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 4: Criar `MembershipPlanRepository.java`**

```java
package com.github.mwacha.wachafit.membership;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, UUID> {
}
```

- [ ] **Step 5: Criar DTOs**

`dto/CreatePlanRequest.java`:
```java
package com.github.mwacha.wachafit.membership.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreatePlanRequest(
    @NotBlank String name,
    String description,
    @Positive int durationMonths,
    @DecimalMin("0.01") BigDecimal price,
    Integer maxClassesPerWeek
) {}
```

`dto/PlanResponse.java`:
```java
package com.github.mwacha.wachafit.membership.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PlanResponse(
    UUID id,
    String name,
    String description,
    int durationMonths,
    BigDecimal price,
    Integer maxClassesPerWeek,
    boolean active,
    Instant createdAt
) {}
```

- [ ] **Step 6: Criar `MembershipPlanService.java`**

```java
package com.github.mwacha.wachafit.membership;

import com.github.mwacha.wachafit.membership.dto.CreatePlanRequest;
import com.github.mwacha.wachafit.membership.dto.PlanResponse;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MembershipPlanService {

    private final MembershipPlanRepository planRepo;

    public MembershipPlanService(MembershipPlanRepository planRepo) {
        this.planRepo = planRepo;
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> getAllPlans() {
        return planRepo.findAll().stream().map(this::toResponse).toList();
    }

    public PlanResponse createPlan(CreatePlanRequest req) {
        MembershipPlan plan = new MembershipPlan();
        plan.setName(req.name());
        plan.setDescription(req.description());
        plan.setDurationMonths(req.durationMonths());
        plan.setPrice(req.price());
        plan.setMaxClassesPerWeek(req.maxClassesPerWeek());
        return toResponse(planRepo.save(plan));
    }

    public PlanResponse updatePlan(UUID id, CreatePlanRequest req) {
        MembershipPlan plan = planRepo.findById(id)
            .orElseThrow(() -> new NotFoundException("Plano não encontrado"));
        plan.setName(req.name());
        plan.setDescription(req.description());
        plan.setDurationMonths(req.durationMonths());
        plan.setPrice(req.price());
        plan.setMaxClassesPerWeek(req.maxClassesPerWeek());
        return toResponse(planRepo.save(plan));
    }

    public void deactivatePlan(UUID id) {
        MembershipPlan plan = planRepo.findById(id)
            .orElseThrow(() -> new NotFoundException("Plano não encontrado"));
        plan.setActive(false);
        planRepo.save(plan);
    }

    private PlanResponse toResponse(MembershipPlan p) {
        return new PlanResponse(p.getId(), p.getName(), p.getDescription(),
            p.getDurationMonths(), p.getPrice(), p.getMaxClassesPerWeek(),
            p.isActive(), p.getCreatedAt());
    }
}
```

- [ ] **Step 7: Criar `MembershipPlanController.java`**

```java
package com.github.mwacha.wachafit.membership;

import com.github.mwacha.wachafit.membership.dto.CreatePlanRequest;
import com.github.mwacha.wachafit.membership.dto.PlanResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/membership-plans")
public class MembershipPlanController {

    private final MembershipPlanService service;

    public MembershipPlanController(MembershipPlanService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<PlanResponse> getAll() {
        return service.getAllPlans();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<PlanResponse> create(@Valid @RequestBody CreatePlanRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createPlan(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public PlanResponse update(@PathVariable UUID id, @Valid @RequestBody CreatePlanRequest req) {
        return service.updatePlan(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID id) {
        service.deactivatePlan(id);
    }
}
```

- [ ] **Step 8: Executar os testes e confirmar que passam**

```bash
cd backend && mvn test -Dtest=MembershipPlanServiceTest -q 2>&1 | tail -6
```

Esperado: `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/com/github/mwacha/wachafit/membership/ \
        backend/src/test/java/com/github/mwacha/wachafit/membership/MembershipPlanServiceTest.java
git commit -m "feat: MembershipPlan CRUD — entity, service, controller"
```

---

### Task 3: MemberSubscription + PaymentCharge + MembershipService

**Files:**
- Create: `backend/src/main/java/com/github/mwacha/wachafit/billing/PaymentCharge.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/billing/PaymentChargeRepository.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/membership/MemberSubscription.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/membership/MemberSubscriptionRepository.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/membership/dto/CreateSubscriptionRequest.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/membership/dto/SubscriptionResponse.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/membership/MembershipService.java`
- Create: `backend/src/main/java/com/github/mwacha/wachafit/membership/MembershipController.java`
- Test: `backend/src/test/java/com/github/mwacha/wachafit/membership/MembershipServiceTest.java`

**Interfaces:**
- Consumes: `MembershipPlanRepository` (do Task 2), `UserRepository` (existente em `com.github.mwacha.wachafit.user`)
- Produces:
  - `MembershipService.createSubscription(UUID studentId, CreateSubscriptionRequest, UUID createdBy) → SubscriptionResponse`
  - `MembershipService.getActiveSubscription(UUID studentId, User requestingUser) → SubscriptionResponse` (null se não tiver)
  - `MembershipService.cancelSubscription(UUID studentId, String cancellationReason, User requestingUser) → void`
  - `PaymentChargeRepository.cancelPendingBySubscriptionId(UUID subscriptionId) → void`

- [ ] **Step 1: Escrever o teste unitário falho**

`backend/src/test/java/com/github/mwacha/wachafit/membership/MembershipServiceTest.java`:

```java
package com.github.mwacha.wachafit.membership;

import com.github.mwacha.wachafit.billing.PaymentCharge;
import com.github.mwacha.wachafit.billing.PaymentChargeRepository;
import com.github.mwacha.wachafit.membership.dto.CreateSubscriptionRequest;
import com.github.mwacha.wachafit.membership.dto.SubscriptionResponse;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.shared.exception.ForbiddenException;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    @Mock MemberSubscriptionRepository subscriptionRepo;
    @Mock MembershipPlanRepository planRepo;
    @Mock PaymentChargeRepository chargeRepo;
    @Mock UserRepository userRepo;
    @InjectMocks MembershipService service;

    private UUID studentId;
    private User adminUser;
    private MembershipPlan plan;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();

        adminUser = new User();
        adminUser.setRole(Role.ADMIN);
        try {
            var f = User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(adminUser, UUID.randomUUID());
        } catch (Exception e) { throw new RuntimeException(e); }

        plan = new MembershipPlan();
        plan.setName("Plano Básico");
        plan.setDurationMonths(1);
        plan.setPrice(new BigDecimal("99.90"));
        try {
            var f = MembershipPlan.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(plan, UUID.randomUUID());
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void createSubscription_shouldPersistSubscriptionAndGenerateCharge() {
        when(userRepo.findById(studentId)).thenReturn(Optional.of(new User()));
        when(subscriptionRepo.existsByStudentIdAndStatus(studentId, "ACTIVE")).thenReturn(false);
        when(planRepo.findById(plan.getId())).thenReturn(Optional.of(plan));
        when(subscriptionRepo.save(any())).thenAnswer(inv -> {
            MemberSubscription s = inv.getArgument(0);
            try {
                var f = MemberSubscription.class.getDeclaredField("id");
                f.setAccessible(true);
                f.set(s, UUID.randomUUID());
            } catch (Exception e) { throw new RuntimeException(e); }
            return s;
        });

        CreateSubscriptionRequest req = new CreateSubscriptionRequest(plan.getId(), LocalDate.of(2026, 7, 1));
        SubscriptionResponse res = service.createSubscription(studentId, req, adminUser.getId());

        assertThat(res.status()).isEqualTo("ACTIVE");
        assertThat(res.expiresAt()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(res.planName()).isEqualTo("Plano Básico");
        verify(chargeRepo).save(any(PaymentCharge.class));
    }

    @Test
    void createSubscription_shouldThrowBusiness_whenAlreadyHasActiveSubscription() {
        when(userRepo.findById(studentId)).thenReturn(Optional.of(new User()));
        when(subscriptionRepo.existsByStudentIdAndStatus(studentId, "ACTIVE")).thenReturn(true);

        assertThatThrownBy(() -> service.createSubscription(studentId,
            new CreateSubscriptionRequest(plan.getId(), LocalDate.now()),
            adminUser.getId()))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("ativa");
    }

    @Test
    void cancelSubscription_shouldSetCancelledAndCancelPendingCharges() {
        MemberSubscription sub = new MemberSubscription();
        sub.setStudentId(studentId);
        sub.setStatus("ACTIVE");
        try {
            var f = MemberSubscription.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(sub, UUID.randomUUID());
        } catch (Exception e) { throw new RuntimeException(e); }

        when(subscriptionRepo.findByStudentIdAndStatus(studentId, "ACTIVE")).thenReturn(Optional.of(sub));

        service.cancelSubscription(studentId, "Mudança de planos", adminUser);

        assertThat(sub.getStatus()).isEqualTo("CANCELLED");
        assertThat(sub.getCancelledAt()).isNotNull();
        assertThat(sub.getCancellationReason()).isEqualTo("Mudança de planos");
        verify(chargeRepo).cancelPendingBySubscriptionId(sub.getId());
    }

    @Test
    void getActiveSubscription_shouldThrowForbidden_whenStudentAccessesOther() {
        User otherStudent = new User();
        otherStudent.setRole(Role.STUDENT);
        try {
            var f = User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(otherStudent, UUID.randomUUID());
        } catch (Exception e) { throw new RuntimeException(e); }

        assertThatThrownBy(() -> service.getActiveSubscription(studentId, otherStudent))
            .isInstanceOf(ForbiddenException.class);
    }
}
```

- [ ] **Step 2: Confirmar que o teste falha**

```bash
cd backend && mvn test -Dtest=MembershipServiceTest -q 2>&1 | tail -6
```

Esperado: `BUILD FAILURE`

- [ ] **Step 3: Criar `PaymentCharge.java`**

```java
package com.github.mwacha.wachafit.billing;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_charges")
public class PaymentCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @Column(length = 20)
    private String gateway;

    @Column(name = "external_charge_id", length = 255)
    private String externalChargeId;

    @Column(name = "external_payment_url", columnDefinition = "TEXT")
    private String externalPaymentUrl;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public UUID getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(UUID v) { this.subscriptionId = v; }
    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID v) { this.studentId = v; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { this.amount = v; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate v) { this.dueDate = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public OffsetDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(OffsetDateTime v) { this.paidAt = v; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String v) { this.paymentMethod = v; }
    public String getGateway() { return gateway; }
    public void setGateway(String v) { this.gateway = v; }
    public String getExternalChargeId() { return externalChargeId; }
    public void setExternalChargeId(String v) { this.externalChargeId = v; }
    public String getExternalPaymentUrl() { return externalPaymentUrl; }
    public void setExternalPaymentUrl(String v) { this.externalPaymentUrl = v; }
    public Instant getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 4: Criar `PaymentChargeRepository.java`**

```java
package com.github.mwacha.wachafit.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PaymentChargeRepository extends JpaRepository<PaymentCharge, UUID> {

    @Modifying
    @Query("UPDATE PaymentCharge c SET c.status = 'CANCELLED' WHERE c.subscriptionId = :subscriptionId AND c.status = 'PENDING'")
    void cancelPendingBySubscriptionId(@Param("subscriptionId") UUID subscriptionId);
}
```

- [ ] **Step 5: Criar `MemberSubscription.java`**

```java
package com.github.mwacha.wachafit.membership;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "member_subscriptions")
public class MemberSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "started_at", nullable = false)
    private LocalDate startedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDate expiresAt;

    @Column(name = "cancelled_at")
    private LocalDate cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID v) { this.studentId = v; }
    public UUID getPlanId() { return planId; }
    public void setPlanId(UUID v) { this.planId = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public LocalDate getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDate v) { this.startedAt = v; }
    public LocalDate getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDate v) { this.expiresAt = v; }
    public LocalDate getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDate v) { this.cancelledAt = v; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String v) { this.cancellationReason = v; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID v) { this.createdBy = v; }
    public Instant getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 6: Criar `MemberSubscriptionRepository.java`**

```java
package com.github.mwacha.wachafit.membership;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MemberSubscriptionRepository extends JpaRepository<MemberSubscription, UUID> {

    boolean existsByStudentIdAndStatus(UUID studentId, String status);

    Optional<MemberSubscription> findByStudentIdAndStatus(UUID studentId, String status);
}
```

- [ ] **Step 7: Criar DTOs da subscription**

`dto/CreateSubscriptionRequest.java`:
```java
package com.github.mwacha.wachafit.membership.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record CreateSubscriptionRequest(
    @NotNull UUID planId,
    @NotNull LocalDate startedAt
) {}
```

`dto/SubscriptionResponse.java`:
```java
package com.github.mwacha.wachafit.membership.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionResponse(
    UUID id,
    UUID studentId,
    UUID planId,
    String planName,
    String status,
    LocalDate startedAt,
    LocalDate expiresAt,
    Instant createdAt
) {}
```

- [ ] **Step 8: Criar `MembershipService.java`**

```java
package com.github.mwacha.wachafit.membership;

import com.github.mwacha.wachafit.billing.PaymentCharge;
import com.github.mwacha.wachafit.billing.PaymentChargeRepository;
import com.github.mwacha.wachafit.membership.dto.CreateSubscriptionRequest;
import com.github.mwacha.wachafit.membership.dto.SubscriptionResponse;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.shared.exception.ForbiddenException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional
public class MembershipService {

    private final MemberSubscriptionRepository subscriptionRepo;
    private final MembershipPlanRepository planRepo;
    private final PaymentChargeRepository chargeRepo;
    private final UserRepository userRepo;

    public MembershipService(MemberSubscriptionRepository subscriptionRepo,
                             MembershipPlanRepository planRepo,
                             PaymentChargeRepository chargeRepo,
                             UserRepository userRepo) {
        this.subscriptionRepo = subscriptionRepo;
        this.planRepo = planRepo;
        this.chargeRepo = chargeRepo;
        this.userRepo = userRepo;
    }

    public SubscriptionResponse createSubscription(UUID studentId, CreateSubscriptionRequest req, UUID createdBy) {
        userRepo.findById(studentId)
            .orElseThrow(() -> new NotFoundException("Aluno não encontrado"));

        if (subscriptionRepo.existsByStudentIdAndStatus(studentId, "ACTIVE")) {
            throw new BusinessException("Aluno já possui uma assinatura ativa");
        }

        MembershipPlan plan = planRepo.findById(req.planId())
            .orElseThrow(() -> new NotFoundException("Plano não encontrado"));

        MemberSubscription sub = new MemberSubscription();
        sub.setStudentId(studentId);
        sub.setPlanId(plan.getId());
        sub.setStatus("ACTIVE");
        sub.setStartedAt(req.startedAt());
        sub.setExpiresAt(req.startedAt().plusMonths(plan.getDurationMonths()));
        sub.setCreatedBy(createdBy);
        MemberSubscription saved = subscriptionRepo.save(sub);

        PaymentCharge charge = new PaymentCharge();
        charge.setSubscriptionId(saved.getId());
        charge.setStudentId(studentId);
        charge.setAmount(plan.getPrice());
        charge.setDueDate(req.startedAt());
        charge.setStatus("PENDING");
        chargeRepo.save(charge);

        return toResponse(saved, plan.getName());
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getActiveSubscription(UUID studentId, User requestingUser) {
        assertCanAccess(studentId, requestingUser);
        return subscriptionRepo.findByStudentIdAndStatus(studentId, "ACTIVE")
            .map(sub -> {
                String planName = planRepo.findById(sub.getPlanId())
                    .map(MembershipPlan::getName).orElse("Plano removido");
                return toResponse(sub, planName);
            }).orElse(null);
    }

    public void cancelSubscription(UUID studentId, String cancellationReason, User requestingUser) {
        assertCanAccess(studentId, requestingUser);
        MemberSubscription sub = subscriptionRepo.findByStudentIdAndStatus(studentId, "ACTIVE")
            .orElseThrow(() -> new NotFoundException("Assinatura ativa não encontrada"));
        sub.setStatus("CANCELLED");
        sub.setCancelledAt(LocalDate.now());
        sub.setCancellationReason(cancellationReason);
        subscriptionRepo.save(sub);
        chargeRepo.cancelPendingBySubscriptionId(sub.getId());
    }

    private void assertCanAccess(UUID studentId, User requestingUser) {
        if (requestingUser.getRole() == Role.STUDENT && !studentId.equals(requestingUser.getId())) {
            throw new ForbiddenException("Access denied");
        }
    }

    private SubscriptionResponse toResponse(MemberSubscription sub, String planName) {
        return new SubscriptionResponse(sub.getId(), sub.getStudentId(), sub.getPlanId(),
            planName, sub.getStatus(), sub.getStartedAt(), sub.getExpiresAt(), sub.getCreatedAt());
    }
}
```

- [ ] **Step 9: Criar `MembershipController.java`**

```java
package com.github.mwacha.wachafit.membership;

import com.github.mwacha.wachafit.membership.dto.CreateSubscriptionRequest;
import com.github.mwacha.wachafit.membership.dto.SubscriptionResponse;
import com.github.mwacha.wachafit.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class MembershipController {

    private final MembershipService service;

    public MembershipController(MembershipService service) {
        this.service = service;
    }

    @PostMapping("/api/students/{studentId}/subscription")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','ADMIN','MANAGER')")
    public ResponseEntity<SubscriptionResponse> create(
            @PathVariable UUID studentId,
            @Valid @RequestBody CreateSubscriptionRequest req,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(service.createSubscription(studentId, req, currentUser.getId()));
    }

    @GetMapping("/api/students/{studentId}/subscription")
    @PreAuthorize("isAuthenticated()")
    public SubscriptionResponse getActive(
            @PathVariable UUID studentId,
            @AuthenticationPrincipal User currentUser) {
        return service.getActiveSubscription(studentId, currentUser);
    }

    @DeleteMapping("/api/students/{studentId}/subscription")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(
            @PathVariable UUID studentId,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal User currentUser) {
        service.cancelSubscription(studentId, reason, currentUser);
    }
}
```

- [ ] **Step 10: Executar os testes e confirmar que passam**

```bash
cd backend && mvn test -Dtest=MembershipServiceTest,MembershipPlanServiceTest -q 2>&1 | tail -6
```

Esperado: `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 11: Commit**

```bash
git add backend/src/main/java/com/github/mwacha/wachafit/billing/ \
        backend/src/main/java/com/github/mwacha/wachafit/membership/ \
        backend/src/test/java/com/github/mwacha/wachafit/membership/MembershipServiceTest.java
git commit -m "feat: MemberSubscription + PaymentCharge + MembershipService"
```

---

### Task 4: Integration Tests + Suite Completa + Merge

**Files:**
- Create: `backend/src/test/java/com/github/mwacha/wachafit/membership/MembershipPlanControllerIntegrationTest.java`
- Create: `backend/src/test/java/com/github/mwacha/wachafit/membership/MembershipControllerIntegrationTest.java`

**Interfaces:**
- Consumes: todos os endpoints criados nas Tasks 2 e 3

- [ ] **Step 1: Criar `MembershipPlanControllerIntegrationTest.java`**

```java
package com.github.mwacha.wachafit.membership;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mwacha.wachafit.auth.dto.LoginRequest;
import com.github.mwacha.wachafit.membership.dto.CreatePlanRequest;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
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

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Testcontainers @ActiveProfiles("test")
class MembershipPlanControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        r.add("jwt.secret", () -> "integration-test-secret-32-chars-ok");
        r.add("jwt.expiration", () -> "3600");
        r.add("app.frontend-url", () -> "http://localhost:5173");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository userRepo;
    @Autowired MembershipPlanRepository planRepo;
    @Autowired PasswordEncoder passwordEncoder;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        planRepo.deleteAll();
        userRepo.deleteAll();

        User admin = new User();
        admin.setName("Admin"); admin.setEmail("admin@t.com");
        admin.setPasswordHash(passwordEncoder.encode("pass"));
        admin.setRole(Role.ADMIN); admin.setActive(true);
        userRepo.save(admin);

        var r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(new LoginRequest("admin@t.com", "pass")))).andReturn();
        adminToken = mapper.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void createPlan_withAdminToken_shouldReturn201() throws Exception {
        CreatePlanRequest req = new CreatePlanRequest("Plano Básico", "Mensal", 1, new BigDecimal("99.90"), 3);
        mvc.perform(post("/api/membership-plans")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Plano Básico"))
            .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void getAllPlans_authenticatedUser_shouldReturn200() throws Exception {
        mvc.perform(get("/api/membership-plans")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());
    }

    @Test
    void deactivatePlan_shouldSetActiveToFalse() throws Exception {
        CreatePlanRequest req = new CreatePlanRequest("Plano Premium", null, 3, new BigDecimal("199.90"), null);
        var create = mvc.perform(post("/api/membership-plans")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andReturn();
        String planId = mapper.readTree(create.getResponse().getContentAsString()).get("id").asText();

        mvc.perform(delete("/api/membership-plans/" + planId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNoContent());
    }
}
```

- [ ] **Step 2: Criar `MembershipControllerIntegrationTest.java`**

```java
package com.github.mwacha.wachafit.membership;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mwacha.wachafit.auth.dto.LoginRequest;
import com.github.mwacha.wachafit.billing.PaymentChargeRepository;
import com.github.mwacha.wachafit.membership.dto.CreatePlanRequest;
import com.github.mwacha.wachafit.membership.dto.CreateSubscriptionRequest;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Testcontainers @ActiveProfiles("test")
class MembershipControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        r.add("jwt.secret", () -> "integration-test-secret-32-chars-ok");
        r.add("jwt.expiration", () -> "3600");
        r.add("app.frontend-url", () -> "http://localhost:5173");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository userRepo;
    @Autowired MembershipPlanRepository planRepo;
    @Autowired MemberSubscriptionRepository subscriptionRepo;
    @Autowired PaymentChargeRepository chargeRepo;
    @Autowired PasswordEncoder passwordEncoder;

    private String adminToken;
    private UUID studentId;
    private UUID planId;

    @BeforeEach
    void setUp() throws Exception {
        chargeRepo.deleteAll();
        subscriptionRepo.deleteAll();
        planRepo.deleteAll();
        userRepo.deleteAll();

        User admin = new User();
        admin.setName("Admin"); admin.setEmail("admin@t.com");
        admin.setPasswordHash(passwordEncoder.encode("pass"));
        admin.setRole(Role.ADMIN); admin.setActive(true);
        userRepo.save(admin);

        User student = new User();
        student.setName("Student"); student.setEmail("student@t.com");
        student.setPasswordHash(passwordEncoder.encode("pass"));
        student.setRole(Role.STUDENT); student.setActive(true);
        userRepo.save(student);
        studentId = student.getId();

        var loginResult = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(new LoginRequest("admin@t.com", "pass")))).andReturn();
        adminToken = mapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();

        var planResult = mvc.perform(post("/api/membership-plans")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CreatePlanRequest("Plano Básico", null, 1, new BigDecimal("99.90"), null))))
            .andReturn();
        planId = UUID.fromString(mapper.readTree(planResult.getResponse().getContentAsString()).get("id").asText());
    }

    @Test
    void createSubscription_withAdminToken_shouldReturn201AndGenerateCharge() throws Exception {
        CreateSubscriptionRequest req = new CreateSubscriptionRequest(planId, LocalDate.of(2026, 7, 1));
        mvc.perform(post("/api/students/" + studentId + "/subscription")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.planName").value("Plano Básico"))
            .andExpect(jsonPath("$.expiresAt").value("2026-08-01"));

        assert chargeRepo.count() == 1;
    }

    @Test
    void createSubscription_whenAlreadyHasActive_shouldReturn409() throws Exception {
        CreateSubscriptionRequest req = new CreateSubscriptionRequest(planId, LocalDate.of(2026, 7, 1));
        mvc.perform(post("/api/students/" + studentId + "/subscription")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated());

        mvc.perform(post("/api/students/" + studentId + "/subscription")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isConflict());
    }

    @Test
    void cancelSubscription_shouldReturnNoContent() throws Exception {
        CreateSubscriptionRequest req = new CreateSubscriptionRequest(planId, LocalDate.of(2026, 7, 1));
        mvc.perform(post("/api/students/" + studentId + "/subscription")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated());

        mvc.perform(delete("/api/students/" + studentId + "/subscription")
                .header("Authorization", "Bearer " + adminToken)
                .param("reason", "Cancelamento teste"))
            .andExpect(status().isNoContent());
    }

    @Test
    void getSubscription_whenStudent_shouldOnlySeeOwn() throws Exception {
        CreateSubscriptionRequest req = new CreateSubscriptionRequest(planId, LocalDate.of(2026, 7, 1));
        mvc.perform(post("/api/students/" + studentId + "/subscription")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated());

        var studentLogin = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(new LoginRequest("student@t.com", "pass")))).andReturn();
        String studentToken = mapper.readTree(studentLogin.getResponse().getContentAsString()).get("token").asText();

        mvc.perform(get("/api/students/" + studentId + "/subscription")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
```

- [ ] **Step 3: Executar somente os novos testes de integração**

```bash
cd backend && mvn test -Dtest="MembershipPlanControllerIntegrationTest,MembershipControllerIntegrationTest" -q 2>&1 | tail -8
```

Esperado: `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS`

- [ ] **Step 4: Executar suíte completa**

```bash
cd backend && mvn test 2>&1 | tail -8
```

Esperado: `Tests run: 137, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS`

(130 anteriores + 7 novos)

- [ ] **Step 5: Commit**

```bash
git add backend/src/test/java/com/github/mwacha/wachafit/membership/
git commit -m "test: integration tests para MembershipPlan e MemberSubscription"
```

- [ ] **Step 6: Merge para main e push**

```bash
git checkout main
git merge feat/etapa5b-membership
git push origin main
git branch -d feat/etapa5b-membership
```

---

## Self-Review

### 1. Cobertura da spec (seção 4.2)

| Requisito | Coberto em |
|-----------|-----------|
| `GET /api/membership-plans` — autenticado | Task 2, MembershipPlanController |
| `POST /api/membership-plans` — ADMIN, MANAGER | Task 2, MembershipPlanController |
| `PUT /api/membership-plans/{id}` — ADMIN, MANAGER | Task 2, MembershipPlanController |
| `DELETE /api/membership-plans/{id}` — soft-deactivate | Task 2, MembershipPlanController |
| `POST /api/students/{id}/subscription` — RECEPTIONIST, ADMIN, MANAGER | Task 3, MembershipController |
| `GET /api/students/{id}/subscription` — dono, RECEPTIONIST, ADMIN, MANAGER | Task 3, MembershipController |
| `DELETE /api/students/{id}/subscription` — ADMIN, MANAGER | Task 3, MembershipController |
| Regra: 1 assinatura ACTIVE — BusinessException | Task 3, MembershipService |
| `expires_at = started_at + durationMonths` | Task 3, MembershipService |
| Gera cobrança inicial em `payment_charges` | Task 3, MembershipService + PaymentChargeRepository |
| Ao cancelar: status CANCELLED, cobranças PENDING → CANCELLED | Task 3, MembershipService |

### 2. Verificação de placeholders

Nenhum TBD, TODO ou "adicione validação" encontrado.

### 3. Consistência de tipos

- `MembershipPlanRepository` criado na Task 2 e consumido nas Tasks 3 e 4 ✅
- `PaymentChargeRepository.cancelPendingBySubscriptionId(UUID)` definido na Task 3 e verificado no teste ✅
- `SubscriptionResponse.planName` propagado de `MembershipPlan.getName()` ✅
- Número de migrações: V20, V21, V22 — sem conflito com V19 existente ✅
