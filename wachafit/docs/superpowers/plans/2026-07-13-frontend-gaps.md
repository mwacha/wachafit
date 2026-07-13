# Frontend Gaps Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Criar as views e correções de frontend que cobrem todos os endpoints já existentes no backend, eliminando o gap entre API e UI.

**Architecture:** Views Vue 3 + Composition API seguindo os padrões do projeto (AppLayout wrapper, PrimeVue components, services existentes, Pinia stores). Nenhum novo serviço ou store é necessário — tudo reutiliza o que já existe.

**Tech Stack:** Vue 3, TypeScript, PrimeVue, Pinia, exerciseService / workoutService / goalService / profileService já disponíveis em `frontend/src/services/`.

## Global Constraints

- Todo componente envolve `<AppLayout>` como raiz do template
- Imports de PrimeVue são individuais por componente (sem auto-import global)
- IDs são `string` (UUID) — nunca `number`
- Textos para o usuário final em português brasileiro
- Seguir padrão de style scoped já usado nas views existentes
- Não criar novos services nem stores — usar os existentes

---

### Task 1: Rotas e links de menu

**Files:**
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/components/AppLayout.vue`

**Interfaces:**
- Produz: rotas `/student/profile`, `/exercises`, `/trainer/students/:id/workout` acessíveis

- [ ] **Step 1: Adicionar as 3 rotas em `router/index.ts`**

Inserir após o bloco `// --- Trainer routes ---` (linha ~138):

```ts
    {
      path: '/trainer/students/:id/workout',
      component: () => import('@/views/trainer/WorkoutPlanView.vue'),
      meta: { requiresAuth: true, roles: ['TRAINER'] as Role[] },
    },
```

Inserir após `/student/photos` (linha ~191):

```ts
    {
      path: '/student/profile',
      component: () => import('@/views/student/ProfileView.vue'),
      meta: { requiresAuth: true, roles: ['STUDENT'] as Role[] },
    },
```

Inserir após o bloco de admin schedules (linha ~67):

```ts
    {
      path: '/exercises',
      component: () => import('@/views/exercises/ExercisesView.vue'),
      meta: { requiresAuth: true, roles: ['TRAINER', 'ADMIN'] as Role[] },
    },
```

- [ ] **Step 2: Adicionar links no menu do TRAINER em `AppLayout.vue`**

No bloco `v-else-if="auth.role === 'TRAINER'"` (após o link `/trainer/profile`), adicionar:

```html
<RouterLink to="/exercises" class="nav-item" active-class="active" title="Exercícios" aria-label="Exercícios" @click="mobileOpen = false"><i class="pi pi-list" /></RouterLink>
```

- [ ] **Step 3: Adicionar link no menu do ADMIN em `AppLayout.vue`**

No bloco `v-if="auth.role === 'ADMIN'"` (após o link de comissões), adicionar:

```html
<RouterLink to="/exercises" class="nav-item" active-class="active" title="Exercícios" aria-label="Exercícios" @click="mobileOpen = false"><i class="pi pi-list" /></RouterLink>
```

- [ ] **Step 4: Adicionar link no menu do STUDENT em `AppLayout.vue`**

No bloco `v-else-if="auth.role === 'STUDENT'"` (após o link `/student/photos`), adicionar:

```html
<RouterLink to="/student/profile" class="nav-item" active-class="active" title="Meu Perfil" aria-label="Meu Perfil" @click="mobileOpen = false"><i class="pi pi-user" /></RouterLink>
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/router/index.ts frontend/src/components/AppLayout.vue
git commit -m "feat: rotas e links de menu para views de perfil, exercícios e workout builder"
```

---

### Task 2: Corrigir `exerciseService.deactivate` (método HTTP errado)

**Files:**
- Modify: `frontend/src/services/exercise.service.ts`

**Interfaces:**
- Consumes: nada
- Produz: `exerciseService.deactivate(id)` chama `PATCH /api/exercises/{id}/deactivate` corretamente

O serviço atual usa `api.delete` mas o backend expõe `PATCH /api/exercises/{id}/deactivate`.

- [ ] **Step 1: Corrigir o método deactivate**

Em `frontend/src/services/exercise.service.ts`, alterar a linha:

```ts
  deactivate: (id: string) => api.delete(`/api/exercises/${id}`),
```

Para:

```ts
  deactivate: (id: string) => api.patch(`/api/exercises/${id}/deactivate`),
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/services/exercise.service.ts
git commit -m "fix: exerciseService.deactivate usa PATCH em vez de DELETE"
```

---

### Task 3: Resolver nomes de exercícios em WorkoutView e RecordsView

**Files:**
- Modify: `frontend/src/views/student/WorkoutView.vue`
- Modify: `frontend/src/views/student/RecordsView.vue`

**Interfaces:**
- Consumes: `exerciseService.search()` → `Exercise[]` com campos `id: string`, `name: string`
- Produz: ambas as views exibem o nome do exercício em vez do UUID parcial

- [ ] **Step 1: Atualizar `WorkoutView.vue`**

Substituir o conteúdo completo de `frontend/src/views/student/WorkoutView.vue`:

```vue
<!-- frontend/src/views/student/WorkoutView.vue -->
<template>
  <AppLayout>
    <div class="p-6 max-w-3xl">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold">Meu Treino</h1>
        <Button
          v-if="workoutStore.activePlan"
          icon="pi pi-download"
          label="Baixar Ficha"
          class="p-button-outlined"
          :loading="downloadingPdf"
          @click="downloadWorkoutPdf"
        />
      </div>

      <div v-if="workoutStore.loading" class="text-center py-8">Carregando...</div>
      <div v-else-if="!workoutStore.activePlan" class="text-surface-400">Nenhuma ficha ativa.</div>
      <div v-else>
        <h2 class="text-lg font-semibold mb-3">{{ workoutStore.activePlan.name }}</h2>
        <div v-for="item in workoutStore.activePlan.items" :key="item.id"
             class="card p-4 mb-3 flex items-center justify-between">
          <div>
            <div class="font-medium">{{ exerciseNames[item.exerciseId] ?? 'Exercício' }}</div>
            <div class="text-sm text-surface-500">
              {{ item.division ? `Divisão ${item.division} — ` : '' }}
              {{ item.sets }}x{{ item.reps }}
              {{ item.suggestedLoadKg ? ` @ ${item.suggestedLoadKg}kg` : '' }}
            </div>
          </div>
          <Button icon="pi pi-plus" text size="small" label="Registrar" @click="openLog(item)" />
        </div>
      </div>

      <Dialog v-model:visible="showLog" header="Registrar Execução" :modal="true" style="width: 380px">
        <form @submit.prevent="submitLog" class="flex flex-col gap-3">
          <InputNumber v-model="logForm.sets" placeholder="Séries" :min="1" />
          <InputNumber v-model="logForm.reps" placeholder="Repetições" :min="1" />
          <InputNumber v-model="logForm.loadKg" placeholder="Carga (kg)" :minFractionDigits="1" />
          <InputText v-model="logForm.notes" placeholder="Notas (opcional)" />
          <p v-if="logError" class="text-red-500 text-sm">{{ logError }}</p>
          <Button type="submit" label="Salvar" :loading="saving" />
        </form>
      </Dialog>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useWorkoutStore } from '@/stores/workout.store'
import { useAuthStore } from '@/stores/auth.store'
import { workoutService } from '@/services/workout.service'
import { exerciseService } from '@/services/exercise.service'
import type { WorkoutPlanItem } from '@/types/api'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import api from '@/services/api'

const workoutStore = useWorkoutStore()
const authStore = useAuthStore()
const downloadingPdf = ref(false)
const showLog = ref(false)
const saving = ref(false)
const logError = ref<string | null>(null)
const currentItem = ref<WorkoutPlanItem | null>(null)
const logForm = ref({ sets: null as number | null, reps: null as number | null, loadKg: null as number | null, notes: '' })
const exerciseNames = ref<Record<string, string>>({})

onMounted(async () => {
  await workoutStore.fetchActivePlan(authStore.userId!)
  const exercises = await exerciseService.search()
  exerciseNames.value = Object.fromEntries(exercises.map(e => [e.id, e.name]))
})

async function downloadWorkoutPdf() {
  if (!authStore.userId) return
  downloadingPdf.value = true
  try {
    const res = await api.get(`/api/students/${authStore.userId}/pdf/workout`, { responseType: 'blob' })
    const url = URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }))
    const a = document.createElement('a')
    a.href = url
    a.download = 'ficha-treino.pdf'
    a.click()
    URL.revokeObjectURL(url)
  } finally {
    downloadingPdf.value = false
  }
}

function openLog(item: WorkoutPlanItem) { currentItem.value = item; showLog.value = true }

async function submitLog() {
  if (!currentItem.value) return
  saving.value = true; logError.value = null
  try {
    await workoutService.createLog(authStore.userId!, {
      exerciseId: currentItem.value.exerciseId,
      performedAt: new Date().toISOString().split('T')[0],
      sets: logForm.value.sets ?? undefined,
      reps: logForm.value.reps ?? undefined,
      loadKg: logForm.value.loadKg ?? undefined,
      notes: logForm.value.notes || undefined,
    })
    showLog.value = false
    logForm.value = { sets: null, reps: null, loadKg: null, notes: '' }
  } catch (e: any) {
    logError.value = e.response?.data?.message ?? 'Erro ao registrar'
  } finally { saving.value = false }
}
</script>
```

- [ ] **Step 2: Atualizar `RecordsView.vue`**

Substituir o conteúdo completo de `frontend/src/views/student/RecordsView.vue`:

```vue
<!-- frontend/src/views/student/RecordsView.vue -->
<template>
  <AppLayout>
    <div class="view-wrap">
      <h1 class="page-title">Recordes Pessoais</h1>
      <DataTable :value="workoutStore.records" :loading="workoutStore.loading" stripedRows>
        <Column header="Exercício" style="min-width:160px">
          <template #body="{ data }">{{ exerciseNames[data.exerciseId] ?? '—' }}</template>
        </Column>
        <Column header="Carga" style="min-width:100px">
          <template #body="{ data }">{{ data.recordLoadKg }} kg</template>
        </Column>
        <Column header="Conquistado em" style="min-width:130px">
          <template #body="{ data }">{{ data.achievedAt }}</template>
        </Column>
      </DataTable>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useWorkoutStore } from '@/stores/workout.store'
import { useAuthStore } from '@/stores/auth.store'
import { exerciseService } from '@/services/exercise.service'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'

const workoutStore = useWorkoutStore()
const authStore = useAuthStore()
const exerciseNames = ref<Record<string, string>>({})

onMounted(async () => {
  await workoutStore.fetchRecords(authStore.userId!)
  const exercises = await exerciseService.search()
  exerciseNames.value = Object.fromEntries(exercises.map(e => [e.id, e.name]))
})
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 16px; max-width: 700px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }
</style>
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/student/WorkoutView.vue frontend/src/views/student/RecordsView.vue
git commit -m "fix: exibir nome do exercício em vez de UUID em WorkoutView e RecordsView"
```

---

### Task 4: GoalsView — mudança de status da meta

**Files:**
- Modify: `frontend/src/views/student/GoalsView.vue`

**Interfaces:**
- Consumes: `goalService.updateStatus(id: string, status: string)` → `Goal`
- Produz: botão de menu por meta permite alterar status para IN_PROGRESS / ACHIEVED / EXPIRED

- [ ] **Step 1: Substituir `GoalsView.vue`**

```vue
<!-- frontend/src/views/student/GoalsView.vue -->
<template>
  <AppLayout>
    <div class="view-wrap">
      <div class="page-header">
        <h1 class="page-title">Minhas Metas</h1>
        <Button label="Nova meta" icon="pi pi-plus" @click="showCreate = true" />
      </div>

      <div v-if="loading" class="empty-state"><i class="pi pi-spin pi-spinner" /></div>
      <div v-else class="goals-list">
        <div v-for="g in goals" :key="g.id" class="goal-card">
          <div class="goal-info">
            <div class="goal-desc">{{ g.description }}</div>
            <div class="goal-meta">
              {{ g.metric ?? '' }}{{ g.targetValue ? ` — Alvo: ${g.targetValue}` : '' }}{{ g.targetDate ? ` até ${g.targetDate}` : '' }}
            </div>
          </div>
          <div class="goal-actions">
            <Tag :severity="goalSeverity(g.status)" :value="goalLabel(g.status)" />
            <Button icon="pi pi-ellipsis-v" text rounded size="small" @click="(e) => toggleMenu(e, g.id)" />
            <Menu :ref="el => menuRefs[g.id] = el as any" :model="menuItems(g)" :popup="true" />
          </div>
        </div>
        <div v-if="goals.length === 0" class="empty-state">Nenhuma meta registrada.</div>
      </div>

      <Dialog v-model:visible="showCreate" header="Nova Meta" :modal="true" style="width: 420px">
        <form @submit.prevent="submitCreate" class="flex flex-col gap-3 pt-2">
          <InputText v-model="form.description" placeholder="Descrição" required />
          <InputText v-model="form.metric" placeholder="Métrica (ex: weight, body_fat)" />
          <InputNumber v-model="form.targetValue" placeholder="Valor alvo" :minFractionDigits="1" />
          <Button type="submit" label="Salvar" :loading="saving" />
        </form>
      </Dialog>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { goalService } from '@/services/goal.service'
import { useAuthStore } from '@/stores/auth.store'
import type { Goal, GoalStatus } from '@/types/api'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Tag from 'primevue/tag'
import Menu from 'primevue/menu'

const authStore = useAuthStore()
const goals = ref<Goal[]>([])
const loading = ref(false)
const showCreate = ref(false)
const saving = ref(false)
const form = ref({ description: '', metric: '', targetValue: null as number | null })
const menuRefs = ref<Record<string, any>>({})

onMounted(async () => {
  loading.value = true
  try { goals.value = await goalService.list(authStore.userId!) }
  finally { loading.value = false }
})

function goalSeverity(status: string) {
  return status === 'ACHIEVED' ? 'success' : status === 'EXPIRED' ? 'danger' : 'info'
}
function goalLabel(status: string) {
  return status === 'ACHIEVED' ? 'Atingida' : status === 'EXPIRED' ? 'Expirada' : 'Em andamento'
}

function toggleMenu(event: Event, id: string) {
  menuRefs.value[id]?.toggle(event)
}

function menuItems(goal: Goal) {
  const statuses: { label: string; status: GoalStatus }[] = [
    { label: 'Em andamento', status: 'IN_PROGRESS' },
    { label: 'Atingida', status: 'ACHIEVED' },
    { label: 'Expirada', status: 'EXPIRED' },
  ]
  return statuses
    .filter(s => s.status !== goal.status)
    .map(s => ({
      label: s.label,
      command: () => changeStatus(goal.id, s.status),
    }))
}

async function changeStatus(id: string, status: GoalStatus) {
  const updated = await goalService.updateStatus(id, status)
  const idx = goals.value.findIndex(g => g.id === id)
  if (idx !== -1) goals.value[idx] = updated
}

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
  } finally { saving.value = false }
}
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 20px; max-width: 680px; }
.page-header { display: flex; align-items: center; justify-content: space-between; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }
.goals-list { display: flex; flex-direction: column; gap: 10px; }
.goal-card {
  background: #fff; border: 1px solid var(--neutral-200);
  border-radius: var(--radius-lg); padding: 14px 16px;
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  box-shadow: var(--shadow-card);
}
.goal-info { flex: 1; min-width: 0; }
.goal-desc { font-weight: 600; color: var(--neutral-900); font-size: 14px; }
.goal-meta { font-size: 12px; color: var(--neutral-500); margin-top: 2px; }
.goal-actions { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.empty-state { text-align: center; padding: 40px; color: var(--neutral-400); font-size: 14px; }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/views/student/GoalsView.vue
git commit -m "feat: mudança de status de metas em GoalsView"
```

