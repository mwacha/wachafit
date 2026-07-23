# Design — Cadastro self-service de academia (tenant) com plano SaaS

**Contexto:** Extensão do plano multi-tenant (`docs/superpowers/plans/2026-07-21-multi-tenant.md`). Este design adiciona uma tela pública onde uma academia se cadastra sozinha no WachaFit, criando seu próprio tenant, conta ADMIN e assinatura de um plano SaaS — sem precisar de intervenção manual do time WachaFit (que continua tendo o caminho administrativo já previsto na Task 8 do plano, `POST /api/super/tenants`).

**Dependências:** Tasks 1–9 do plano multi-tenant (Tenant, TenantContext, User.tenant, JWT com tenantId, TenantAwareEntity) precisam estar prontas antes desta feature.

---

## 1. Modelo de dados

Novo pacote `saas/` (paralelo a `membership/`, sem relação com ele — `MembershipPlan` é o plano que o **aluno** contrata da academia; `SaasPlan` é o plano que a **academia** contrata do WachaFit).

### `SaasPlan` (tabela `saas_plans`)
- `id UUID`
- `name VARCHAR(100)`
- `description TEXT`
- `price NUMERIC(10,2)`
- `billingPeriodMonths INT` (ex: 1)
- `maxUsers INTEGER` (nullable = ilimitado)
- `active BOOLEAN`
- CRUD exposto apenas para `SUPER_ADMIN` via `/api/super/saas-plans`, seguindo o padrão de `MembershipPlanController`.

### `TenantSubscription` (tabela `tenant_subscriptions`)
- `id UUID`
- `tenantId UUID` (FK `tenants`)
- `saasPlanId UUID` (FK `saas_plans`)
- `status` enum: `TRIALING | ACTIVE | PAST_DUE | CANCELED`
- `trialEndsAt Instant` (nullable — null quando não há trial, ex: backfill do tenant "Personal Studio")
- `currentPeriodEnd Instant`
- `createdAt Instant`
- Um tenant tem no máximo uma subscription "corrente" (regra de aplicação, não constraint de banco — permite histórico de trocas de plano).

### `TenantCharge` (tabela `tenant_charges`)
- `id UUID`
- `tenantId UUID`
- `subscriptionId UUID`
- `amount NUMERIC(10,2)`
- `dueDate LocalDate`
- `status` (`PENDING | PAID | OVERDUE`)
- `paymentMethod` (`CREDIT_CARD | PIX | BOLETO`)
- `paidAt OffsetDateTime` (nullable)
- `createdAt Instant`
- Sem `gateway`/`externalChargeId` por enquanto — não há gateway real, mesmo espírito do `ManualGatewayAdapter` atual.

Nenhuma dessas três tabelas estende `TenantAwareEntity` — elas **são** a definição do tenant e seus planos, não dados operacionais isolados por tenant.

### Migrations
- `V32__create_saas_plans.sql` — cria tabela + seed de 2-3 planos padrão (ex: "Starter", "Pro").
- `V33__create_tenant_subscriptions_and_charges.sql` — cria as duas tabelas + seed de uma `TenantSubscription` com `status = ACTIVE`, `trialEndsAt = NULL`, para o tenant `00000000-0000-0000-0000-000000000001` (Personal Studio), vinculada ao plano "Pro". **Sem** `TenantCharge` retroativa.

### Ajuste na Task 1 (V29__create_tenants.sql)
O INSERT do tenant padrão passa a ser:
```sql
INSERT INTO tenants (id, name, slug)
VALUES ('00000000-0000-0000-0000-000000000001', 'Personal Studio', 'personal-studio');
```
(mesmo `id` fixo, para não quebrar a migração de dados existentes das Tasks 3 e 5 do plano multi-tenant).

---

## 2. Backend — endpoints e fluxo do cadastro self-service

### `GET /api/public/saas-plans` (sem auth)
Lista `SaasPlan` com `active = true`, ordenados por preço. Retorna `id, name, description, price, billingPeriodMonths, maxUsers`.

### `GET /api/public/check-slug?slug=...` (sem auth)
Retorna `{ available: boolean }` — usado no passo 2 do wizard para feedback em tempo real (debounce no frontend).

### `POST /api/public/signup` (sem auth — roda antes de qualquer `TenantContext` existir)

Request:
```json
{
  "admin": { "name": "...", "email": "...", "password": "..." },
  "company": { "name": "...", "cnpj": "...", "phone": "...", "slug": "..." },
  "plan": { "saasPlanId": "uuid", "paymentMethod": "CREDIT_CARD|PIX|BOLETO" }
}
```

Response: igual ao `LoginResponse` atual (token JWT + tenantId + role) — o admin cai autenticado direto no dashboard.

**Fluxo (`SignupService.signup(...)`):**

