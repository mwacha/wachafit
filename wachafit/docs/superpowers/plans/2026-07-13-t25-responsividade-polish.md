# T-25: Responsividade e Polish — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Corrigir dialogs quebrados em mobile, adicionar feedback inline de sucesso após mutações, e inserir mensagens de empty state faltantes.

**Architecture:** Alterações puramente de UI em 6 views existentes — sem novas rotas, stores ou serviços. Cada task toca arquivos diferentes (exceto ExercisesView, que aparece em Task 1 e Task 2 em linhas distintas). Não há suite de testes automatizados no frontend; verificação é manual no browser com `npm run dev` em `frontend/`.

**Tech Stack:** Vue 3 + TypeScript, PrimeVue (Aura), Vite dev server (`cd frontend && npm run dev`)

## Global Constraints

- PrimeVue components são importados individualmente (nunca globais): `import Button from 'primevue/button'`
- Padrão de dialog responsivo do projeto: `style="width: min(Xpx, 95vw)"` — nunca `width: Xpx` sozinho
- Feedback inline: `ref<string>('')` por componente + `setTimeout 3000ms` auto-clear — sem Toast, sem store global
- CSS scoped em todos os componentes Vue; classes utilitárias Tailwind permitidas em partes existentes
- Mensagens de sucesso com cor `#22c55e` e classe `.success-msg`
- Mensagens de empty state com `var(--p-text-muted-color, #9ca3af)` e classe `.empty-msg`
- Commits em português, prefixo `fix:` ou `feat:`

---

### Task 1: Dialog width fixes

**Files:**
- Modify: `frontend/src/views/student/GoalsView.vue:28`
- Modify: `frontend/src/views/student/WorkoutView.vue:35`
- Modify: `frontend/src/views/exercises/ExercisesView.vue:54`
- Modify: `frontend/src/views/trainer/StudentOverviewView.vue:66`
- Modify: `frontend/src/views/trainer/StudentOverviewView.vue:105`

**Interfaces:**
- Consumes: nada de tasks anteriores
- Produces: nada que tasks posteriores dependam

- [ ] **Step 1: Corrigir GoalsView.vue**

Linha 28, trocar:
```html
<Dialog v-model:visible="showCreate" header="Nova Meta" :modal="true" style="width: 420px">
```
por:
```html
<Dialog v-model:visible="showCreate" header="Nova Meta" :modal="true" style="width: min(420px, 95vw)">
```

- [ ] **Step 2: Corrigir WorkoutView.vue**

Linha 35, trocar:
```html
<Dialog v-model:visible="showLog" header="Registrar Execução" :modal="true" style="width: 380px">
```
por:
```html
<Dialog v-model:visible="showLog" header="Registrar Execução" :modal="true" style="width: min(380px, 95vw)">
```

- [ ] **Step 3: Corrigir ExercisesView.vue**

Linha 54, trocar:
```html
<Dialog v-model:visible="showDialog" :header="editingId ? 'Editar exercício' : 'Novo exercício'"
  :modal="true" style="width: 460px">
```
por:
```html
<Dialog v-model:visible="showDialog" :header="editingId ? 'Editar exercício' : 'Novo exercício'"
  :modal="true" style="width: min(460px, 95vw)">
```

- [ ] **Step 4: Corrigir StudentOverviewView.vue — dois dialogs**

Linha 66, trocar:
```html
<Dialog v-model:visible="showAssessment" header="Nova Avaliação" :modal="true" style="width: 480px">
```
por:
```html
<Dialog v-model:visible="showAssessment" header="Nova Avaliação" :modal="true" style="width: min(480px, 95vw)">
```

Linha 105, trocar:
```html
<Dialog v-model:visible="showGoal" header="Nova Meta" :modal="true" style="width: 420px">
```
por:
```html
<Dialog v-model:visible="showGoal" header="Nova Meta" :modal="true" style="width: min(420px, 95vw)">
```

- [ ] **Step 5: Verificar no browser**

```bash
cd frontend && npm run dev
```

