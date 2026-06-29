# WachaFit Etapa 5 — Cadastros Completos + Financeiro
**Data:** 2026-06-29
**Escopo:** Perfis completos (5A), Matrículas e Planos (5B), Cobranças + Gateway (5C), Caixa e Relatórios (5D)

---

## 1. Visão Geral

Expansão do WachaFit para suportar operação completa de academia:
- **5A — Perfis:** novos roles (MANAGER, RECEPTIONIST, CASHIER), perfil estendido de aluno (dados pessoais + anamnese) e de profissional (CREF, especialidades, contrato, comissão)
- **5B — Matrículas:** catálogo de planos, matrícula do aluno vinculada ao plano, ciclo de vida da assinatura
- **5C — Cobranças:** geração automática de cobranças, integração com PagSeguro/Mercado Pago (boleto/PIX), webhook de confirmação
- **5D — Relatórios:** receita, inadimplência, fluxo de caixa, comissões de profissionais

**Princípio de extensão:** nenhuma tabela existente é quebrada. Novos dados vão em tabelas satélite com FK para `users`.

---

## 2. Novos Roles

```java
public enum Role {
    ADMIN, MANAGER, RECEPTIONIST, CASHIER, TRAINER, STUDENT
}
```

| Role | Permissões |
|------|-----------|
| ADMIN / MANAGER | Tudo |
| RECEPTIONIST | Matricular aluno, registrar pgto manual, ver agenda |
| CASHIER | Cobranças, caixa, relatórios financeiros |
| TRAINER | Agenda própria, fichas, avaliações dos seus alunos |
| STUDENT | Próprio perfil, reservas, treino, fotos |

---

## 3. Schema do Banco

### Etapa 5A — Perfis

