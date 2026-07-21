<!-- frontend/src/views/trainer/StudentOverviewView.vue -->
<template>
  <AppLayout>
    <div class="view-wrap">
      <Button icon="pi pi-arrow-left" text label="Voltar" @click="$router.back()" class="mb-2" />
      <h1 class="page-title">Visão do Aluno</h1>

      <TabView>
        <!-- ── Tab Avaliações ── -->
        <TabPanel value="0" header="Avaliações">
          <div class="tab-content">
            <div class="tab-header">
              <span class="tab-count">{{ sortedAssessments.length }} avaliação(ões)</span>
              <Button icon="pi pi-plus" label="Nova avaliação" size="small" @click="showAssessment = true" />
            </div>
            <div v-if="sortedAssessments.length === 0" class="empty-state">Nenhuma avaliação registrada.</div>
            <div v-for="(a, idx) in sortedAssessments" :key="a.id" class="assessment-card">
              <!-- Cabeçalho -->
              <div class="acard-header" @click="toggleCard(a.id)">
                <div class="acard-date-wrap">
                  <span class="acard-badge">{{ sortedAssessments.length - idx }}ª</span>
                  <span class="acard-date">{{ formatDate(a.assessedAt) }}</span>
                </div>
                <div class="acard-summary">
                  <span v-if="a.weightKg">{{ a.weightKg }} kg</span>
                  <span v-if="a.bodyFatPct">{{ a.bodyFatPct }}% gordura</span>
                  <span v-if="a.bmi">IMC {{ a.bmi }}</span>
                </div>
                <i :class="['acard-chevron pi', expandedCards.has(a.id) ? 'pi-chevron-up' : 'pi-chevron-down']" />
              </div>

              <!-- Detalhes -->
              <div v-if="expandedCards.has(a.id)" class="acard-body">
                <div class="acard-metrics">
                  <div v-if="a.weightKg" class="metric-box">
                    <span class="metric-lbl">Peso</span>
                    <span class="metric-val">{{ a.weightKg }}<small>kg</small></span>
                  </div>
                  <div v-if="a.heightCm" class="metric-box">
                    <span class="metric-lbl">Altura</span>
                    <span class="metric-val">{{ a.heightCm }}<small>cm</small></span>
                  </div>
                  <div v-if="a.bodyFatPct" class="metric-box">
                    <span class="metric-lbl">% Gordura</span>
                    <span class="metric-val">{{ a.bodyFatPct }}<small>%</small></span>
                  </div>
                  <div v-if="a.bmi" class="metric-box">
                    <span class="metric-lbl">IMC</span>
                    <span class="metric-val">{{ a.bmi }}</span>
                  </div>
                </div>

                <template v-if="a.measurements?.length">
                  <h4 class="measures-title">Medidas corporais</h4>
                  <div class="measures-grid">
                    <div v-for="m in a.measurements" :key="m.bodyPart" class="measure-item">
                      <span class="measure-part">{{ m.bodyPart }}</span>
                      <span class="measure-val">{{ m.valueCm }} cm</span>
                    </div>
                  </div>
                </template>

                <div v-if="a.notes" class="acard-notes">
                  <i class="pi pi-comment" /> {{ a.notes }}
                </div>
              </div>
            </div>
          </div>
        </TabPanel>

        <!-- ── Tab Metas ── -->
        <TabPanel value="1" header="Metas">
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
        <TabPanel value="2" header="Fichas de Treino">
          <div class="tab-content">
            <div class="tab-header">
              <span class="tab-count">{{ plans.length }} ficha(s)</span>
              <Button icon="pi pi-plus" label="Nova ficha" size="small"
                @click="$router.push(`/trainer/students/${studentId}/workout`)" />
            </div>
            <div v-if="loadingPlans" class="empty-state"><i class="pi pi-spin pi-spinner" /></div>
            <div v-else-if="plans.length === 0" class="empty-state">Nenhuma ficha de treino.</div>
            <div v-for="p in plans" :key="p.id" class="plan-card">
              <!-- Cabeçalho do plano -->
              <div class="plan-header" @click="togglePlan(p.id)">
                <div class="plan-header-left">
                  <i :class="['acard-chevron pi', expandedPlans.has(p.id) ? 'pi-chevron-up' : 'pi-chevron-down']" />
                  <span class="plan-name">{{ p.name }}</span>
                  <Tag :severity="p.active ? 'success' : 'secondary'" :value="p.active ? 'Ativa' : 'Inativa'" />
                </div>
                <div class="plan-header-actions" @click.stop>
                  <Button v-if="!p.active" label="Ativar" size="small" outlined
                    :loading="activatingPlan === p.id" @click="activatePlan(p.id)" />
                  <Button icon="pi pi-pencil" text size="small" aria-label="Editar ficha"
                    @click="$router.push(`/trainer/students/${studentId}/workout?planId=${p.id}`)" />
                </div>
              </div>

              <!-- Detalhes expandíveis -->
              <div v-if="expandedPlans.has(p.id)" class="plan-body">
                <p v-if="p.description" class="plan-desc">{{ p.description }}</p>
                <div v-if="p.items?.length === 0" class="empty-state" style="padding:16px">Nenhum exercício cadastrado.</div>
                <template v-else>
                  <div v-for="group in groupByDivision(p.items)" :key="group.division" class="division-group">
                    <div v-if="group.division !== '—'" class="division-label">
                      Divisão {{ group.division }}
                    </div>
                    <div class="exercise-table">
                      <div class="ex-row ex-header">
                        <span class="ex-col-name">Exercício</span>
                        <span class="ex-col-num">Séries × Reps</span>
                        <span class="ex-col-num">Carga</span>
                        <span class="ex-col-num">Descanso</span>
                        <span class="ex-col-notes">Notas</span>
                      </div>
                      <div v-for="item in group.items" :key="item.id" class="ex-row">
                        <span class="ex-col-name">{{ exerciseNames[item.exerciseId] ?? '—' }}</span>
                        <span class="ex-col-num">{{ item.sets }}×{{ item.reps }}</span>
                        <span class="ex-col-num">{{ item.suggestedLoadKg ? `${item.suggestedLoadKg} kg` : '—' }}</span>
                        <span class="ex-col-num">{{ item.restSeconds ? `${item.restSeconds}s` : '—' }}</span>
                        <span class="ex-col-notes">{{ item.notes ?? '—' }}</span>
                      </div>
                    </div>
                  </div>
                </template>
              </div>
            </div>
          </div>
        </TabPanel>
      </TabView>

      <p v-if="successMsg" class="success-msg">{{ successMsg }}</p>

      <!-- Dialog: Nova Avaliação -->
      <Dialog v-model:visible="showAssessment" header="Nova Avaliação" :modal="true" style="width: min(520px, 95vw)">
        <form @submit.prevent="submitAssessment" class="assessment-form">
          <div class="form-row">
            <div class="form-field">
              <label class="form-label">Peso (kg)</label>
              <InputNumber v-model="aForm.weightKg" :minFractionDigits="1" fluid />
            </div>
            <div class="form-field">
              <label class="form-label">Altura (cm)</label>
              <InputNumber v-model="aForm.heightCm" :minFractionDigits="1" fluid />
            </div>
            <div class="form-field">
              <label class="form-label">% Gordura</label>
              <InputNumber v-model="aForm.bodyFatPct" :minFractionDigits="1" fluid />
            </div>
          </div>

          <div class="form-field">
            <label class="form-label">Notas</label>
            <Textarea v-model="aForm.notes" rows="2" autoResize style="width:100%" />
          </div>

          <!-- Medidas corporais -->
          <div class="form-field">
            <div class="measures-header">
              <label class="form-label">Medidas corporais</label>
              <Button type="button" icon="pi pi-plus" label="Adicionar" text size="small" @click="addMeasurement" />
            </div>
            <div v-for="(m, i) in aForm.measurements" :key="i" class="measure-row">
              <Select v-model="m.bodyPart" :options="bodyPartOptions" placeholder="Parte do corpo"
                editable style="flex:1" />
              <InputNumber v-model="m.valueCm" placeholder="cm" :minFractionDigits="1" style="width:100px" />
              <Button type="button" icon="pi pi-trash" text severity="danger" size="small" @click="removeMeasurement(i)" />
            </div>
          </div>

          <div class="form-actions">
            <Button type="button" label="Cancelar" outlined @click="showAssessment = false" />
            <Button type="submit" label="Salvar avaliação" :loading="saving" />
          </div>
        </form>
      </Dialog>

      <!-- Dialog: Nova Meta -->
      <Dialog v-model:visible="showGoal" header="Nova Meta" :modal="true" style="width: min(420px, 95vw)">
        <form @submit.prevent="submitGoal" class="goal-form">
          <div class="form-field">
            <label class="form-label">Descrição *</label>
            <InputText v-model="gForm.description" placeholder="Ex: Perder 5kg em 3 meses" class="w-full" required />
          </div>
          <div class="form-field">
            <label class="form-label">Métrica</label>
            <InputText v-model="gForm.metric" placeholder="Ex: weight" class="w-full" />
          </div>
          <div class="form-actions">
            <Button type="button" label="Cancelar" outlined @click="showGoal = false" />
            <Button type="submit" label="Salvar" :loading="saving" />
          </div>
        </form>
      </Dialog>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import AppLayout from '@/components/AppLayout.vue'