Abrir cada tela em viewport 375px (DevTools → responsive). Confirmar que os dialogs se abrem sem ultrapassar a tela.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/views/student/GoalsView.vue \
        frontend/src/views/student/WorkoutView.vue \
        frontend/src/views/exercises/ExercisesView.vue \
        frontend/src/views/trainer/StudentOverviewView.vue
git commit -m "fix: dialogs responsivos com min(Xpx, 95vw) em 5 views"
```

---

### Task 2: Empty states

**Files:**
- Modify: `frontend/src/views/student/RecordsView.vue:6`
- Modify: `frontend/src/views/exercises/ExercisesView.vue:20`
- Modify: `frontend/src/views/student/WorkoutView.vue:18`

**Interfaces:**
- Consumes: nada de tasks anteriores (Task 1 altera linhas diferentes do WorkoutView e ExercisesView)
- Produces: nada que tasks posteriores dependam

**Contexto:** RecordsView e ExercisesView usam `<DataTable>` do PrimeVue, que aceita a prop `emptyMessage` para exibir texto quando não há dados. WorkoutView já tem empty state mas com texto diferente do spec.

- [ ] **Step 1: RecordsView — adicionar emptyMessage ao DataTable**

Linha 6, trocar:
```html
<DataTable :value="workoutStore.records" :loading="workoutStore.loading" stripedRows>
```
por:
```html
<DataTable :value="workoutStore.records" :loading="workoutStore.loading" stripedRows
  emptyMessage="Nenhum recorde registrado.">
```

- [ ] **Step 2: ExercisesView — adicionar emptyMessage ao DataTable**

Linha 20, trocar:
```html
<DataTable :value="exercises" :loading="loading" stripedRows>
```
por:
```html
<DataTable :value="exercises" :loading="loading" stripedRows
  emptyMessage="Nenhum exercício encontrado.">
```

- [ ] **Step 3: WorkoutView — atualizar texto do empty state**

Linha 18, trocar:
```html
<div v-else-if="!workoutStore.activePlan" class="text-surface-400">Nenhuma ficha ativa.</div>
```
por:
```html
<div v-else-if="!workoutStore.activePlan" class="text-surface-400">Você não tem uma ficha de treino ativa.</div>
```

- [ ] **Step 4: Verificar no browser**

Com `npm run dev` rodando, logar como STUDENT. Navegar em:
- `/student/workout` — confirmar texto "Você não tem uma ficha de treino ativa." quando sem ficha
- `/student/records` — confirmar texto "Nenhum recorde registrado." quando lista vazia
- `/exercises` — confirmar texto "Nenhum exercício encontrado." quando busca retorna zero resultados (buscar algo inexistente, ex: "zzzzz")

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/student/RecordsView.vue \
        frontend/src/views/exercises/ExercisesView.vue \
        frontend/src/views/student/WorkoutView.vue
git commit -m "fix: empty states em RecordsView, ExercisesView e WorkoutView"
```

---

### Task 3: Inline success feedback

**Files:**
- Modify: `frontend/src/views/student/GoalsView.vue`
- Modify: `frontend/src/views/exercises/ExercisesView.vue`
- Modify: `frontend/src/views/trainer/WorkoutPlanView.vue`
- Modify: `frontend/src/views/trainer/StudentOverviewView.vue`

**Interfaces:**
- Consumes: nada de tasks anteriores
- Produces: nada

**Padrão a aplicar em cada view:**

Script:
```ts
const successMsg = ref('')
function showSuccess(msg: string) {
  successMsg.value = msg
  setTimeout(() => { successMsg.value = '' }, 3000)
}
```

Template (inserir próximo à ação correspondente):
```html
<p v-if="successMsg" class="success-msg">{{ successMsg }}</p>
```

CSS (adicionar no `<style scoped>`):
```css
.success-msg { color: #22c55e; font-size: 0.875rem; margin-top: 0.5rem; }
```

---

#### GoalsView.vue

Ações: mudar status de meta → "Status atualizado.", criar meta → "Meta criada."

- [ ] **Step 1: Adicionar successMsg ao script**

No bloco `<script setup lang="ts">`, após `const menuRefs = ref<Record<string, any>>({})` (linha 59), adicionar:

