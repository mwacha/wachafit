# Design — Conta única com seleção de academia (multi-membership)

**Contexto:** Hoje (após o plano multi-tenant, `docs/superpowers/plans/2026-07-21-multi-tenant.md`), o login exige que o usuário digite o `tenantSlug` da academia, porque `User.email` é único apenas por `(email, tenant_id)` — não existe forma de saber em qual academia procurar o e-mail sem essa informação. Se a mesma pessoa (mesmo e-mail) atua em duas academias, hoje isso exige duas linhas de `User` totalmente independentes, cada uma com sua própria senha.

Este design substitui essa exigência: login por e-mail+senha únicos, e a pessoa escolhe entre as academias às quais está associada — via popup logo após o login e/ou um seletor permanente no header.

---

## 1. Modelo de dados

### `Account` (tabela `accounts`) — nova
Identidade global de login, separada do vínculo com qualquer academia específica.

- `id UUID`
- `name VARCHAR(120)`
- `email VARCHAR(160) UNIQUE NOT NULL` (único **globalmente**, diferente da constraint atual de `users.email`)
- `password_hash VARCHAR(255) NOT NULL`
- `active BOOLEAN NOT NULL DEFAULT true`
- `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`

### `User` (já existe) — passa a ser só o vínculo com uma academia
- Remove `email` e `password_hash`
- Adiciona `account_id UUID NOT NULL` (FK para `accounts`)
- Mantém `tenant_id`, `role`, `active`, `created_at` exatamente como hoje

**Nenhuma outra tabela muda.** As ~30 referências existentes a `User.id` (`studentId`, `trainerId`, `createdBy`, `assessedBy`, `uploadedBy`, etc., em `Booking`, `PhysicalAssessment`, `WorkoutPlan`, `GroupClass` e todo o resto) continuam significando exatamente o que já significam hoje: "o vínculo desta pessoa nesta academia". `UserDetailsServiceImpl.loadUserByUsername(userId)` continua carregando um `User` (vínculo) por seu próprio id, sem nenhuma mudança — todo o mecanismo de isolamento por tenant (JWT com `tenantId`, `TenantContext`, `TenantFilterAspect`, as correções de segurança das Tasks 14-16) permanece válido sem alteração.

### `PasswordResetToken` — passa a apontar para `Account`
Hoje aponta para `User` (vínculo). Como a senha passa a ser única por pessoa (não por academia), o reset de senha também precisa ser único por pessoa: `PasswordResetToken.user` (FK para `users.id`) vira `PasswordResetToken.account` (FK para `accounts.id`).

### Migration de dados existentes
Cria um `Account` por **e-mail distinto** já existente em `users` (não um por `User`) — `accounts.email` é único globalmente, então dois `User`s que hoje já compartilham o mesmo e-mail (em tenants diferentes) precisam necessariamente apontar para o **mesmo** `Account` após a migração; criar dois `Account`s com o mesmo e-mail violaria a constraint. Para cada e-mail distinto, o `Account` herda `name`/`password_hash` de um dos `User`s daquele e-mail (qualquer um — na prática, o mais antigo); todos os `User`s daquele e-mail recebem o `account_id` desse `Account`. Ninguém perde acesso, nenhuma senha muda para quem hoje já tem e-mail único (a esmagadora maioria); quem já dividia e-mail entre tenants passa a logar com a senha de um dos dois registros (a ser resolvido manualmente pelo suporte apenas nesse caso raro, indicado num relatório de migração).

---

## 2. Fluxo de login e seleção de academia

### `POST /api/auth/login`
Request: `{ email, password }` (sem `tenantSlug`).

