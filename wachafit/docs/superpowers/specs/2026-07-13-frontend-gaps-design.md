# Design — Preenchimento de gaps do frontend

**Data:** 2026-07-13
**Status:** Aprovado

---

## Contexto

O backend possui controllers completos para avaliações, fichas de treino, exercícios, metas, fotos e perfis de aluno/trainer. O frontend cobre a maioria desses endpoints, mas tem lacunas funcionais identificadas abaixo.

---

## Escopo

### Seção 1 — Correções em views existentes

#### 1.1 Nomes de exercício (`WorkoutView`, `RecordsView`)
Ambas mostram `exerciseId.slice(0,8)` em vez do nome. Correção: no `onMounted` chamar `exerciseService.search()` sem filtros e construir um mapa `{ [id]: name }` local. Substituir todas as ocorrências de ID bruto pelo nome resolvido.

#### 1.2 Status de metas (`GoalsView`)
Adicionar menu de ações por meta com 3 opções (IN_PROGRESS / ACHIEVED / EXPIRED) via `Menu` ou `SplitButton` do PrimeVue. Ao selecionar, chama `PATCH /api/goals/{id}/status`. O `Tag` de status é atualizado localmente após sucesso.

#### 1.3 Medidas corporais na avaliação (`StudentOverviewView`)
O form de nova avaliação só captura peso/altura/gordura. Adicionar seção "Medidas" com lista dinâmica:
- Botão "+ Adicionar medida" insere par (`bodyPart`: text, `valueCm`: number)
- Botão de lixeira remove item da lista
- Array `measurements[]` enviado no POST já é aceito pelo backend

---

### Seção 2 — Views novas simples

#### 2.1 `/student/profile` — Perfil do aluno (leitura)
- **Role:** `STUDENT`
- **Menu:** link "Meu Perfil" no sidebar do aluno
- **Layout:** dois cards verticais
  - **Dados pessoais:** CPF, data de nascimento, gênero, telefone, endereço completo, contato de emergência — de `GET /api/students/{userId}/profile`
  - **Saúde & PAR-Q:** condições pré-existentes (flags booleanas), medicamentos, restrições físicas, nível de atividade, objetivo de fitness, assinatura PAR-Q (data) — de `GET /api/students/{userId}/health`
- Campos `null` exibem "—". Nenhum campo editável.

#### 2.2 `/exercises` — Biblioteca de exercícios
- **Roles:** `TRAINER` e `ADMIN`
- **Menu:** link "Exercícios" no sidebar de ambos
- **Layout:** barra de busca (`q`) + dropdown de grupo muscular → tabela com colunas: Nome / Grupo Muscular / Descrição / Vídeo / Status (ativo/inativo)
- **TRAINER:** pode criar e editar via dialog (nome*, grupo muscular*, descrição, URL de vídeo)
- **ADMIN:** tudo do trainer + botão "Desativar" (`PATCH /api/exercises/{id}/deactivate`)
- Dialog único para criar e editar, reutilizado com `v-model:visible`

---

### Seção 3 — StudentOverviewView expandida

Converter layout atual (2 cards lado a lado) para **TabView com 3 abas**:

| Tab | Conteúdo |
|-----|---------|
| Avaliações | Conteúdo atual + seção de medidas corporais (Seção 1.3) |
| Metas | Conteúdo atual + mudança de status por meta |
| Fichas de Treino | Lista de fichas + navegação para builder |

**Tab "Fichas de Treino":**
- Carrega via `GET /api/students/{id}/workout-plans`
- Cada item mostra nome + badge (ATIVA / INATIVA) + botão "Ativar" (se inativa, via `PATCH /activate`) + botão "Ver/Editar" (navega para `/trainer/students/:id/workout?planId=xxx`)
- Botão "+ Nova Ficha" navega para `/trainer/students/:id/workout` sem `planId`

---

### Seção 4 — Workout Plan Builder (`/trainer/students/:id/workout`)

- **Role:** `TRAINER`
- **Rota:** `/trainer/students/:id/workout` com query param opcional `?planId=xxx`

**Layout dois painéis** (stack em mobile):

**Painel esquerdo — Lista de fichas**
- Header com nome do aluno (busca o user pelo `id`)
- Lista de fichas via `GET /api/students/{id}/workout-plans`: nome + badge ATIVA/INATIVA
- Clique na ficha popula o painel direito
- Botão "+ Nova Ficha" limpa o editor

**Painel direito — Editor da ficha**
- **Cabeçalho:** campos `name` (obrigatório) e `description` (opcional)
- **Tabela de itens:** uma linha por exercício com colunas:
  - Exercício (autocomplete com debounce 300ms em `GET /api/exercises?q=...`)
  - Divisão (texto livre: A, B, C…)
  - Séries (number)
  - Reps (text, ex: "10-12")
  - Carga sugerida (kg, number decimal)
  - Descanso (s, number)
  - Ordem (number)
  - Notas (text)
  - Ação (lixeira)
- Botão "+ Exercício" adiciona linha em branco
- **Footer:**
  - "Salvar" → `POST /api/students/{id}/workout-plans` (nova) ou `PUT /api/workout-plans/{planId}` (edição)
  - "Ativar" → `PATCH /api/workout-plans/{planId}/activate` (visível só em fichas inativas já salvas)

**Resolução de nomes ao carregar ficha existente:** buscar lista completa de exercícios e fazer lookup por `exerciseId` para popular o autocomplete corretamente.

---

## Rotas a adicionar no router

```
/student/profile             → roles: ['STUDENT']
/exercises                   → roles: ['TRAINER', 'ADMIN']
/trainer/students/:id/workout → roles: ['TRAINER']
```

## Links de menu a adicionar

| Role | Item | Rota |
|------|------|------|
| STUDENT | Meu Perfil | `/student/profile` |
| TRAINER | Exercícios | `/exercises` |
| ADMIN | Exercícios | `/exercises` |

## Arquivos a criar

- `frontend/src/views/student/ProfileView.vue`
- `frontend/src/views/exercises/ExercisesView.vue`
- `frontend/src/views/trainer/WorkoutPlanView.vue`

## Arquivos a editar

- `frontend/src/views/student/WorkoutView.vue` — resolver nomes de exercício
- `frontend/src/views/student/RecordsView.vue` — resolver nomes de exercício
- `frontend/src/views/student/GoalsView.vue` — adicionar mudança de status
- `frontend/src/views/trainer/StudentOverviewView.vue` — converter para TabView + medidas + fichas
- `frontend/src/router/index.ts` — adicionar 3 rotas novas
- `frontend/src/components/AppLayout.vue` — adicionar links de menu