---

### Task 5: StudentOverviewView — TabView + medidas + aba de fichas

**Files:**
- Modify: `frontend/src/views/trainer/StudentOverviewView.vue`

**Interfaces:**
- Consumes: `workoutService.listPlans(studentId)` → `WorkoutPlan[]`, `workoutService.activatePlan(planId)` → `WorkoutPlan`
- Produz: view com 3 abas: Avaliações (com medidas), Metas (com mudança de status), Fichas de Treino

- [ ] **Step 1: Substituir `StudentOverviewView.vue`**

```vue
<!-- frontend/src/views/trainer/StudentOverviewView.vue -->
<template>
  <AppLayout>
    <div class="view-wrap">
      <Button icon="pi pi-arrow-left" text label="Voltar" @click="$router.back()" class="mb-2" />
      <h1 class="page-title">Visão do Aluno</h1>

      <TabView>
        <!-- ── Tab Avaliações ── -->
        <TabPanel header="Avaliações">
          <div class="tab-content">
            <div class="tab-header">
              <span class="tab-count">{{ assessmentStore.assessments.length }} avaliação(ões)</span>
              <Button icon="pi pi-plus" label="Nova avaliação" size="small" @click="showAssessment = true" />
            </div>
            <div v-if="assessmentStore.assessments.length === 0" class="empty-state">Nenhuma avaliação.</div>
            <div v-for="a in assessmentStore.assessments" :key="a.id" class="list-item">
              <span>{{ a.assessedAt }}</span>
              <span>Peso: {{ a.weightKg ?? '—' }} kg</span>
              <span>BF: {{ a.bodyFatPct ?? '—' }}%</span>
              <span>IMC: {{ a.bmi ?? '—' }}</span>
            </div>
          </div>
        </TabPanel>

        <!-- ── Tab Metas ── -->
        <TabPanel header="Metas">
          <div class="tab-content">
            <div class="tab-header">
              <span class="tab-count">{{ goals.length }} meta(s)</span>
              <Button icon="pi pi-plus" label="Nova meta" size="small" @click="showGoal = true" />
            </div>
            <div v-if="goals.length === 0" class="empty-state">Nenhuma meta.</div>
            <div v-for="g in goals" :key="g.id" class="list-item">
              <span class="flex-1">{{ g.description }}</span>
              <Tag :severity="goalSeverity(g.status)" :value="goalLabel(g.status)" />
              <Button icon="pi pi-ellipsis-v" text rounded size="small" @click="(e) => toggleGoalMenu(e, g.id)" />
              <Menu :ref="el => goalMenuRefs[g.id] = el as any" :model="goalMenuItems(g)" :popup="true" />
            </div>
          </div>
        </TabPanel>

        <!-- ── Tab Fichas de Treino ── -->
        <TabPanel header="Fichas de Treino">
          <div class="tab-content">
            <div class="tab-header">
              <span class="tab-count">{{ plans.length }} ficha(s)</span>
              <Button icon="pi pi-plus" label="Nova ficha" size="small"
                @click="$router.push(`/trainer/students/${studentId}/workout`)" />
            </div>
            <div v-if="loadingPlans" class="empty-state"><i class="pi pi-spin pi-spinner" /></div>
            <div v-else-if="plans.length === 0" class="empty-state">Nenhuma ficha de treino.</div>
            <div v-for="p in plans" :key="p.id" class="list-item">
              <span class="flex-1 font-medium">{{ p.name }}</span>
              <Tag :severity="p.active ? 'success' : 'secondary'" :value="p.active ? 'Ativa' : 'Inativa'" />
              <Button v-if="!p.active" label="Ativar" size="small" outlined :loading="activatingPlan === p.id"
                @click="activatePlan(p.id)" />
              <Button icon="pi pi-pencil" text size="small"
                @click="$router.push(`/trainer/students/${studentId}/workout?planId=${p.id}`)" />
            </div>
          </div>
        </TabPanel>
      </TabView>

      <!-- Dialog: Nova Avaliação -->
      <Dialog v-model:visible="showAssessment" header="Nova Avaliação" :modal="true" style="width: 480px">
        <form @submit.prevent="submitAssessment" class="flex flex-col gap-3 pt-2">
          <div class="grid grid-cols-2 gap-3">
            <div class="flex flex-col gap-1">
              <label class="field-label">Peso (kg)</label>
              <InputNumber v-model="aForm.weightKg" :minFractionDigits="1" />
            </div>
            <div class="flex flex-col gap-1">
              <label class="field-label">Altura (cm)</label>
              <InputNumber v-model="aForm.heightCm" :minFractionDigits="1" />
            </div>
            <div class="flex flex-col gap-1">
              <label class="field-label">% Gordura</label>
              <InputNumber v-model="aForm.bodyFatPct" :minFractionDigits="1" />
            </div>
          </div>
          <div class="flex flex-col gap-1">
            <label class="field-label">Notas</label>
            <Textarea v-model="aForm.notes" rows="2" />
          </div>

          <!-- Medidas corporais -->
          <div class="flex flex-col gap-2">
            <div class="flex items-center justify-between">
              <label class="field-label">Medidas corporais</label>
              <Button type="button" icon="pi pi-plus" label="Adicionar" text size="small" @click="addMeasurement" />
            </div>
            <div v-for="(m, i) in aForm.measurements" :key="i" class="flex gap-2 items-center">
              <InputText v-model="m.bodyPart" placeholder="Ex: cintura" class="flex-1" />
              <InputNumber v-model="m.valueCm" placeholder="cm" :minFractionDigits="1" style="width:100px" />
              <Button type="button" icon="pi pi-trash" text severity="danger" size="small" @click="removeMeasurement(i)" />
            </div>
          </div>

          <Button type="submit" label="Salvar" :loading="saving" />
        </form>
      </Dialog>

      <!-- Dialog: Nova Meta -->
      <Dialog v-model:visible="showGoal" header="Nova Meta" :modal="true" style="width: 420px">
        <form @submit.prevent="submitGoal" class="flex flex-col gap-3 pt-2">
          <InputText v-model="gForm.description" placeholder="Descrição" required />
          <InputText v-model="gForm.metric" placeholder="Métrica (ex: weight)" />
          <Button type="submit" label="Salvar" :loading="saving" />
        </form>
      </Dialog>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppLayout from '@/components/AppLayout.vue'
import { useAssessmentStore } from '@/stores/assessment.store'
import { assessmentService } from '@/services/assessment.service'
import { goalService } from '@/services/goal.service'
import { workoutService } from '@/services/workout.service'
import type { Goal, GoalStatus, WorkoutPlan } from '@/types/api'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Textarea from 'primevue/textarea'
import Tag from 'primevue/tag'
import Menu from 'primevue/menu'
import TabView from 'primevue/tabview'
import TabPanel from 'primevue/tabpanel'

const route = useRoute()
const router = useRouter()
const studentId = route.params.id as string
const assessmentStore = useAssessmentStore()

const goals = ref<Goal[]>([])
const plans = ref<WorkoutPlan[]>([])
const loadingPlans = ref(false)
const activatingPlan = ref<string | null>(null)

const showAssessment = ref(false)
const showGoal = ref(false)
const saving = ref(false)

const aForm = ref({
  weightKg: null as number | null,
  heightCm: null as number | null,
  bodyFatPct: null as number | null,
  notes: '',
  measurements: [] as { bodyPart: string; valueCm: number | null }[],
})
const gForm = ref({ description: '', metric: '' })
const goalMenuRefs = ref<Record<string, any>>({})

onMounted(async () => {
  await assessmentStore.fetchAssessments(studentId)
  goals.value = await goalService.list(studentId)
  loadingPlans.value = true
  try { plans.value = await workoutService.listPlans(studentId) }
  finally { loadingPlans.value = false }
})

function goalSeverity(status: string) {
  return status === 'ACHIEVED' ? 'success' : status === 'EXPIRED' ? 'danger' : 'info'
}
function goalLabel(status: string) {
  return status === 'ACHIEVED' ? 'Atingida' : status === 'EXPIRED' ? 'Expirada' : 'Em andamento'
}
function toggleGoalMenu(event: Event, id: string) { goalMenuRefs.value[id]?.toggle(event) }
function goalMenuItems(goal: Goal) {
  const statuses: { label: string; status: GoalStatus }[] = [
    { label: 'Em andamento', status: 'IN_PROGRESS' },
    { label: 'Atingida', status: 'ACHIEVED' },
    { label: 'Expirada', status: 'EXPIRED' },
  ]
  return statuses
    .filter(s => s.status !== goal.status)
    .map(s => ({ label: s.label, command: () => changeGoalStatus(goal.id, s.status) }))
}
async function changeGoalStatus(id: string, status: GoalStatus) {
  const updated = await goalService.updateStatus(id, status)
  const idx = goals.value.findIndex(g => g.id === id)
  if (idx !== -1) goals.value[idx] = updated
}

function addMeasurement() { aForm.value.measurements.push({ bodyPart: '', valueCm: null }) }
function removeMeasurement(i: number) { aForm.value.measurements.splice(i, 1) }

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
        .filter(m => m.bodyPart && m.valueCm !== null)
        .map(m => ({ bodyPart: m.bodyPart, valueCm: m.valueCm as number })),
    } as any)
    showAssessment.value = false
    aForm.value = { weightKg: null, heightCm: null, bodyFatPct: null, notes: '', measurements: [] }
    await assessmentStore.fetchAssessments(studentId)
  } finally { saving.value = false }
}

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
  } finally { saving.value = false }
}

async function activatePlan(planId: string) {
  activatingPlan.value = planId
  try {
    const updated = await workoutService.activatePlan(planId)
    plans.value = plans.value.map(p => ({ ...p, active: p.id === updated.id }))
  } finally { activatingPlan.value = null }
}
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 16px; max-width: 760px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }
.tab-content { display: flex; flex-direction: column; gap: 10px; padding-top: 12px; }
.tab-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 4px; }
.tab-count { font-size: 13px; color: var(--neutral-500); }
.list-item {
  display: flex; align-items: center; gap: 12px; flex-wrap: wrap;
  padding: 10px 14px; border-radius: var(--radius-md);
  background: #fff; border: 1px solid var(--neutral-200);
  font-size: 13px; color: var(--neutral-700);
}
.empty-state { text-align: center; padding: 32px; color: var(--neutral-400); font-size: 13px; }
.field-label { font-size: 12px; font-weight: 600; color: var(--neutral-600); }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/views/trainer/StudentOverviewView.vue
git commit -m "feat: StudentOverviewView com TabView, medidas corporais e aba de fichas de treino"
```