1. Busca `Account` pelo e-mail, confere a senha. Se `account.active == false`, mesmo com senha certa: `401 Unauthorized` (mesma mensagem genérica de credenciais inválidas).
2. Busca todos os `User` (vínculos) ativos (`user.active == true`) ligados a essa conta, em qualquer academia.
3. **1 vínculo:** loga direto — devolve o token completo de sempre: `{ token, role, userId, tenantId }`. Zero mudança de experiência para quem só tem uma academia.
4. **Mais de 1 vínculo:** devolve `{ selectTenantToken, memberships: [{ tenantId, tenantName, tenantSlug, role }, ...] }`. `selectTenantToken` é um token de curta duração (poucos minutos) que prova apenas "esta senha já foi conferida" — não carrega `tenantId`/`role`, então não abre acesso a nenhum endpoint protegido.
5. **0 vínculos ativos:** `401 Unauthorized` (mesma mensagem genérica de credenciais inválidas, para não revelar que a conta existe mas está sem vínculos ativos).

### `POST /api/auth/select-tenant`
Request: `{ selectTenantToken, tenantId }`.

Valida o `selectTenantToken`, confirma que `tenantId` está entre os vínculos da conta, emite o token completo (mesmo formato de sempre) para aquele vínculo específico. É aqui que aparece o popup pós-login.

### `POST /api/auth/switch-tenant` (autenticado)
Request: `{ tenantId }`.

Usa a conta já autenticada (o JWT precisa carregar `accountId`, além de `userId`/`role`/`tenantId` de hoje), confirma que `tenantId` é um vínculo válido da conta, emite um token novo já trocado — sem repetir a senha. É o que o seletor do header aciona.

### `GET /api/auth/my-tenants` (autenticado)
Retorna a lista de academias/vínculos da conta atual (`[{ tenantId, tenantName, tenantSlug, role }, ...]`), para popular tanto o popup quanto o seletor do header.

---

## 3. Cadastro com e-mail já existente

Ao criar um `User` novo (seja por `/register`, seja por um admin/recepcionista cadastrando aluno/trainer): se o e-mail já tiver `Account`, ignora a senha digitada no formulário e apenas cria o vínculo novo, ligado à conta existente. A pessoa continua usando a senha que já tinha.

`/register` (autocadastro de aluno numa academia já existente) **continua pedindo `tenantSlug`** — isso não muda, é uma pergunta diferente ("qual academia você está entrando" vs. "qual das minhas academias eu quero usar agora").

---

## 4. Consequências em peças existentes

- **JWT:** ganha a claim `accountId`. Tokens emitidos antes deste deploy, sem essa claim, são tratados como inválidos (mesma regra de segurança já aplicada hoje para `tenantId` ausente — força novo login; nenhum token novo pode nascer sem ela).
- **`SUPER_ADMIN`:** passa a ter conta+vínculo como qualquer pessoa, sem caso especial no login. Continua bypassando o filtro de tenant nos dados.
- **Frontend `LoginView.vue`:** perde o campo de slug. A resposta do login pode vir pronta (1 vínculo) ou disparar o popup de seleção (mais de 1).
- **Componente novo — seletor de academia:** usado tanto no popup pós-login quanto embutido no header (`AppLayout.vue`, entre a busca e o botão de sair). **Sempre visível**, mesmo para quem só tem uma academia (mostra o nome atual; vira dropdown funcional só quando há mais de uma) — comportamento consistente para todos os usuários, sem o botão aparecer/sumir dependendo da pessoa.

---

## 5. Fora de escopo (não coberto por este design)

- Convite por e-mail para uma pessoa se auto-associar a uma academia (hoje, o vínculo é sempre criado por um admin/recepcionista já dentro da academia, ou pelo próprio `/register`).
- Trocar e-mail/senha da conta a partir de dentro de uma academia específica — ainda não desenhado onde essa tela viveria.
- Aviso manual para os casos (raros) em que dois `User`s hoje já compartilhavam e-mail entre tenants: a migração os une automaticamente num só `Account` (ver seção 1), mas não avisa proativamente a pessoa de que sua senha pode ter mudado para uma das duas contas — isso fica para uma ação manual de suporte, fora deste plano.