#### V16 — `student_profiles`
```sql
CREATE TABLE student_profiles (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                UUID NOT NULL UNIQUE REFERENCES users(id),
    cpf                    VARCHAR(14) NOT NULL UNIQUE,
    birth_date             DATE,
    phone                  VARCHAR(20),
    address_line           VARCHAR(200),
    address_city           VARCHAR(100),
    address_state          CHAR(2),
    address_zip            VARCHAR(9),
    emergency_contact_name VARCHAR(120),
    emergency_contact_phone VARCHAR(20),
    profile_photo_key      VARCHAR(255),
    document_photo_key     VARCHAR(255),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

#### V17 — `student_health`
```sql
CREATE TABLE student_health (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id               UUID NOT NULL UNIQUE REFERENCES users(id),
    has_heart_condition   BOOLEAN NOT NULL DEFAULT false,
    has_diabetes          BOOLEAN NOT NULL DEFAULT false,
    has_hypertension      BOOLEAN NOT NULL DEFAULT false,
    medications           TEXT,
    physical_restrictions TEXT,
    parq_signed_at        DATE,
    notes                 TEXT,
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

#### V18 — `trainer_profiles`
```sql
CREATE TABLE trainer_profiles (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL UNIQUE REFERENCES users(id),
    cref             VARCHAR(20),
    specialties      TEXT[],
    bio              TEXT,
    profile_photo_key VARCHAR(255),
    contract_type    VARCHAR(20) CHECK (contract_type IN ('CLT','PJ','FREELANCE')),
    admission_date   DATE,
    commission_type  VARCHAR(20) CHECK (commission_type IN ('FIXED','PERCENTAGE')),
    commission_value NUMERIC(8,2),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### Etapa 5B — Matrículas e Planos

#### V19 — `membership_plans`
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

#### V20 — `member_subscriptions`
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

### Etapa 5C — Cobranças

#### V21 — `payment_charges`
```sql
CREATE TABLE payment_charges (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id     UUID NOT NULL REFERENCES member_subscriptions(id),
    student_id          UUID NOT NULL REFERENCES users(id),
    amount              NUMERIC(10,2) NOT NULL,
    due_date            DATE NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING','PAID','OVERDUE','CANCELLED')),
    paid_at             TIMESTAMPTZ,
    payment_method      VARCHAR(20) CHECK (payment_method IN ('BOLETO','PIX','CARD','CASH')),
    gateway             VARCHAR(20) CHECK (gateway IN ('PAGSEGURO','MERCADOPAGO','MANUAL')),
    external_charge_id  VARCHAR(255),
    external_payment_url TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_charges_student_status ON payment_charges(student_id, status);
CREATE INDEX idx_charges_due_date       ON payment_charges(due_date, status);
```

---

## 4. Módulos Backend

### 4.1 `profile` — `com.github.mwacha.wachafit.profile`

**Endpoints:**

| Método | Rota | Acesso |
|--------|------|--------|
| POST | `/api/students/{id}/profile` | RECEPTIONIST, ADMIN, MANAGER |
| GET | `/api/students/{id}/profile` | dono, TRAINER, ADMIN, MANAGER |
| PUT | `/api/students/{id}/profile` | dono, RECEPTIONIST, ADMIN, MANAGER |
| POST | `/api/students/{id}/health` | dono, TRAINER, ADMIN, MANAGER |
| GET | `/api/students/{id}/health` | dono, TRAINER, ADMIN, MANAGER |
| PUT | `/api/students/{id}/health` | dono, TRAINER, ADMIN, MANAGER |
| POST | `/api/trainers/{id}/profile` | ADMIN, MANAGER |
| GET | `/api/trainers/{id}/profile` | autenticado |
| PUT | `/api/trainers/{id}/profile` | ADMIN, MANAGER |

**DTOs chave:**
- `CreateStudentProfileRequest(cpf, birthDate?, phone?, addressLine?, addressCity?, addressState?, addressZip?, emergencyContactName?, emergencyContactPhone?)`
- `StudentHealthRequest(hasHeartCondition, hasDiabetes, hasHypertension, medications?, physicalRestrictions?, parqSignedAt?, notes?)`
- `CreateTrainerProfileRequest(cref?, specialties?, bio?, contractType?, admissionDate?, commissionType?, commissionValue?)`

**Regras:**
- CPF único no sistema — BusinessException se duplicado
- RN-11: aluno só vê próprio perfil; TRAINER/ADMIN/MANAGER veem qualquer aluno

---

### 4.2 `membership` — `com.github.mwacha.wachafit.membership`

**Endpoints:**

| Método | Rota | Acesso |
|--------|------|--------|
| GET | `/api/membership-plans` | autenticado |
| POST | `/api/membership-plans` | ADMIN, MANAGER |
| PUT | `/api/membership-plans/{id}` | ADMIN, MANAGER |
| DELETE | `/api/membership-plans/{id}` | ADMIN, MANAGER (soft-deactivate) |
| POST | `/api/students/{id}/subscription` | RECEPTIONIST, ADMIN, MANAGER |
| GET | `/api/students/{id}/subscription` | dono, RECEPTIONIST, ADMIN, MANAGER |
| DELETE | `/api/students/{id}/subscription` | ADMIN, MANAGER |

**Regras:**
- Aluno pode ter apenas **uma assinatura ACTIVE** — BusinessException se tentar criar segunda
- Ao criar assinatura: `expires_at = started_at + duration_months`, gera cobrança inicial em `payment_charges`
- Ao cancelar: status → CANCELLED, cobranças PENDING → CANCELLED

**DTOs:**
- `CreatePlanRequest(name, description?, durationMonths, price, maxClassesPerWeek?)`
- `CreateSubscriptionRequest(planId, startedAt)`
- `SubscriptionResponse(id, studentId, planId, planName, status, startedAt, expiresAt, createdAt)`

---

### 4.3 `billing` — `com.github.mwacha.wachafit.billing`

**Endpoints:**

| Método | Rota | Acesso |
|--------|------|--------|
| GET | `/api/students/{id}/charges` | dono, RECEPTIONIST, CASHIER, ADMIN, MANAGER |
| POST | `/api/students/{id}/charges` | ADMIN, MANAGER (cobrança avulsa) |
| PATCH | `/api/charges/{id}/pay` | RECEPTIONIST, CASHIER, ADMIN, MANAGER |
| PATCH | `/api/charges/{id}/cancel` | ADMIN, MANAGER |
| POST | `/api/payments/webhook` | público (validado por assinatura) |

**Integração Gateway:**
- `PaymentGatewayService` — interface com dois adapters: `MercadoPagoAdapter` e `PagSeguroAdapter`
- Selecionado via `app.payment.gateway` em `application.yml`
- Ao criar cobrança com gateway: POST na API externa → salva `external_charge_id` e `external_payment_url`
- Webhook `POST /api/payments/webhook`: valida `x-signature`, atualiza status da cobrança, chama `SubscriptionService` para suspender se necessário

**Regras:**
- Cobrança gerada automaticamente ao matricular (primeira mensalidade)
- Job agendado (`@Scheduled(cron = "0 0 6 * * *")`): muda PENDING com `due_date < today` para OVERDUE
- OVERDUE há `app.payment.suspend-after-days` dias → suspende assinatura

**Config:**
```yaml
app:
  payment:
    gateway: mercadopago
    access-token: ${PAYMENT_ACCESS_TOKEN}
    webhook-secret: ${PAYMENT_WEBHOOK_SECRET}
    suspend-after-days: 5
```

**DTOs:**
- `ChargeResponse(id, studentId, subscriptionId, amount, dueDate, status, paidAt, paymentMethod, externalPaymentUrl, createdAt)`
- `ManualPaymentRequest(paymentMethod)` — para `PATCH /pay`

---

### 4.4 `report` — `com.github.mwacha.wachafit.report`

**Endpoints:**

| Método | Rota | Acesso |
|--------|------|--------|
| GET | `/api/reports/revenue?from=&to=` | CASHIER, MANAGER, ADMIN |
| GET | `/api/reports/overdue` | CASHIER, MANAGER, ADMIN |
| GET | `/api/reports/subscriptions` | MANAGER, ADMIN |
| GET | `/api/reports/trainer-commissions?from=&to=` | MANAGER, ADMIN |
| GET | `/api/reports/cash-flow?from=&to=` | CASHIER, MANAGER, ADMIN |

**DTOs:**
```java
record RevenueReport(YearMonth month, BigDecimal total, int chargesCount) {}
record OverdueStudent(UUID studentId, String name, BigDecimal totalDue, int daysOverdue) {}
record SubscriptionStats(int active, int suspended, int cancelled, int expired) {}
record TrainerCommission(UUID trainerId, String name, String commissionType, BigDecimal commissionDue, int classesCount) {}
record CashFlowDay(LocalDate date, BigDecimal received, BigDecimal pending, BigDecimal overdue) {}
```

Todos os relatórios são queries JPQL/native de leitura, sem estado mutável.

---

## 5. Frontend (novas telas)

### Novas rotas e views

**Admin / Manager:**
- `/admin/membership-plans` — CRUD de planos
- `/admin/students/{id}/enroll` — matrícula com seleção de plano
- `/admin/students/{id}/profile` — dados pessoais completos
- `/admin/reports/revenue` — gráfico + tabela de receita
- `/admin/reports/overdue` — inadimplentes com ação de cobrança
- `/admin/reports/commissions` — comissões por profissional

**Cashier:**
- `/cashier/cash-flow` — fluxo de caixa diário
- `/cashier/charges` — lista de cobranças pendentes/vencidas

**Receptionist:**
- `/reception/enroll` — tela de matrícula rápida
- `/reception/charges/{id}/pay` — registrar pagamento manual

**Student:**
- `/student/subscription` — meu plano atual, próxima cobrança, link de pagamento
- `/student/charges` — histórico de cobranças

**Trainer:**
- `/trainer/profile` — editar próprio perfil (CREF, bio, foto)

### Novos stores Pinia
`membership.store.ts`, `billing.store.ts`, `profile.store.ts`

### Novos serviços
`membership.service.ts`, `billing.service.ts`, `profile.service.ts`, `report.service.ts`

---

## 6. Testes

- **5A:** unit (ProfileService, HealthService, TrainerProfileService) + integration por endpoint
- **5B:** unit (MembershipService: regra 1-active, geração de cobrança) + integration
- **5C:** unit (BillingService: gateway mock, webhook handler, job) + integration; gateway testado com mock/stub
- **5D:** unit (ReportService queries) + integration com dados seed

---

## 7. Configuração adicional

`application.yml`:
```yaml
app:
  payment:
    gateway: mercadopago
    access-token: ${PAYMENT_ACCESS_TOKEN}
    webhook-secret: ${PAYMENT_WEBHOOK_SECRET}
    suspend-after-days: 5
  upload-dir: uploads    # já existente
```

`application-dev.yml`:
```yaml
app:
  payment:
    gateway: manual    # sem chamadas reais em dev
    suspend-after-days: 5
```