```ts
const successMsg = ref('')
function showSuccess(msg: string) {
  successMsg.value = msg
  setTimeout(() => { successMsg.value = '' }, 3000)
}
```

- [ ] **Step 2: Chamar showSuccess em changeStatus**

Trocar a função `changeStatus` (linhas 92–96):
```ts
async function changeStatus(id: string, status: GoalStatus) {
  const updated = await goalService.updateStatus(id, status)
  const idx = goals.value.findIndex(g => g.id === id)
  if (idx !== -1) goals.value[idx] = updated
}
```
por:
```ts
async function changeStatus(id: string, status: GoalStatus) {
  const updated = await goalService.updateStatus(id, status)
  const idx = goals.value.findIndex(g => g.id === id)
  if (idx !== -1) goals.value[idx] = updated
  showSuccess('Status atualizado.')
}
```

- [ ] **Step 3: Chamar showSuccess em submitCreate**

Trocar `showCreate.value = false` (dentro de `submitCreate`) por:
```ts
showCreate.value = false
showSuccess('Meta criada.')
```

O bloco final de `submitCreate` fica:
```ts
async function submitCreate() {
  saving.value = true
  try {
    const g = await goalService.create(authStore.userId!, {
      description: form.value.description,
      metric: form.value.metric || undefined,
      targetValue: form.value.targetValue ?? undefined,
    })
    goals.value.unshift(g)
    showCreate.value = false
    form.value = { description: '', metric: '', targetValue: null }
    showSuccess('Meta criada.')
  } finally { saving.value = false }
}
```

- [ ] **Step 4: Adicionar elemento de feedback no template**

Após `</div>` que fecha o `div.goals-list` (linha 26) e antes de `<Dialog`, inserir:
```html
<p v-if="successMsg" class="success-msg">{{ successMsg }}</p>
```

- [ ] **Step 5: Adicionar CSS**

No `<style scoped>`, após `.empty-state { ... }`, adicionar:
```css
.success-msg { color: #22c55e; font-size: 0.875rem; margin-top: 0; }
```

---

#### ExercisesView.vue

Ações: criar → "Exercício criado.", editar → "Exercício atualizado.", desativar → "Exercício desativado."

- [ ] **Step 6: Adicionar successMsg ao script**

Após `const deactivatingId = ref<string | null>(null)` (linha 105), adicionar:

```ts
const successMsg = ref('')
function showSuccess(msg: string) {
  successMsg.value = msg
  setTimeout(() => { successMsg.value = '' }, 3000)
}
```

- [ ] **Step 7: Chamar showSuccess em submitForm**

Trocar `showDialog.value = false` (dentro do `if/else` de `submitForm`) por chamadas específicas:
```ts
async function submitForm() {
  saving.value = true
  try {
    const payload = {
      name: form.value.name,
      muscleGroup: form.value.muscleGroup,
      description: form.value.description || undefined,
      videoUrl: form.value.videoUrl || undefined,
    }
    if (editingId.value) {
      const updated = await exerciseService.update(editingId.value, payload)
      const idx = exercises.value.findIndex(e => e.id === editingId.value)
      if (idx !== -1) exercises.value[idx] = updated
      showDialog.value = false
      showSuccess('Exercício atualizado.')
    } else {
      const created = await exerciseService.create(payload)
      exercises.value.unshift(created)
      showDialog.value = false
      showSuccess('Exercício criado.')
    }
  } finally { saving.value = false }
}
```

- [ ] **Step 8: Chamar showSuccess em deactivate**

Trocar a função `deactivate` por:
```ts
async function deactivate(id: string) {
  deactivatingId.value = id
  try {
    await exerciseService.deactivate(id)
    const idx = exercises.value.findIndex(e => e.id === id)
    if (idx !== -1) exercises.value[idx] = { ...exercises.value[idx], active: false }
    showSuccess('Exercício desativado.')
  } finally { deactivatingId.value = null }
}
```

- [ ] **Step 9: Adicionar elemento de feedback no template**