1. Validações: `slug` (regex + unicidade em `tenants`), `cnpj` (formato + dígito verificador + unicidade em `tenants`), `email` (formato — unicidade é por tenant, e como o tenant é novo não há conflito), senha (política já usada no `RegisterView` atual: mín. 8 caracteres).
2. **Transação 1 (obrigatória, tudo ou nada):**
   a. Cria `Tenant` (`active = true`).
   b. Cria `User` com `role = ADMIN`, vinculado ao tenant.
   c. Cria `TenantSubscription` (`status = TRIALING`, `trialEndsAt = now + 14 dias`).
   d. Commit. Se qualquer passo falhar (ex: slug duplicado por race condition pego pela constraint `UNIQUE`), tudo é revertido e retorna `409`.
3. **Passo 2 (best-effort, fora da transação principal ou em `REQUIRES_NEW`):**
   - Cria `TenantCharge` (`status = PENDING`, `dueDate = trialEndsAt`, `paymentMethod` escolhido, `amount = plan.price`).
   - Se falhar: apenas loga o erro (`logger.error`). **Não** reverte a criação do tenant/admin/subscription. A ausência de charge fica visível para o `SUPER_ADMIN` reconciliar manualmente depois (mesmo padrão do `OverdueJobService` existente).
4. Gera JWT (`JwtUtil.generateToken(user)`, já com `tenantId` claim da Task 2) e retorna a resposta de login.

**Erros esperados:**
- Slug ou CNPJ duplicado → `409 Conflict` com mensagem indicando o campo.
- Plano inexistente ou `active = false` → `400 Bad Request`.
- Senha fora da política → `400 Bad Request`.
- Constraint `UNIQUE` do banco como última linha de defesa contra race condition de slug/CNPJ concorrente (a validação em memória sozinha não é suficiente).

---

## 3. Frontend — wizard de cadastro

Nova rota pública `/signup-academia`, substituindo o fluxo atual de `RegisterView.vue`. Estrutura de 3 passos com barra de progresso, estado local (sem Pinia — é local à tela, só vai para o `auth.store.ts` depois do sucesso).

**Passo 1 — Conta admin:** nome, e-mail, senha, confirmar senha (reaproveita campos/validações já existentes em `RegisterView.vue`).

**Passo 2 — Dados da empresa:** razão social, CNPJ (com máscara), telefone (com máscara), slug (auto-sugerido a partir do nome da empresa, editável, com checagem de disponibilidade via debounce em `GET /api/public/check-slug`).

**Passo 3 — Plano e pagamento:** cards com os planos vindos de `GET /api/public/saas-plans` (seleção única). Campos de pagamento condicionais à forma escolhida:
- `CREDIT_CARD`: nome no cartão (sem coletar número — sem gateway real, é só intenção/preferência registrada).
- `PIX`: nenhum campo extra (chave Pix exibida depois no painel de billing).
- `BOLETO`: nenhum campo extra além do CNPJ já capturado no passo 2.

Botão final "Criar conta" chama `POST /api/public/signup` com os dados acumulados. Sucesso → salva token/tenantId no `auth.store.ts` (mesma lógica do login) → redireciona para o dashboard ADMIN. Erro `409` (slug/CNPJ duplicado) → volta para o passo 2 com mensagem inline.

---

## 4. Encaixe no plano multi-tenant existente

Novas tasks a acrescentar em `docs/superpowers/plans/2026-07-21-multi-tenant.md`, dependentes das Tasks 1–9:

- **Task 10:** `SaasPlan` entity + migration `V32` + CRUD `SUPER_ADMIN` (`/api/super/saas-plans`)
- **Task 11:** `TenantSubscription` + `TenantCharge` entities + migration `V33` (inclui o seed ACTIVE do tenant "Personal Studio", combinado com o ajuste na Task 1)
- **Task 12:** `SignupService` + `POST /api/public/signup` + `GET /api/public/saas-plans` + `GET /api/public/check-slug`
- **Task 13:** Frontend — wizard de 3 passos em `/signup-academia`

Cada task segue o mesmo formato TDD do restante do plano (teste que falha → implementação → teste passa → commit).

### Edge cases a cobrir nos testes
- Slug/CNPJ duplicado → `409`, transação principal não deixa `Tenant` órfão sem `User`/`TenantSubscription`.
- Signup concorrente com o mesmo slug/CNPJ → constraint `UNIQUE` no banco pega o que a validação em memória não pegar.
- Falha ao criar `TenantCharge` não impede a criação do tenant/admin (best-effort, conforme decisão acima) — subscription fica sem cobrança inicial, precisa de reconciliação manual pelo `SUPER_ADMIN`.
- Backfill do tenant "Personal Studio": `TenantSubscription` com `status = ACTIVE` e `trialEndsAt = NULL`, sem `TenantCharge` retroativa.