import { useAssessmentStore } from '@/stores/assessment.store'
import { assessmentService } from '@/services/assessment.service'
import { goalService } from '@/services/goal.service'
import { workoutService } from '@/services/workout.service'
import { exerciseService } from '@/services/exercise.service'
import type { Goal, GoalStatus, WorkoutPlan, WorkoutPlanItem } from '@/types/api'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Textarea from 'primevue/textarea'
import Tag from 'primevue/tag'
import Menu from 'primevue/menu'
import TabView from 'primevue/tabview'
import TabPanel from 'primevue/tabpanel'
import Select from 'primevue/select'

const route = useRoute()
const studentId = route.params.id as string
const assessmentStore = useAssessmentStore()

const exerciseNames = ref<Record<string, string>>({})
const expandedPlans = ref<Set<string>>(new Set())
function togglePlan(id: string) {
  const s = new Set(expandedPlans.value)
  if (s.has(id)) s.delete(id); else s.add(id)
  expandedPlans.value = s
}
function groupByDivision(items: WorkoutPlanItem[]) {
  const groups = new Map<string, WorkoutPlanItem[]>()
  for (const item of [...items].sort((a, b) => a.orderIndex - b.orderIndex)) {
    const key = item.division ?? '—'
    if (!groups.has(key)) groups.set(key, [])
    groups.get(key)!.push(item)
  }
  return Array.from(groups.entries()).map(([division, items]) => ({ division, items }))
}