---

### Task 6: ProfileView do aluno (leitura)

**Files:**
- Create: `frontend/src/views/student/ProfileView.vue`

**Interfaces:**
- Consumes: `profileService.getStudentProfile(studentId)` → `StudentProfile | null`, `profileService.getStudentHealth(studentId)` → `StudentHealth | null`
- Produz: view `/student/profile` com cards de dados pessoais e saúde

- [ ] **Step 1: Criar `frontend/src/views/student/ProfileView.vue`**

```vue
<!-- frontend/src/views/student/ProfileView.vue -->
<template>
  <AppLayout>
    <div class="view-wrap">
      <h1 class="page-title">Meu Perfil</h1>

      <div v-if="loading" class="empty-state"><i class="pi pi-spin pi-spinner empty-icon" /></div>
      <template v-else>

        <!-- Dados pessoais -->
        <div class="section-card">
          <h2 class="section-title"><i class="pi pi-user mr-2" />Dados Pessoais</h2>
          <div v-if="!profile" class="empty-state">Perfil não preenchido.</div>
          <div v-else class="fields-grid">
            <div class="field"><span class="field-label">CPF</span><span>{{ profile.cpf }}</span></div>
            <div class="field"><span class="field-label">RG</span><span>{{ profile.rg ?? '—' }}</span></div>
            <div class="field"><span class="field-label">Data de nascimento</span><span>{{ profile.birthDate ?? '—' }}</span></div>
            <div class="field"><span class="field-label">Gênero</span><span>{{ profile.gender ?? '—' }}</span></div>
            <div class="field"><span class="field-label">Estado civil</span><span>{{ profile.maritalStatus ?? '—' }}</span></div>
            <div class="field"><span class="field-label">Profissão</span><span>{{ profile.profession ?? '—' }}</span></div>
            <div class="field"><span class="field-label">Telefone</span><span>{{ profile.phone ?? '—' }}</span></div>
            <div class="field col-span-2"><span class="field-label">Endereço</span>
              <span>{{ addressLine }}</span>
            </div>
            <div class="field"><span class="field-label">Contato de emergência</span>
              <span>{{ profile.emergencyContactName ?? '—' }} ({{ profile.emergencyContactRelationship ?? '—' }}) {{ profile.emergencyContactPhone ?? '' }}</span>
            </div>
          </div>
        </div>

        <!-- Saúde & PAR-Q -->
        <div class="section-card">
          <h2 class="section-title"><i class="pi pi-heart mr-2" />Saúde & PAR-Q</h2>
          <div v-if="!health" class="empty-state">Ficha de saúde não preenchida.</div>
          <div v-else>
            <div class="fields-grid">
              <div class="field"><span class="field-label">Nível de atividade</span><span>{{ health.activityLevel ?? '—' }}</span></div>
              <div class="field"><span class="field-label">Objetivo</span><span>{{ health.fitnessGoal ?? '—' }}</span></div>
              <div class="field"><span class="field-label">Nível de condicionamento</span><span>{{ health.fitnessLevel ?? '—' }}</span></div>
              <div class="field"><span class="field-label">Horas de sono</span><span>{{ health.sleepHours ?? '—' }}</span></div>
              <div class="field"><span class="field-label">Nível de estresse</span><span>{{ health.stressLevel ?? '—' }}/10</span></div>
              <div class="field"><span class="field-label">Medicamentos</span><span>{{ health.medications ?? '—' }}</span></div>
              <div class="field"><span class="field-label">Restrições físicas</span><span>{{ health.physicalRestrictions ?? '—' }}</span></div>
            </div>

            <h3 class="subsection-title">Condições de saúde</h3>
            <div class="conditions-grid">
              <div class="condition" :class="{ active: health.hasHeartCondition }">Cardíaco</div>
              <div class="condition" :class="{ active: health.hasDiabetes }">Diabetes</div>
              <div class="condition" :class="{ active: health.hasHypertension }">Hipertensão</div>
              <div class="condition" :class="{ active: health.hasRespiratoryCondition }">Respiratório</div>
              <div class="condition" :class="{ active: health.hasOrthopedicCondition }">Ortopédico</div>
              <div class="condition" :class="{ active: health.hasChronicPain }">Dor crônica</div>
              <div class="condition" :class="{ active: health.hadSurgery }">Cirurgia prévia</div>
              <div class="condition" :class="{ active: health.smokes }">Fumante</div>
              <div class="condition" :class="{ active: health.drinksAlcohol }">Álcool</div>
            </div>

            <h3 class="subsection-title">PAR-Q</h3>
            <div class="parq-grid">
              <div class="parq-item" :class="{ flag: health.parqHeartProblem }">Problema cardíaco</div>
              <div class="parq-item" :class="{ flag: health.parqChestPainExercise }">Dor no peito ao exercitar</div>
              <div class="parq-item" :class="{ flag: health.parqChestPainRest }">Dor no peito em repouso</div>
              <div class="parq-item" :class="{ flag: health.parqDizziness }">Tontura/desmaio</div>
              <div class="parq-item" :class="{ flag: health.parqBoneJoint }">Osso/articulação</div>
              <div class="parq-item" :class="{ flag: health.parqBloodPressureMeds }">Medicação PA</div>
              <div class="parq-item" :class="{ flag: health.parqOtherReason }">Outro</div>
            </div>
            <div class="field mt-3">
              <span class="field-label">Assinatura PAR-Q</span>
              <span>{{ health.parqSignedAt ?? 'Não assinado' }}</span>
            </div>
          </div>
        </div>

      </template>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import profileService from '@/services/profile.service'
import { useAuthStore } from '@/stores/auth.store'
import type { StudentProfile, StudentHealth } from '@/types/api'

const authStore = useAuthStore()
const profile = ref<StudentProfile | null>(null)
const health = ref<StudentHealth | null>(null)
const loading = ref(false)

const addressLine = computed(() => {
  if (!profile.value) return '—'
  const p = profile.value
  if (!p.addressLine) return '—'
  return [p.addressLine, p.addressNumber, p.addressComplement, p.addressNeighborhood, p.addressCity, p.addressState, p.addressZip]
    .filter(Boolean).join(', ')
})

onMounted(async () => {
  loading.value = true
  try {
    [profile.value, health.value] = await Promise.all([
      profileService.getStudentProfile(authStore.userId!),
      profileService.getStudentHealth(authStore.userId!),
    ])
  } finally { loading.value = false }
})
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 20px; max-width: 780px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }
.section-card {
  background: #fff; border: 1px solid var(--neutral-200);
  border-radius: var(--radius-lg); padding: 20px;
  box-shadow: var(--shadow-card);
}
.section-title { font-size: 16px; font-weight: 700; color: var(--neutral-800); margin-bottom: 16px; display: flex; align-items: center; }
.subsection-title { font-size: 13px; font-weight: 700; color: var(--neutral-600); margin: 16px 0 8px; text-transform: uppercase; letter-spacing: 0.5px; }
.fields-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.field { display: flex; flex-direction: column; gap: 2px; }
.field-label { font-size: 11px; font-weight: 700; color: var(--neutral-500); text-transform: uppercase; letter-spacing: 0.4px; }
.col-span-2 { grid-column: span 2; }
.conditions-grid, .parq-grid { display: flex; flex-wrap: wrap; gap: 6px; }
.condition, .parq-item {
  padding: 4px 10px; border-radius: 20px; font-size: 12px;
  background: var(--neutral-100); color: var(--neutral-500); border: 1px solid var(--neutral-200);
}
.condition.active { background: #dcfce7; color: #166534; border-color: #86efac; }
.parq-item.flag { background: #fef2f2; color: #991b1b; border-color: #fca5a5; }
.empty-state { text-align: center; padding: 32px; color: var(--neutral-400); font-size: 14px; }
.empty-icon { font-size: 2rem; }

@media (max-width: 520px) {
  .fields-grid { grid-template-columns: 1fr; }
  .col-span-2 { grid-column: span 1; }
}
</style>
```

