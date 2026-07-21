<!-- frontend/src/views/student/WorkoutView.vue -->
<template>
  <AppLayout>
    <div class="view-wrap">
      <div class="page-header">
        <h1 class="page-title">Meu Treino</h1>
        <Button v-if="workoutStore.activePlan"
          icon="pi pi-download" label="Baixar Ficha" outlined
          :loading="downloadingPdf" @click="downloadWorkoutPdf" />
      </div>

      <div v-if="workoutStore.loading" class="empty-state">
        <i class="pi pi-spin pi-spinner empty-icon" />
        <p>Carregando...</p>
      </div>

      <div v-else-if="!workoutStore.activePlan" class="empty-state">
        <i class="pi pi-file empty-icon" />
        <p>Você não tem uma ficha de treino ativa.</p>
        <p class="empty-hint">Solicite ao seu personal para criar uma ficha.</p>
      </div>

      <div v-else class="plan-wrap">
        <!-- Cabeçalho da ficha -->
        <div class="plan-card-header">
          <div>
            <h2 class="plan-name">{{ workoutStore.activePlan.name }}</h2>
            <p v-if="workoutStore.activePlan.description" class="plan-desc">
              {{ workoutStore.activePlan.description }}
            </p>
          </div>
          <Tag severity="success" value="Ativa" />
        </div>

        <!-- Exercícios agrupados por divisão -->
        <div v-for="group in groupedItems" :key="group.division" class="division-section">
          <div v-if="group.division !== '—'" class="division-header">
            <span class="division-badge">Divisão {{ group.division }}</span>
            <span class="division-count">{{ group.items.length }} exercício(s)</span>
          </div>

          <div class="exercise-list">
            <div v-for="item in group.items" :key="item.id" class="exercise-card">
              <div class="ex-info">
                <span class="ex-name">{{ exerciseNames[item.exerciseId] ?? 'Exercício' }}</span>
                <div class="ex-chips">
                  <span class="ex-chip chip-primary">{{ item.sets }}×{{ item.reps }}</span>
                  <span v-if="item.suggestedLoadKg" class="ex-chip chip-blue">
                    <i class="pi pi-circle-fill" style="font-size:6px" /> {{ item.suggestedLoadKg }} kg
                  </span>
                  <span v-if="item.restSeconds" class="ex-chip chip-gray">
                    <i class="pi pi-clock" style="font-size:10px" /> {{ item.restSeconds }}s
                  </span>
                </div>
                <p v-if="item.notes" class="ex-notes">{{ item.notes }}</p>
              </div>
              <Button icon="pi pi-plus" label="Registrar" size="small" outlined
                @click="openLog(item)" />
            </div>
          </div>
        </div>
      </div>

      <!-- Dialog: Registrar execução -->
      <Dialog v-model:visible="showLog" header="Registrar Execução" :modal="true" style="width: min(380px, 95vw)">
        <div v-if="currentItem" class="log-form">
          <p class="log-exercise-name">{{ exerciseNames[currentItem.exerciseId] ?? 'Exercício' }}</p>
          <div class="form-row">
            <div class="form-field">
              <label class="form-label">Séries</label>
              <InputNumber v-model="logForm.sets" :min="1" fluid />
            </div>
            <div class="form-field">
              <label class="form-label">Repetições</label>
              <InputNumber v-model="logForm.reps" :min="1" fluid />
            </div>
          </div>
          <div class="form-field">
            <label class="form-label">Carga (kg)</label>
            <InputNumber v-model="logForm.loadKg" :minFractionDigits="1" fluid />
          </div>
          <div class="form-field">
            <label class="form-label">Notas</label>
            <InputText v-model="logForm.notes" placeholder="Opcional" style="width:100%" />
          </div>
          <p v-if="logError" class="error-msg">{{ logError }}</p>
          <div class="form-actions">
            <Button label="Cancelar" outlined @click="showLog = false" />
            <Button label="Salvar" :loading="saving" @click="submitLog" />
          </div>
        </div>
      </Dialog>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
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
import Tag from 'primevue/tag'
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

const groupedItems = computed(() => {
  const items = workoutStore.activePlan?.items ?? []
  const groups = new Map<string, WorkoutPlanItem[]>()
  for (const item of [...items].sort((a, b) => a.orderIndex - b.orderIndex)) {
    const key = item.division ?? '—'
    if (!groups.has(key)) groups.set(key, [])
    groups.get(key)!.push(item)
  }
  return Array.from(groups.entries()).map(([division, items]) => ({ division, items }))
})