const bodyPartOptions = [
  'Cintura', 'Quadril', 'Abdômen', 'Peito', 'Bíceps direito', 'Bíceps esquerdo',
  'Coxa direita', 'Coxa esquerda', 'Panturrilha direita', 'Panturrilha esquerda',
  'Ombro', 'Antebraço direito', 'Antebraço esquerdo',
]

const expandedCards = ref<Set<string>>(new Set())
function toggleCard(id: string) {
  const s = new Set(expandedCards.value)
  if (s.has(id)) s.delete(id); else s.add(id)
  expandedCards.value = s
}
function formatDate(iso: string) {
  return new Date(iso + 'T00:00:00').toLocaleDateString('pt-BR')
}
const sortedAssessments = computed(() =>
  [...assessmentStore.assessments].sort((a, b) => b.assessedAt.localeCompare(a.assessedAt))
)

const goals = ref<Goal[]>([])
const plans = ref<WorkoutPlan[]>([])
const loadingPlans = ref(false)
const activatingPlan = ref<string | null>(null)
const showAssessment = ref(false)
const showGoal = ref(false)
const saving = ref(false)

const successMsg = ref('')
function showSuccess(msg: string) {
  successMsg.value = msg
  setTimeout(() => { successMsg.value = '' }, 3000)
}

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
  await Promise.all([
    assessmentStore.fetchAssessments(studentId).then(() => {
      if (sortedAssessments.value.length > 0) {
        expandedCards.value = new Set([sortedAssessments.value[0].id])
      }
    }),
    goalService.list(studentId).then(g => { goals.value = g }),
    (async () => {
      loadingPlans.value = true
      try {
        plans.value = await workoutService.listPlans(studentId)
        // auto-expand the active plan
        const active = plans.value.find(p => p.active)
        if (active) expandedPlans.value = new Set([active.id])
      } finally { loadingPlans.value = false }
    })(),
    exerciseService.search().then(exs => {
      exerciseNames.value = Object.fromEntries(exs.map(e => [e.id, e.name]))
    }),
  ])
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
  showSuccess('Status atualizado.')
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
    showSuccess('Avaliação registrada.')
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
    showSuccess('Meta criada.')
  } finally { saving.value = false }
}

