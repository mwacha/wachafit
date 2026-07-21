# Design — T-25: Responsividade e Polish

**Data:** 2026-07-13
**Status:** Aprovado

---

## Contexto

O frontend do wachafit (Vue 3 + TypeScript + PrimeVue Aura) tem três problemas de polish identificados: dialogs com width fixa que quebram em telas <420px, ausência de feedback visual após ações de mutação, e listas que ficam vazias sem mensagem explicativa ao usuário.

---

## Escopo

### Seção 1 — Dialog width fixes

5 dialogs com `style="width: Xpx"` hard-coded que quebram em mobile. Correção: substituir pelo padrão `min(Xpx, 95vw)` já adotado em outras views do projeto.

| Arquivo | Width atual | Width corrigida |
|---------|-------------|-----------------|
| `frontend/src/views/student/GoalsView.vue` | `420px` | `min(420px, 95vw)` |
| `frontend/src/views/student/WorkoutView.vue` | `380px` | `min(380px, 95vw)` |
| `frontend/src/views/exercises/ExercisesView.vue` | `460px` | `min(460px, 95vw)` |
| `frontend/src/views/trainer/StudentOverviewView.vue` | `480px` | `min(480px, 95vw)` |
| `frontend/src/views/trainer/StudentOverviewView.vue` | `420px` | `min(420px, 95vw)` |

---

### Seção 2 — Inline success feedback

Padrão a adotar em cada componente afetado:

```ts
const successMsg = ref('')

function showSuccess(msg: string) {
  successMsg.value = msg
  setTimeout(() => { successMsg.value = '' }, 3000)
}
```

Template:
```html
<p v-if="successMsg" class="success-msg">{{ successMsg }}</p>
```

CSS (scoped):
```css
.success-msg {
  color: #22c55e;
  font-size: 0.875rem;
  margin-top: 0.5rem;
}
```

**Views e ações:**

| View | Ação | Mensagem |
|------|------|----------|
| `GoalsView.vue` | Mudar status da meta | "Status atualizado." |
| `ExercisesView.vue` | Criar exercício | "Exercício criado." |
| `ExercisesView.vue` | Editar exercício | "Exercício atualizado." |
| `ExercisesView.vue` | Desativar exercício | "Exercício desativado." |
| `WorkoutPlanView.vue` | Salvar ficha | "Ficha salva." |
| `WorkoutPlanView.vue` | Ativar ficha | "Ficha ativada." |
| `StudentOverviewView.vue` | Adicionar avaliação | "Avaliação registrada." |
| `StudentOverviewView.vue` | Ativar ficha de treino | "Ficha ativada." |

O `successMsg` é local por componente — sem store global, sem Toast.

---

### Seção 3 — Empty states

Padrão: parágrafo com classe `.empty-msg` exibido via `v-if="!loading && lista.length === 0"`.

```css
.empty-msg {
  color: var(--p-text-muted-color, #9ca3af);
  font-size: 0.875rem;
  text-align: center;
  padding: 2rem 0;
}
```

**Views e mensagens:**

| View | Condição | Mensagem |
|------|----------|----------|
| `GoalsView.vue` | `!loading && goals.length === 0` | "Nenhuma meta definida." |
| `WorkoutView.vue` (aluno) | `!loading && !activePlan` | "Você não tem uma ficha de treino ativa." |
| `ExercisesView.vue` | `!loading && exercises.length === 0` | "Nenhum exercício encontrado." |
| `WorkoutPlanView.vue` (lista) | `!loading && plans.length === 0` | "Nenhuma ficha criada ainda." |
| `RecordsView.vue` | `!loading && records.length === 0` | "Nenhum recorde registrado." |

---

## Arquivos a modificar

- `frontend/src/views/student/GoalsView.vue`
- `frontend/src/views/student/WorkoutView.vue`
- `frontend/src/views/student/RecordsView.vue`
- `frontend/src/views/exercises/ExercisesView.vue`
- `frontend/src/views/trainer/WorkoutPlanView.vue`
- `frontend/src/views/trainer/StudentOverviewView.vue`

## Arquivos a criar

Nenhum.

---

## Critérios de aceite

- Nenhum dialog quebra em viewport de 320px
- Após toda ação de mutação, uma mensagem de sucesso verde aparece e some em 3 segundos
- Listas vazias exibem texto explicativo em vez de área em branco
