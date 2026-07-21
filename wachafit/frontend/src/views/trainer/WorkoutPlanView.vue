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
                <Select
                  v-model="item.exerciseId"
                  :options="allExercises"
                  optionLabel="name"
                  optionValue="id"
                  placeholder="Selecionar exercício..."
                  filter
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
          <p v-if="successMsg" class="success-msg">{{ successMsg }}</p>
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
import Select from 'primevue/select'

const route = useRoute()
const studentId = route.params.id as string
const planIdFromQuery = route.query.planId as string | undefined

// State
const plans = ref<WorkoutPlan[]>([])
const loadingPlans = ref(false)
const selectedPlanId = ref<string | null>(null)
const creating = ref(false)
const saving = ref(false)
const activating = ref(false)
const saveError = ref<string | null>(null)
const successMsg = ref('')
function showSuccess(msg: string) {
  successMsg.value = msg
  setTimeout(() => { successMsg.value = '' }, 3000)
}
const allExercises = ref<Exercise[]>([])
const exerciseMap = ref<Record<string, string>>({})

interface EditorItem {
  exerciseId: string
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

const editorVisible = computed(() => creating.value || editor.value.name !== '' || selectedPlanId.value !== null)
const activePlan = computed(() => plans.value.find(p => p.id === selectedPlanId.value) ?? null)

onMounted(async () => {
  loadingPlans.value = true
  try {
    plans.value = await workoutService.listPlans(studentId)
    allExercises.value = await exerciseService.search()
    exerciseMap.value = Object.fromEntries(allExercises.value.map(e => [e.id, e.name]))

    if (planIdFromQuery) {
      const found = plans.value.find(p => p.id === planIdFromQuery)
      if (found) selectPlan(found)
    } else {
      creating.value = true
    }
  } finally { loadingPlans.value = false }
})

function selectPlan(plan: WorkoutPlan) {
  creating.value = false
  selectedPlanId.value = plan.id
  editor.value.name = plan.name
  editor.value.description = plan.description ?? ''
  editor.value.items = plan.items.map(item => ({
    exerciseId: item.exerciseId,
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
  creating.value = true
}

function addItem() {
  editor.value.items.push({
    exerciseId: '',
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
    showSuccess('Ficha salva.')
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
    showSuccess('Ficha ativada.')
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

.success-msg { color: #22c55e; font-size: 0.875rem; margin-top: 0.25rem; }

/* Mobile */
@media (max-width: 768px) {
  .builder-shell { flex-direction: column; height: auto; }
  .plans-panel { width: 100%; }
}
</style>