- [ ] **Step 2: Criar a pasta se necessário e commit**

```bash
git add frontend/src/views/student/ProfileView.vue
git commit -m "feat: view de perfil do aluno (leitura)"
```

---

### Task 7: ExercisesView — biblioteca de exercícios

**Files:**
- Create: `frontend/src/views/exercises/ExercisesView.vue`

**Interfaces:**
- Consumes: `exerciseService.search(params?)` → `Exercise[]`, `exerciseService.create(data)` → `Exercise`, `exerciseService.update(id, data)` → `Exercise`, `exerciseService.deactivate(id)` → void
- Produz: view `/exercises` com tabela filtrável e CRUD (trainer cria/edita, admin também desativa)

- [ ] **Step 1: Criar pasta e arquivo**

```bash
mkdir -p frontend/src/views/exercises
```

- [ ] **Step 2: Criar `frontend/src/views/exercises/ExercisesView.vue`**

```vue
<!-- frontend/src/views/exercises/ExercisesView.vue -->
<template>
  <AppLayout>
    <div class="view-wrap">
      <div class="page-header">
        <h1 class="page-title">Biblioteca de Exercícios</h1>
        <Button icon="pi pi-plus" label="Novo exercício" @click="openCreate" />
      </div>

      <!-- Filtros -->
      <div class="filters-row">
        <span class="p-input-icon-left search-wrap">
          <i class="pi pi-search" />
          <InputText v-model="searchQ" placeholder="Buscar por nome..." @input="onSearch" />
        </span>
        <Dropdown v-model="selectedGroup" :options="muscleGroups" placeholder="Grupo muscular"
          showClear @change="onSearch" style="width:200px" />
      </div>

      <DataTable :value="exercises" :loading="loading" stripedRows>
        <Column field="name" header="Nome" style="min-width:160px" />
        <Column field="muscleGroup" header="Grupo Muscular" style="min-width:140px" />
        <Column field="description" header="Descrição" style="min-width:200px">
          <template #body="{ data }">{{ data.description ?? '—' }}</template>
        </Column>
        <Column header="Vídeo" style="min-width:80px">
          <template #body="{ data }">
            <a v-if="data.videoUrl" :href="data.videoUrl" target="_blank" class="video-link">
              <i class="pi pi-external-link" /> Ver
            </a>
            <span v-else>—</span>
          </template>
        </Column>
        <Column header="Status" style="min-width:90px">
          <template #body="{ data }">
            <Tag :severity="data.active ? 'success' : 'secondary'" :value="data.active ? 'Ativo' : 'Inativo'" />
          </template>
        </Column>
        <Column header="Ações" style="min-width:120px">
          <template #body="{ data }">
            <div class="flex gap-1">
              <Button icon="pi pi-pencil" text size="small" title="Editar" @click="openEdit(data)" />
              <Button v-if="auth.role === 'ADMIN' && data.active"
                icon="pi pi-ban" text severity="danger" size="small" title="Desativar"
                :loading="deactivatingId === data.id"
                @click="deactivate(data.id)" />
            </div>
          </template>
        </Column>
      </DataTable>

      <!-- Dialog criar/editar -->
      <Dialog v-model:visible="showDialog" :header="editingId ? 'Editar exercício' : 'Novo exercício'"
        :modal="true" style="width: 460px">
        <form @submit.prevent="submitForm" class="flex flex-col gap-3 pt-2">
          <div class="flex flex-col gap-1">
            <label class="field-label">Nome *</label>
            <InputText v-model="form.name" required />
          </div>
          <div class="flex flex-col gap-1">
            <label class="field-label">Grupo muscular *</label>
            <InputText v-model="form.muscleGroup" required />
          </div>
          <div class="flex flex-col gap-1">
            <label class="field-label">Descrição</label>
            <Textarea v-model="form.description" rows="2" />
          </div>
          <div class="flex flex-col gap-1">
            <label class="field-label">URL do vídeo</label>
            <InputText v-model="form.videoUrl" placeholder="https://..." />
          </div>
          <div class="flex justify-end gap-2 mt-2">
            <Button type="button" label="Cancelar" outlined @click="showDialog = false" />
            <Button type="submit" label="Salvar" :loading="saving" />
          </div>
        </form>
      </Dialog>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { exerciseService } from '@/services/exercise.service'
import { useAuthStore } from '@/stores/auth.store'
import type { Exercise } from '@/types/api'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'
import Dropdown from 'primevue/dropdown'

const auth = useAuthStore()
const exercises = ref<Exercise[]>([])
const loading = ref(false)
const searchQ = ref('')
const selectedGroup = ref<string | null>(null)
const showDialog = ref(false)
const saving = ref(false)
const editingId = ref<string | null>(null)
const deactivatingId = ref<string | null>(null)
const form = ref({ name: '', muscleGroup: '', description: '', videoUrl: '' })

const muscleGroups = [
  'Peito', 'Costas', 'Ombros', 'Bíceps', 'Tríceps',
  'Antebraço', 'Abdômen', 'Glúteos', 'Quadríceps',
  'Isquiotibiais', 'Panturrilha', 'Cardio', 'Funcional',
]

let searchTimer: ReturnType<typeof setTimeout>

onMounted(() => loadExercises())

async function loadExercises() {
  loading.value = true
  try {
    exercises.value = await exerciseService.search({
      q: searchQ.value || undefined,
      muscleGroup: selectedGroup.value || undefined,
    })
  } finally { loading.value = false }
}

function onSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(loadExercises, 300)
}

function openCreate() {
  editingId.value = null
  form.value = { name: '', muscleGroup: '', description: '', videoUrl: '' }
  showDialog.value = true
}

function openEdit(ex: Exercise) {
  editingId.value = ex.id
  form.value = { name: ex.name, muscleGroup: ex.muscleGroup, description: ex.description ?? '', videoUrl: ex.videoUrl ?? '' }
  showDialog.value = true
}

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
    } else {
      const created = await exerciseService.create(payload)
      exercises.value.unshift(created)
    }
    showDialog.value = false
  } finally { saving.value = false }
}

async function deactivate(id: string) {
  deactivatingId.value = id
  try {
    await exerciseService.deactivate(id)
    const idx = exercises.value.findIndex(e => e.id === id)
    if (idx !== -1) exercises.value[idx] = { ...exercises.value[idx], active: false }
  } finally { deactivatingId.value = null }
}
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 16px; max-width: 900px; }
.page-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }
.filters-row { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; }
.search-wrap { position: relative; }
.field-label { font-size: 12px; font-weight: 600; color: var(--neutral-600); }
.video-link { color: var(--blue-500); font-size: 13px; display: flex; align-items: center; gap: 4px; }
</style>
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/exercises/ExercisesView.vue
git commit -m "feat: biblioteca de exercícios com busca, CRUD e desativação"
```