onMounted(async () => {
  await workoutStore.fetchActivePlan(authStore.userId!)
  const exercises = await exerciseService.search()
  exerciseNames.value = Object.fromEntries(exercises.map(e => [e.id, e.name]))
})

function openLog(item: WorkoutPlanItem) {
  currentItem.value = item
  logForm.value = { sets: null, reps: null, loadKg: null, notes: '' }
  logError.value = null
  showLog.value = true
}

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
  } catch (e: any) {
    logError.value = e.response?.data?.message ?? 'Erro ao registrar'
  } finally { saving.value = false }
}

async function downloadWorkoutPdf() {
  if (!authStore.userId) return
  downloadingPdf.value = true
  try {
    const res = await api.get(`/api/students/${authStore.userId}/pdf/workout`, { responseType: 'blob' })
    const url = URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }))
    const a = document.createElement('a'); a.href = url; a.download = 'ficha-treino.pdf'; a.click()
    URL.revokeObjectURL(url)
  } finally { downloadingPdf.value = false }
}
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 20px }
.page-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }

.empty-state {
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  text-align: center; padding: 48px 24px;
  background: #fff; border: 1px solid var(--neutral-200);
  border-radius: var(--radius-lg); color: var(--neutral-500); font-size: 14px;
}
.empty-icon { font-size: 2.5rem; color: var(--neutral-300); }
.empty-hint { font-size: 13px; }

/* Plan header */
.plan-wrap { display: flex; flex-direction: column; gap: 16px; }
.plan-card-header {
  display: flex; align-items: flex-start; justify-content: space-between; gap: 12px;
  background: #fff; border: 1px solid var(--neutral-200);
  border-radius: var(--radius-lg); padding: 16px 20px;
  box-shadow: var(--shadow-card);
}
.plan-name { font-size: 17px; font-weight: 700; color: var(--neutral-900); margin: 0 0 4px; }
.plan-desc { font-size: 13px; color: var(--neutral-500); margin: 0; }

/* Division sections */
.division-section { display: flex; flex-direction: column; gap: 8px; }
.division-header { display: flex; align-items: center; gap: 10px; }
.division-badge {
  font-size: 12px; font-weight: 700; letter-spacing: .05em; text-transform: uppercase;
  background: #eff6ff; color: var(--blue-600);
  padding: 4px 12px; border-radius: 20px;
}
.division-count { font-size: 12px; color: var(--neutral-400); }

/* Exercise cards */
.exercise-list { display: flex; flex-direction: column; gap: 8px; }
.exercise-card {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  background: #fff; border: 1px solid var(--neutral-200);
  border-radius: var(--radius-md); padding: 12px 16px;
  box-shadow: var(--shadow-card);
  transition: box-shadow 0.15s;
}
.exercise-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.07); }
.ex-info { display: flex; flex-direction: column; gap: 5px; flex: 1; min-width: 0; }
.ex-name { font-size: 14px; font-weight: 700; color: var(--neutral-900); }

.ex-chips { display: flex; gap: 6px; flex-wrap: wrap; align-items: center; }
.ex-chip {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 11px; font-weight: 700; padding: 3px 9px; border-radius: 12px;
}
.chip-primary { background: var(--blue-500); color: #fff; }
.chip-blue { background: #dbeafe; color: #1d4ed8; }
.chip-gray { background: var(--neutral-100); color: var(--neutral-600); }

.ex-notes { font-size: 11px; color: var(--neutral-400); font-style: italic; margin: 0; }

/* Log dialog */
.log-form { display: flex; flex-direction: column; gap: 14px; padding: 8px 0 4px; }
.log-exercise-name { font-size: 14px; font-weight: 700; color: var(--neutral-800); margin: 0; }
.form-row { display: flex; gap: 12px; }
.form-field { display: flex; flex-direction: column; gap: 6px; flex: 1; }
.form-label { font-size: 13px; font-weight: 600; color: var(--neutral-700); }
.form-actions { display: flex; justify-content: flex-end; gap: 8px; padding-top: 4px; }
.error-msg { color: #dc2626; font-size: 12px; }
</style>