Após o `</DataTable>` (linha 50) e antes do `<Dialog`, inserir:
```html
<p v-if="successMsg" class="success-msg">{{ successMsg }}</p>
```

- [ ] **Step 10: Adicionar CSS**

No `<style scoped>`, ao final, adicionar:
```css
.success-msg { color: #22c55e; font-size: 0.875rem; margin-top: 0; }
```

---

#### WorkoutPlanView.vue

Ações: salvar ficha → "Ficha salva.", ativar ficha → "Ficha ativada."

- [ ] **Step 11: Adicionar successMsg ao script**

Após `const saveError = ref<string | null>(null)` (linha 121), adicionar:

```ts
const successMsg = ref('')
function showSuccess(msg: string) {
  successMsg.value = msg
  setTimeout(() => { successMsg.value = '' }, 3000)
}
```

- [ ] **Step 12: Chamar showSuccess em savePlan**

No final do bloco `try` de `savePlan` (após o `if/else`), adicionar `showSuccess('Ficha salva.')`:
```ts
async function savePlan() {
  if (!editor.value.name.trim()) { saveError.value = 'Nome da ficha é obrigatório.'; return }
  saving.value = true; saveError.value = null
  try {
    if (selectedPlanId.value) {
      const updated = await api.put<WorkoutPlan>(`/api/workout-plans/${selectedPlanId.value}`, buildPayload()).then(r => r.data)
      const idx = plans.value.findIndex(p => p.id === selectedPlanId.value)
      if (idx !== -1) plans.value[idx] = updated
    } else {
      const created = await workoutService.createPlan(studentId, buildPayload())
      plans.value.unshift(created)
      selectedPlanId.value = created.id
    }
    showSuccess('Ficha salva.')
  } catch (e: any) {
    saveError.value = e.response?.data?.message ?? 'Erro ao salvar.'
  } finally { saving.value = false }
}
```

- [ ] **Step 13: Chamar showSuccess em activateCurrent**

Trocar `activateCurrent` por:
```ts
async function activateCurrent() {
  if (!selectedPlanId.value) return
  activating.value = true
  try {
    const updated = await workoutService.activatePlan(selectedPlanId.value)
    plans.value = plans.value.map(p => ({ ...p, active: p.id === updated.id }))
    showSuccess('Ficha ativada.')
  } finally { activating.value = false }
}
```

- [ ] **Step 14: Adicionar elemento de feedback no template**

No `editor-footer` (após o `<Tag v-if="activePlan?.active" .../>`, linha 89) e antes do `<p v-if="saveError"`, inserir:
```html
<p v-if="successMsg" class="success-msg">{{ successMsg }}</p>
```

O bloco do footer fica:
```html
<div class="editor-footer">
  <Button label="Salvar" :loading="saving" @click="savePlan" />
  <Button v-if="selectedPlanId && !activePlan?.active"
    label="Ativar ficha" severity="success" outlined :loading="activating"
    @click="activateCurrent" />
  <Tag v-if="activePlan?.active" severity="success" value="Ficha ativa" />
</div>
<p v-if="successMsg" class="success-msg">{{ successMsg }}</p>
<p v-if="saveError" class="text-red-500 text-sm mt-1">{{ saveError }}</p>
```

- [ ] **Step 15: Adicionar CSS**

No `<style scoped>`, ao final, adicionar:
```css
.success-msg { color: #22c55e; font-size: 0.875rem; margin-top: 0.25rem; }
```

---

#### StudentOverviewView.vue

Ações: registrar avaliação → "Avaliação registrada.", criar meta → "Meta criada.", ativar ficha → "Ficha ativada.", mudar status de meta → "Status atualizado."

- [ ] **Step 16: Adicionar successMsg ao script**

Após `const saving = ref(false)` (linha 147), adicionar:

```ts
const successMsg = ref('')
function showSuccess(msg: string) {
  successMsg.value = msg
  setTimeout(() => { successMsg.value = '' }, 3000)
}
```

- [ ] **Step 17: Chamar showSuccess nas 4 funções**

