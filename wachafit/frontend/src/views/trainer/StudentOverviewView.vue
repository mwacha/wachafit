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
              <Button icon="pi pi-pencil" text size="small" aria-label="Ver/Editar ficha"
                @click="$router.push(`/trainer/students/${studentId}/workout?planId=${p.id}`)" />
            </div>
          </div>
        </TabPanel>
      </TabView>

      <!-- Dialog: Nova Avaliação -->
      <Dialog v-model:visible="showAssessment" header="Nova Avaliação" :modal="true" style="width: min(480px, 95vw)">
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
              <InputText v-model="m.bodyPart" placeholder="Parte (ex: cintura)" style="flex:1" />
              <InputNumber v-model="m.valueCm" placeholder="cm" :minFractionDigits="1" style="width:100px" />
              <Button type="button" icon="pi pi-trash" text severity="danger" size="small" @click="removeMeasurement(i)" />
            </div>
          </div>

          <Button type="submit" label="Salvar" :loading="saving" />
        </form>
      </Dialog>

      <!-- Dialog: Nova Meta -->
      <Dialog v-model:visible="showGoal" header="Nova Meta" :modal="true" style="width: min(420px, 95vw)">
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
        .filter(m => m.bodyPart && m.valueCm != null)
        .map(m => ({ bodyPart: m.bodyPart, valueCm: m.valueCm as number })),
    })
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
}
.empty-state { text-align: center; padding: 32px; color: var(--neutral-400); font-size: 13px; }
.field-label { font-size: 12px; font-weight: 600; color: var(--neutral-600); }
</style>