---

### Task 8: WorkoutPlanView — editor de fichas de treino

**Files:**
- Create: `frontend/src/views/trainer/WorkoutPlanView.vue`

**Interfaces:**
- Consumes:
  - `workoutService.listPlans(studentId)` → `WorkoutPlan[]`
  - `workoutService.createPlan(studentId, data)` → `WorkoutPlan`
  - `workoutService.activatePlan(planId)` → `WorkoutPlan`
  - `api.put('/api/workout-plans/{planId}', data)` → `WorkoutPlan` (via `workoutService` — método `updatePlan` a invocar diretamente via api)
  - `exerciseService.search({ q })` → `Exercise[]` (autocomplete com debounce)
  - `userService.getById(id)` ou listagem de users para nome do aluno
- Produz: view `/trainer/students/:id/workout` com painel esquerdo (lista de fichas) e painel direito (editor)

**Nota:** `workoutService` não tem `updatePlan`. Usar `api.put` diretamente com a URL, importando `api` de `@/services/api`.

- [ ] **Step 1: Criar `frontend/src/views/trainer/WorkoutPlanView.vue`**

```vue
<!-- frontend/src/views/trainer/WorkoutPlanView.vue -->
<template>
  <AppLayout>
    <div class="builder-shell">

      <!-- Painel esquerdo: lista de fichas -->
      <aside class="plans-panel">
        <Button icon="pi pi-arrow-left" text label="Voltar" size="small" @click="$router.back()" class="mb-3" />
        <h2 class="panel-title">Fichas de treino</h2>
        <div v-if="loadingPlans" class="plans-empty"><i class="pi pi-spin pi-spinner" /></div>
        <div v-else class="plans-list">
          <div v-for="p in plans" :key="p.id"
            class="plan-item" :class="{ selected: selectedPlanId === p.id }"
            @click="selectPlan(p)">
            <div class="plan-item-name">{{ p.name }}</div>
            <Tag :severity="p.active ? 'success' : 'secondary'" :value="p.active ? 'Ativa' : 'Inativa'" />
          </div>
          <div v-if="plans.length === 0" class="plans-empty">Nenhuma ficha.</div>
        </div>
        <Button icon="pi pi-plus" label="Nova ficha" outlined class="mt-3 w-full" @click="newPlan" />
      </aside>

      <!-- Painel direito: editor -->
      <section class="editor-panel">
        <div v-if="!editorVisible" class="editor-empty">
          <i class="pi pi-file-edit editor-empty-icon" />
          <p>Selecione uma ficha ou crie uma nova.</p>
        </div>

        <div v-else class="editor-content">
          <div class="editor-header">
            <div class="flex flex-col gap-1 flex-1">
              <label class="field-label">Nome da ficha *</label>
              <InputText v-model="editor.name" placeholder="Ex: Hipertrofia - Fase 1" required />
            </div>
            <div class="flex flex-col gap-1 flex-1">
              <label class="field-label">Descrição</label>
              <InputText v-model="editor.description" placeholder="Opcional" />
            </div>
          </div>

          <!-- Tabela de exercícios -->
          <div class="items-table">
            <div class="items-header">
              <span class="col-exercise">Exercício</span>
              <span class="col-div">Divisão</span>
              <span class="col-num">Séries</span>
              <span class="col-reps">Reps</span>
              <span class="col-num">Carga (kg)</span>
              <span class="col-num">Descanso (s)</span>
              <span class="col-num">Ordem</span>
              <span class="col-notes">Notas</span>
              <span class="col-action"></span>
            </div>

            <div v-for="(item, idx) in editor.items" :key="idx" class="items-row">
              <div class="col-exercise">
                <AutoComplete
                  v-model="item.exerciseDisplay"
                  :suggestions="exerciseSuggestions"
                  optionLabel="name"
                  placeholder="Buscar exercício..."
                  :delay="300"
                  @complete="searchExercises($event.query)"
                  @item-select="(e) => onExerciseSelect(e.value, idx)"
                  forceSelection
                  class="w-full"
                />
              </div>
              <input v-model="item.division" class="cell-input" placeholder="A/B/C" />
              <input v-model.number="item.sets" type="number" min="1" class="cell-input" placeholder="3" />
              <input v-model="item.reps" class="cell-input" placeholder="10-12" />
              <input v-model.number="item.suggestedLoadKg" type="number" step="0.5" class="cell-input" placeholder="0" />
              <input v-model.number="item.restSeconds" type="number" class="cell-input" placeholder="60" />
              <input v-model.number="item.orderIndex" type="number" class="cell-input" placeholder="1" />
              <input v-model="item.notes" class="cell-input" placeholder="—" />
              <Button icon="pi pi-trash" text severity="danger" size="small" @click="removeItem(idx)" />
            </div>
          </div>

          <Button icon="pi pi-plus" label="Adicionar exercício" text @click="addItem" class="mt-2" />

          <!-- Footer de ações -->
          <div class="editor-footer">
            <Button label="Salvar" :loading="saving" @click="savePlan" />
            <Button v-if="selectedPlanId && !activePlan?.active"
              label="Ativar ficha" severity="success" outlined :loading="activating"
              @click="activateCurrent" />
            <Tag v-if="activePlan?.active" severity="success" value="Ficha ativa" />
          </div>
          <p v-if="saveError" class="text-red-500 text-sm mt-1">{{ saveError }}</p>
        </div>
      </section>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import AppLayout from '@/components/AppLayout.vue'
import { workoutService } from '@/services/workout.service'
import { exerciseService } from '@/services/exercise.service'
import api from '@/services/api'
import type { WorkoutPlan, Exercise } from '@/types/api'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Tag from 'primevue/tag'
import AutoComplete from 'primevue/autocomplete'

const route = useRoute()
const studentId = route.params.id as string
const planIdFromQuery = route.query.planId as string | undefined

// State
const plans = ref<WorkoutPlan[]>([])
const loadingPlans = ref(false)
const selectedPlanId = ref<string | null>(null)
const saving = ref(false)
const activating = ref(false)
const saveError = ref<string | null>(null)
const exerciseSuggestions = ref<Exercise[]>([])
const exerciseMap = ref<Record<string, string>>({})

interface EditorItem {
  exerciseId: string
  exerciseDisplay: Exercise | null
  division: string
  sets: number | null
  reps: string
  suggestedLoadKg: number | null
  restSeconds: number | null
  orderIndex: number
  notes: string
}

const editor = ref({
  name: '',
  description: '',
  items: [] as EditorItem[],
})

const editorVisible = computed(() => editor.value.name !== '' || selectedPlanId.value !== null)
const activePlan = computed(() => plans.value.find(p => p.id === selectedPlanId.value) ?? null)

onMounted(async () => {
  loadingPlans.value = true
  try {
    plans.value = await workoutService.listPlans(studentId)
    const allExercises = await exerciseService.search()
    exerciseMap.value = Object.fromEntries(allExercises.map(e => [e.id, e.name]))

    if (planIdFromQuery) {
      const found = plans.value.find(p => p.id === planIdFromQuery)
      if (found) selectPlan(found)
    }
  } finally { loadingPlans.value = false }
})

function selectPlan(plan: WorkoutPlan) {
  selectedPlanId.value = plan.id
  editor.value.name = plan.name
  editor.value.description = plan.description ?? ''
  editor.value.items = plan.items.map(item => ({
    exerciseId: item.exerciseId,
    exerciseDisplay: { id: item.exerciseId, name: exerciseMap.value[item.exerciseId] ?? item.exerciseId } as Exercise,
    division: item.division ?? '',
    sets: item.sets,
    reps: item.reps,
    suggestedLoadKg: item.suggestedLoadKg,
    restSeconds: item.restSeconds,
    orderIndex: item.orderIndex,
    notes: item.notes ?? '',
  }))
}

function newPlan() {
  selectedPlanId.value = null
  editor.value = { name: '', description: '', items: [] }
}

function addItem() {
  editor.value.items.push({
    exerciseId: '',
    exerciseDisplay: null,
    division: '',
    sets: null,
    reps: '',
    suggestedLoadKg: null,
    restSeconds: null,
    orderIndex: editor.value.items.length + 1,
    notes: '',
  })
}

function removeItem(idx: number) { editor.value.items.splice(idx, 1) }

async function searchExercises(query: string) {
  exerciseSuggestions.value = await exerciseService.search({ q: query })
}

function onExerciseSelect(exercise: Exercise, idx: number) {
  editor.value.items[idx].exerciseId = exercise.id
  exerciseMap.value[exercise.id] = exercise.name
}

function buildPayload() {
  return {
    name: editor.value.name,
    description: editor.value.description || undefined,
    items: editor.value.items
      .filter(item => item.exerciseId)
      .map((item, i) => ({
        exerciseId: item.exerciseId,
        division: item.division || undefined,
        sets: item.sets ?? 1,
        reps: item.reps || '1',
        suggestedLoadKg: item.suggestedLoadKg ?? undefined,
        restSeconds: item.restSeconds ?? undefined,
        orderIndex: item.orderIndex || i + 1,
        notes: item.notes || undefined,
      })),
  }
}

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
  } catch (e: any) {
    saveError.value = e.response?.data?.message ?? 'Erro ao salvar.'
  } finally { saving.value = false }
}

async function activateCurrent() {
  if (!selectedPlanId.value) return
  activating.value = true
  try {
    const updated = await workoutService.activatePlan(selectedPlanId.value)
    plans.value = plans.value.map(p => ({ ...p, active: p.id === updated.id }))
  } finally { activating.value = false }
}
</script>

<style scoped>
.builder-shell { display: flex; gap: 20px; height: calc(100dvh - 100px); overflow: hidden; }

/* Painel esquerdo */
.plans-panel {
  width: 240px; flex-shrink: 0;
  background: #fff; border: 1px solid var(--neutral-200);
  border-radius: var(--radius-lg); padding: 16px;
  display: flex; flex-direction: column;
  overflow-y: auto;
}
.panel-title { font-family: var(--font-display); font-size: 15px; font-weight: 700; color: var(--neutral-800); margin-bottom: 10px; }
.plans-list { display: flex; flex-direction: column; gap: 6px; flex: 1; }
.plan-item {
  padding: 8px 10px; border-radius: var(--radius-md);
  border: 1.5px solid var(--neutral-200); cursor: pointer;
  display: flex; flex-direction: column; gap: 4px;
  transition: border-color 0.15s, background 0.15s;
}
.plan-item:hover { border-color: var(--blue-300); background: #f0f6ff; }
.plan-item.selected { border-color: var(--blue-500); background: #eff6ff; }
.plan-item-name { font-size: 13px; font-weight: 600; color: var(--neutral-800); }
.plans-empty { text-align: center; padding: 20px; color: var(--neutral-400); font-size: 13px; }

/* Painel direito */
.editor-panel {
  flex: 1; min-width: 0;
  background: #fff; border: 1px solid var(--neutral-200);
  border-radius: var(--radius-lg); padding: 20px;
  overflow-y: auto; display: flex; flex-direction: column;
}
.editor-empty {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 10px;
  color: var(--neutral-400);
}
.editor-empty-icon { font-size: 3rem; }
.editor-content { display: flex; flex-direction: column; gap: 16px; }
.editor-header { display: flex; gap: 16px; flex-wrap: wrap; }
.field-label { font-size: 12px; font-weight: 600; color: var(--neutral-600); }

/* Tabela de itens */
.items-table { overflow-x: auto; }
.items-header, .items-row {
  display: grid;
  grid-template-columns: 2fr 70px 60px 80px 90px 90px 60px 1fr 36px;
  gap: 6px; align-items: center; min-width: 740px;
}
.items-header { font-size: 11px; font-weight: 700; color: var(--neutral-500); text-transform: uppercase; letter-spacing: 0.4px; padding: 4px 0 6px; border-bottom: 1px solid var(--neutral-200); }
.items-row { padding: 6px 0; border-bottom: 1px solid var(--neutral-100); }
.cell-input {
  width: 100%; padding: 5px 8px; font-size: 13px;
  border: 1.5px solid var(--neutral-200); border-radius: var(--radius-sm);
  background: #fff; color: var(--neutral-900); outline: none;
  transition: border-color 0.15s;
}
.cell-input:focus { border-color: var(--blue-400); }
.col-exercise {} .col-div {} .col-num {} .col-reps {} .col-notes {} .col-action {}
.editor-footer { display: flex; align-items: center; gap: 12px; padding-top: 8px; border-top: 1px solid var(--neutral-200); margin-top: 8px; }

/* Mobile */
@media (max-width: 768px) {
  .builder-shell { flex-direction: column; height: auto; }
  .plans-panel { width: 100%; }
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/views/trainer/WorkoutPlanView.vue
git commit -m "feat: workout plan builder para trainer"
```

---

## Self-Review

**Cobertura da spec:**
- ✅ 1.1 Nomes de exercício em WorkoutView e RecordsView → Task 3
- ✅ 1.2 Mudança de status em GoalsView → Task 4
- ✅ 1.3 Medidas corporais no form de avaliação → Task 5
- ✅ 2.1 `/student/profile` leitura → Task 6
- ✅ 2.2 `/exercises` CRUD → Task 7
- ✅ Seção 3 StudentOverviewView tabs + fichas → Task 5
- ✅ Seção 4 WorkoutPlanView → Task 8
- ✅ Rotas e menu → Task 1
- ✅ Bug exerciseService.deactivate → Task 2

**Sem placeholders, sem TBDs.**

**Consistência de tipos:** `WorkoutPlan`, `Exercise`, `Goal`, `GoalStatus`, `StudentProfile`, `StudentHealth` usados conforme definidos em `types/api.ts`. Métodos de service usados conforme assinaturas lidas nos arquivos.