Função `changeGoalStatus` — adicionar `showSuccess('Status atualizado.')`:
```ts
async function changeGoalStatus(id: string, status: GoalStatus) {
  const updated = await goalService.updateStatus(id, status)
  const idx = goals.value.findIndex(g => g.id === id)
  if (idx !== -1) goals.value[idx] = updated
  showSuccess('Status atualizado.')
}
```

Função `submitAssessment` — adicionar `showSuccess('Avaliação registrada.')` após `await assessmentStore.fetchAssessments(studentId)`:
```ts
async function submitAssessment() {
  saving.value = true
  try {
    await assessmentService.create(studentId, {
      assessedAt: new Date().toISOString().split('T')[0],
      weightKg: aForm.value.weightKg ?? undefined,
      heightCm: aForm.value.heightCm ?? undefined,
      bodyFatPct: aForm.value.bodyFatPct ?? undefined,
      notes: aForm.value.notes || undefined,
      measurements: aForm.value.measurements
        .filter(m => m.bodyPart && m.valueCm != null)
        .map(m => ({ bodyPart: m.bodyPart, valueCm: m.valueCm as number })),
    })
    showAssessment.value = false
    aForm.value = { weightKg: null, heightCm: null, bodyFatPct: null, notes: '', measurements: [] }
    await assessmentStore.fetchAssessments(studentId)
    showSuccess('Avaliação registrada.')
  } finally { saving.value = false }
}
```

Função `submitGoal` — adicionar `showSuccess('Meta criada.')` após `showGoal.value = false`:
```ts
async function submitGoal() {
  saving.value = true
  try {
    const g = await goalService.create(studentId, {
      description: gForm.value.description,
      metric: gForm.value.metric || undefined,
    })
    goals.value.unshift(g)
    showGoal.value = false
    gForm.value = { description: '', metric: '' }
    showSuccess('Meta criada.')
  } finally { saving.value = false }
}
```

Função `activatePlan` — adicionar `showSuccess('Ficha ativada.')`:
```ts
async function activatePlan(planId: string) {
  activatingPlan.value = planId
  try {
    const updated = await workoutService.activatePlan(planId)
    plans.value = plans.value.map(p => ({ ...p, active: p.id === updated.id }))
    showSuccess('Ficha ativada.')
  } finally { activatingPlan.value = null }
}
```

- [ ] **Step 18: Adicionar elemento de feedback no template**

Após o fechamento `</TabView>` (linha 63) e antes do `<Dialog v-model:visible="showAssessment"`, inserir:
```html
<p v-if="successMsg" class="success-msg">{{ successMsg }}</p>
```

- [ ] **Step 19: Adicionar CSS**

No `<style scoped>`, após `.field-label { ... }`, adicionar:
```css
.success-msg { color: #22c55e; font-size: 0.875rem; margin-top: 0; }
```

- [ ] **Step 20: Verificar no browser**

Com `npm run dev` rodando, testar:
1. GoalsView: mudar status de uma meta → mensagem verde aparece e some em 3s
2. GoalsView: criar nova meta → mensagem "Meta criada." após fechar dialog
3. ExercisesView: criar/editar exercício → mensagem aparece abaixo da tabela
4. ExercisesView: desativar (ADMIN) → mensagem "Exercício desativado."
5. WorkoutPlanView: salvar ficha → mensagem "Ficha salva." no footer
6. WorkoutPlanView: ativar ficha inativa → mensagem "Ficha ativada."
7. StudentOverviewView: registrar avaliação → mensagem após fechar dialog
8. StudentOverviewView: criar meta → mensagem "Meta criada."
9. StudentOverviewView: ativar ficha de treino → mensagem "Ficha ativada."
10. StudentOverviewView: mudar status de meta → mensagem "Status atualizado."

- [ ] **Step 21: Commit**

```bash
git add frontend/src/views/student/GoalsView.vue \
        frontend/src/views/exercises/ExercisesView.vue \
        frontend/src/views/trainer/WorkoutPlanView.vue \
        frontend/src/views/trainer/StudentOverviewView.vue
git commit -m "feat: feedback inline de sucesso após mutações (T-25)"
```