async function activatePlan(planId: string) {
  activatingPlan.value = planId
  try {
    const updated = await workoutService.activatePlan(planId)
    plans.value = plans.value.map(p => ({ ...p, active: p.id === updated.id }))
    showSuccess('Ficha ativada.')
  } finally { activatingPlan.value = null }
}
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 16px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }
.tab-content { display: flex; flex-direction: column; gap: 10px; padding-top: 12px; }
.tab-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 4px; }
.tab-count { font-size: 13px; color: var(--neutral-500); }
.empty-state { text-align: center; padding: 32px; color: var(--neutral-400); font-size: 13px; }
.success-msg { color: #22c55e; font-size: 0.875rem; margin-top: 0; }

/* Remaining goal/plan list items */
.list-item {
  display: flex; align-items: center; gap: 12px; flex-wrap: wrap;
  padding: 10px 14px; border-radius: var(--radius-md);
  background: #fff; border: 1px solid var(--neutral-200);
}

/* ── Assessment cards ── */
.assessment-card { border: 1px solid var(--neutral-200); border-radius: var(--radius-md); overflow: hidden; }
.acard-header {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 14px; cursor: pointer;
  background: var(--neutral-50); transition: background 0.15s;
}
.acard-header:hover { background: var(--neutral-100); }
.acard-date-wrap { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.acard-badge {
  font-size: 11px; font-weight: 700; background: var(--blue-500); color: #fff;
  padding: 2px 7px; border-radius: 10px;
}
.acard-date { font-size: 13px; font-weight: 700; color: var(--neutral-800); white-space: nowrap; }
.acard-summary {
  flex: 1; display: flex; gap: 10px; flex-wrap: wrap;
  font-size: 12px; color: var(--neutral-500); font-weight: 500;
}
.acard-chevron { font-size: 12px; color: var(--neutral-400); flex-shrink: 0; }
.acard-body { padding: 14px; border-top: 1px solid var(--neutral-200); background: #fff; }

.acard-metrics {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 8px; margin-bottom: 14px;
}
.metric-box {
  background: var(--neutral-50); border-radius: var(--radius-md);
  padding: 8px 10px; display: flex; flex-direction: column; gap: 2px;
}
.metric-lbl { font-size: 10px; font-weight: 600; color: var(--neutral-500); text-transform: uppercase; letter-spacing: .04em; }
.metric-val { font-family: var(--font-display); font-size: 20px; font-weight: 700; color: var(--neutral-900); }
.metric-val small { font-size: 12px; font-weight: 500; color: var(--neutral-400); }

.measures-title { font-size: 12px; font-weight: 700; color: var(--neutral-600); margin: 0 0 8px; text-transform: uppercase; letter-spacing: .04em; }
.measures-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(130px, 1fr)); gap: 6px; margin-bottom: 10px; }
.measure-item {
  display: flex; justify-content: space-between; align-items: center;
  background: var(--neutral-50); border-radius: 8px; padding: 6px 10px;
}
.measure-part { font-size: 11px; font-weight: 600; color: var(--neutral-700); text-transform: capitalize; }
.measure-val  { font-size: 12px; font-weight: 700; color: var(--neutral-900); }

.acard-notes {
  display: flex; align-items: flex-start; gap: 8px; font-size: 12px; color: var(--neutral-600);
  background: #fffbeb; border: 1px solid #fde68a;
  border-radius: var(--radius-md); padding: 8px 10px;
}

/* ── Plan cards ── */
.plan-card { border: 1px solid var(--neutral-200); border-radius: var(--radius-md); overflow: hidden; }
.plan-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 14px; cursor: pointer;
  background: var(--neutral-50); transition: background 0.15s; gap: 10px;
}
.plan-header:hover { background: var(--neutral-100); }
.plan-header-left { display: flex; align-items: center; gap: 10px; flex: 1; overflow: hidden; }
.plan-name { font-size: 13px; font-weight: 700; color: var(--neutral-800); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.plan-header-actions { display: flex; align-items: center; gap: 4px; flex-shrink: 0; }
.plan-body { padding: 14px; border-top: 1px solid var(--neutral-200); background: #fff; }
.plan-desc { font-size: 12px; color: var(--neutral-500); margin-bottom: 12px; font-style: italic; }

.division-group { margin-bottom: 16px; }
.division-group:last-child { margin-bottom: 0; }
.division-label {
  font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: .06em;
  color: var(--blue-600); background: #eff6ff; border-radius: 6px;
  padding: 3px 10px; display: inline-block; margin-bottom: 8px;
}

.exercise-table { display: flex; flex-direction: column; gap: 0; border: 1px solid var(--neutral-200); border-radius: var(--radius-md); overflow: hidden; }
.ex-row { display: grid; grid-template-columns: 2fr 1fr 1fr 1fr 2fr; gap: 0; align-items: center; }
.ex-header { background: var(--neutral-100); padding: 6px 10px; }
.ex-header span { font-size: 10px; font-weight: 700; color: var(--neutral-500); text-transform: uppercase; letter-spacing: .04em; padding: 0 6px; }
.ex-row:not(.ex-header) { padding: 8px 10px; border-top: 1px solid var(--neutral-100); }
.ex-row:not(.ex-header):hover { background: var(--neutral-50); }
.ex-col-name { font-size: 13px; font-weight: 600; color: var(--neutral-800); padding: 0 6px; }
.ex-col-num { font-size: 12px; color: var(--neutral-600); padding: 0 6px; }
.ex-col-notes { font-size: 11px; color: var(--neutral-400); padding: 0 6px; font-style: italic; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* ── Assessment form dialog ── */
.assessment-form { display: flex; flex-direction: column; gap: 18px; padding: 8px 0 4px; }
.form-row { display: flex; gap: 12px; }
.goal-form { display: flex; flex-direction: column; gap: 20px; padding: 8px 0 4px; }
.form-field { display: flex; flex-direction: column; gap: 6px; flex: 1; }
.form-label { font-size: 13px; font-weight: 600; color: var(--neutral-700); }
.measures-header { display: flex; align-items: center; justify-content: space-between; }
.measure-row { display: flex; align-items: center; gap: 8px; margin-top: 6px; }
.form-actions { display: flex; justify-content: flex-end; gap: 8px; padding-top: 4px; }
</style>
