<!-- frontend/src/views/student/EvolutionView.vue -->
<template>
  <AppLayout>
    <div class="view-wrap">
      <div class="page-header">
        <h1 class="page-title">Evolução Física</h1>
        <Button icon="pi pi-download" label="Baixar PDF" outlined :loading="downloadingPdf" @click="downloadEvolutionPdf" />
      </div>

      <div v-if="assessmentStore.loading" class="empty-state">
        <i class="pi pi-spin pi-spinner empty-icon" />
        <p>Carregando...</p>
      </div>

      <div v-else-if="assessmentStore.assessments.length === 0" class="empty-state">
        <i class="pi pi-chart-line empty-icon" />
        <p>Nenhuma avaliação registrada ainda.</p>
        <p class="empty-hint">Peça ao seu personal para registrar sua primeira avaliação.</p>
      </div>

      <template v-else>

        <!-- KPIs da última avaliação vs primeira -->
        <div class="kpi-strip">
          <div v-for="kpi in kpis" :key="kpi.label" class="kpi-item">
            <span class="kpi-label">{{ kpi.label }}</span>
            <span class="kpi-value">{{ kpi.value }}</span>
            <span v-if="kpi.delta !== null" :class="['kpi-delta', kpi.deltaGood ? 'delta-good' : 'delta-bad']">
              <i :class="kpi.deltaGood ? 'pi pi-arrow-down' : 'pi pi-arrow-up'" />
              {{ Math.abs(kpi.delta).toFixed(1) }} vs 1ª avaliação
            </span>
          </div>
        </div>

        <!-- Gráfico peso / gordura / IMC -->
        <div class="section-card">
          <div class="section-header">
            <h2 class="section-title">Progresso ao longo do tempo</h2>
            <div class="metric-toggles">
              <button v-for="m in metrics" :key="m.key"
                :class="['toggle-btn', { active: m.visible }]"
                :style="m.visible ? { background: m.color + '22', borderColor: m.color, color: m.color } : {}"
                @click="m.visible = !m.visible">
                {{ m.label }}
              </button>
            </div>
          </div>
          <Chart type="line" :data="chartData" :options="chartOptions" class="evolution-chart" />
        </div>

        <!-- Gráfico medidas corporais -->
        <div v-if="measurementSeries.length > 0" class="section-card">
          <div class="section-header">
            <h2 class="section-title">Medidas corporais</h2>
            <div class="metric-toggles">
              <button v-for="s in measurementSeries" :key="s.label"
                :class="['toggle-btn', { active: s.visible }]"
                :style="s.visible ? { background: s.color + '22', borderColor: s.color, color: s.color } : {}"
                @click="s.visible = !s.visible">
                {{ s.label }}
              </button>
            </div>
          </div>
          <Chart type="line" :data="measurementChartData" :options="measurementChartOptions" class="evolution-chart" />
        </div>

        <!-- Histórico de avaliações (cards) -->
        <div class="section-card">
          <h2 class="section-title">Histórico de avaliações</h2>
          <div class="assessments-list">
            <div v-for="(a, idx) in sortedAssessments" :key="a.id" class="assessment-card">
              <!-- Cabeçalho do card -->
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

              <!-- Detalhes expansíveis -->
              <div v-if="expandedCards.has(a.id)" class="acard-body">
                <!-- Métricas principais -->
                <div class="acard-metrics">
                  <div v-if="a.weightKg" class="metric-box">
                    <span class="metric-label">Peso</span>
                    <span class="metric-val">{{ a.weightKg }} <small>kg</small></span>
                  </div>
                  <div v-if="a.heightCm" class="metric-box">
                    <span class="metric-label">Altura</span>
                    <span class="metric-val">{{ a.heightCm }} <small>cm</small></span>
                  </div>
                  <div v-if="a.bodyFatPct" class="metric-box">
                    <span class="metric-label">% Gordura</span>
                    <span class="metric-val">{{ a.bodyFatPct }}<small>%</small></span>
                  </div>
                  <div v-if="a.bmi" class="metric-box">
                    <span class="metric-label">IMC</span>
                    <span class="metric-val">{{ a.bmi }}</span>
                    <span class="metric-tag" :class="bmiClass(Number(a.bmi))">{{ bmiLabel(Number(a.bmi)) }}</span>
                  </div>
                </div>

                <!-- Medidas corporais -->
                <template v-if="a.measurements?.length">
                  <h4 class="measures-title">Medidas corporais</h4>
                  <div class="measures-grid">
                    <div v-for="m in a.measurements" :key="m.bodyPart" class="measure-item">
                      <span class="measure-part">{{ m.bodyPart }}</span>
                      <span class="measure-val">{{ m.valueCm }} cm</span>
                    </div>
                  </div>
                </template>

                <!-- Notas -->
                <div v-if="a.notes" class="acard-notes">
                  <i class="pi pi-comment" />
                  {{ a.notes }}
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Progressão de Cargas -->
        <div class="section-card">
          <h2 class="section-title">Progressão de Cargas</h2>
          <Select
            v-model="selectedExerciseId"
            :options="exercises"
            optionLabel="name"
            optionValue="id"
            placeholder="Selecione um exercício"
            filter
            class="exercise-select"
            @change="loadProgression"
          />
          <div v-if="loadingProgression" class="chart-empty"><i class="pi pi-spin pi-spinner" /></div>
          <p v-else-if="selectedExerciseId && progressionPoints.length === 0" class="empty-msg">
            Nenhum registro encontrado para este exercício.
          </p>
          <Chart v-else-if="progressionPoints.length > 0"
            type="line" :data="progressionChartData" :options="progressionChartOptions"
            class="progression-chart" />
        </div>

      </template>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useAssessmentStore } from '@/stores/assessment.store'
import { useAuthStore } from '@/stores/auth.store'
import Button from 'primevue/button'
import Chart from 'primevue/chart'
import api from '@/services/api'
import { exerciseService } from '@/services/exercise.service'
import { workoutService } from '@/services/workout.service'
import Select from 'primevue/select'
import type { Exercise, ProgressionPoint } from '@/types/api'

const assessmentStore = useAssessmentStore()
const authStore = useAuthStore()
const downloadingPdf = ref(false)
const exercises = ref<Exercise[]>([])
const selectedExerciseId = ref<string | null>(null)
const progressionPoints = ref<ProgressionPoint[]>([])
const loadingProgression = ref(false)
const expandedCards = ref<Set<string>>(new Set())

const metrics = reactive([
  { key: 'weightKg',   label: 'Peso (kg)',  color: '#3b82f6', visible: true },
  { key: 'bodyFatPct', label: '% Gordura',  color: '#f97316', visible: true },
  { key: 'bmi',        label: 'IMC',        color: '#8b5cf6', visible: false },
])

// Assessments ordenados do mais recente para o mais antigo
const sortedAssessments = computed(() =>
  [...assessmentStore.assessments].sort((a, b) => b.assessedAt.localeCompare(a.assessedAt))
)

// KPIs: comparar última vs primeira avaliação
const kpis = computed(() => {
  const list = [...assessmentStore.assessments].sort((a, b) => a.assessedAt.localeCompare(b.assessedAt))
  if (list.length === 0) return []
  const first = list[0]
  const last  = list[list.length - 1]
  const result = []

  if (last.weightKg != null) result.push({
    label: 'Peso atual', value: `${last.weightKg} kg`,
    delta: list.length > 1 && first.weightKg != null ? Number(last.weightKg) - Number(first.weightKg) : null,
    deltaGood: true, // perder peso = bom (delta negativo = bom)
  })
  if (last.bodyFatPct != null) result.push({
    label: '% Gordura atual', value: `${last.bodyFatPct}%`,
    delta: list.length > 1 && first.bodyFatPct != null ? Number(last.bodyFatPct) - Number(first.bodyFatPct) : null,
    deltaGood: true,
  })
  if (last.bmi != null) result.push({
    label: 'IMC atual', value: String(last.bmi),
    delta: null, deltaGood: true,
  })
  result.push({ label: 'Avaliações', value: String(list.length), delta: null, deltaGood: true })
  return result
})

function toggleCard(id: string) {
  const s = new Set(expandedCards.value)
  if (s.has(id)) s.delete(id); else s.add(id)
  expandedCards.value = s
}

// Gráfico principal
const chartData = computed(() => {
  const pts = assessmentStore.evolution
  return {
    labels: pts.map(p => formatDate(p.assessedAt)),
    datasets: metrics
      .filter(m => m.visible)
      .map(m => ({
        label: m.label,
        data: pts.map(p => (p as any)[m.key]),
        borderColor: m.color,
        backgroundColor: m.color + '22',
        fill: false, tension: 0.3, pointRadius: 5, pointHoverRadius: 7, spanGaps: true,
      })),
  }
})

// Gráfico de medidas corporais
const MEASURE_COLORS = ['#06b6d4','#10b981','#f59e0b','#ef4444','#a855f7','#ec4899']

const measurementSeries = computed(() => {
  const allParts = new Map<string, { date: string; value: number }[]>()
  const asc = [...assessmentStore.assessments].sort((a, b) => a.assessedAt.localeCompare(b.assessedAt))
  for (const a of asc) {
    for (const m of (a.measurements ?? [])) {
      if (!allParts.has(m.bodyPart)) allParts.set(m.bodyPart, [])
      allParts.get(m.bodyPart)!.push({ date: formatDate(a.assessedAt), value: Number(m.valueCm) })
    }
  }
  return Array.from(allParts.entries()).map(([label, pts], i) => ({
    label, pts, color: MEASURE_COLORS[i % MEASURE_COLORS.length], visible: i < 4,
  }))
})

const allMeasureDates = computed(() => {
  const dates = new Set<string>()
  const asc = [...assessmentStore.assessments].sort((a, b) => a.assessedAt.localeCompare(b.assessedAt))
  for (const a of asc) if (a.measurements?.length) dates.add(formatDate(a.assessedAt))
  return Array.from(dates)
})

const measurementChartData = computed(() => ({
  labels: allMeasureDates.value,
  datasets: measurementSeries.value.filter(s => s.visible).map(s => ({
    label: s.label,
    data: allMeasureDates.value.map(d => s.pts.find(p => p.date === d)?.value ?? null),
    borderColor: s.color,
    backgroundColor: s.color + '22',
    fill: false, tension: 0.3, pointRadius: 5, spanGaps: true,
  })),
}))

const chartOptions = {
  responsive: true, maintainAspectRatio: false,
  plugins: { legend: { display: false }, tooltip: { mode: 'index', intersect: false } },
  scales: {
    x: { grid: { color: '#f1f5f9' } },
    y: { grid: { color: '#f1f5f9' }, beginAtZero: false },
  },
}

const measurementChartOptions = {
  responsive: true, maintainAspectRatio: false,
  plugins: { legend: { display: true, position: 'bottom' as const }, tooltip: { mode: 'index', intersect: false } },
  scales: {
    x: { grid: { color: '#f1f5f9' } },
    y: { grid: { color: '#f1f5f9' }, beginAtZero: false, ticks: { callback: (v: number) => `${v} cm` } },
  },
}

// Progressão de cargas
const progressionChartData = computed(() => ({
  labels: progressionPoints.value.map(p => p.performedAt),
  datasets: [{
    label: 'Carga (kg)', data: progressionPoints.value.map(p => p.loadKg),
    borderColor: '#22c55e', backgroundColor: '#22c55e22',
    fill: false, tension: 0.3, pointRadius: 5, spanGaps: true,
  }],
}))
const progressionChartOptions = {
  responsive: true, maintainAspectRatio: false,
  plugins: { legend: { display: false }, tooltip: { mode: 'index', intersect: false } },
  scales: {
    x: { grid: { color: '#f1f5f9' } },
    y: { grid: { color: '#f1f5f9' }, beginAtZero: true },
  },
}

function formatDate(iso: string) {
  return new Date(iso + 'T00:00:00').toLocaleDateString('pt-BR')
}

function bmiLabel(bmi: number) {
  if (bmi < 18.5) return 'Abaixo do peso'
  if (bmi < 25)   return 'Normal'
  if (bmi < 30)   return 'Sobrepeso'
  return 'Obesidade'
}
function bmiClass(bmi: number) {
  if (bmi < 18.5) return 'bmi-low'
  if (bmi < 25)   return 'bmi-ok'
  if (bmi < 30)   return 'bmi-warn'
  return 'bmi-danger'
}

onMounted(async () => {
  await assessmentStore.fetchAssessments(authStore.userId!)
  exercises.value = await exerciseService.search()
  // Auto-expand a avaliação mais recente
  if (assessmentStore.assessments.length > 0) {
    const latest = sortedAssessments.value[0]
    if (latest) expandedCards.value = new Set([latest.id])
  }
})

async function loadProgression() {
  if (!selectedExerciseId.value) return
  loadingProgression.value = true
  try {
    progressionPoints.value = await workoutService.progression(authStore.userId!, selectedExerciseId.value)
  } finally { loadingProgression.value = false }
}

async function downloadEvolutionPdf() {
  if (!authStore.userId) return
  downloadingPdf.value = true
  try {
    const res = await api.get(`/api/students/${authStore.userId}/pdf/evolution`, { responseType: 'blob' })
    const url = URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }))
    const a = document.createElement('a'); a.href = url; a.download = 'evolucao.pdf'; a.click()
    URL.revokeObjectURL(url)
  } finally { downloadingPdf.value = false }
}
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 20px }
.page-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }

/* KPI strip */
.kpi-strip {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
}
.kpi-item {
  background: #fff; border: 1px solid var(--neutral-200);
  border-radius: var(--radius-lg); padding: 14px 16px;
  display: flex; flex-direction: column; gap: 4px;
  box-shadow: var(--shadow-card);
}
.kpi-label { font-size: 11px; font-weight: 600; color: var(--neutral-500); text-transform: uppercase; letter-spacing: .05em; }
.kpi-value { font-family: var(--font-display); font-size: 24px; font-weight: 700; color: var(--neutral-900); }
.kpi-delta { font-size: 11px; font-weight: 600; display: flex; align-items: center; gap: 3px; }
.delta-good { color: #16a34a; }
.delta-bad  { color: #dc2626; }

/* Section cards */
.section-card {
  background: #fff; border: 1px solid var(--neutral-200);
  border-radius: var(--radius-lg); padding: 20px;
  box-shadow: var(--shadow-card);
}
.section-header { display: flex; align-items: flex-start; justify-content: space-between; flex-wrap: wrap; gap: 10px; margin-bottom: 14px; }
.section-title { font-size: 15px; font-weight: 700; color: var(--neutral-800); margin: 0; }

.metric-toggles { display: flex; gap: 6px; flex-wrap: wrap; }
.toggle-btn {
  padding: 3px 12px; border-radius: 20px; border: 1.5px solid var(--neutral-200);
  background: transparent; color: var(--neutral-500);
  font-size: 12px; font-weight: 600; cursor: pointer; transition: all 0.15s;
}
.toggle-btn:hover { border-color: var(--neutral-400); }

.evolution-chart { height: 260px; }
.progression-chart { height: 200px; margin-top: 16px; }

/* Assessment cards */
.assessments-list { display: flex; flex-direction: column; gap: 10px; }
.assessment-card {
  border: 1px solid var(--neutral-200); border-radius: var(--radius-md);
  overflow: hidden;
}
.acard-header {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 16px; cursor: pointer;
  background: var(--neutral-50);
  transition: background 0.15s;
}
.acard-header:hover { background: var(--neutral-100); }
.acard-date-wrap { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.acard-badge {
  font-size: 11px; font-weight: 700;
  background: var(--blue-500); color: #fff;
  padding: 2px 7px; border-radius: 10px;
}
.acard-date { font-size: 13px; font-weight: 700; color: var(--neutral-800); white-space: nowrap; }
.acard-summary {
  flex: 1; display: flex; gap: 12px; flex-wrap: wrap;
  font-size: 13px; color: var(--neutral-500); font-weight: 500;
}
.acard-chevron { font-size: 13px; color: var(--neutral-400); flex-shrink: 0; }

.acard-body { padding: 16px; border-top: 1px solid var(--neutral-200); background: #fff; }

/* Métricas principais */
.acard-metrics {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
  gap: 10px;
  margin-bottom: 16px;
}
.metric-box {
  background: var(--neutral-50); border-radius: var(--radius-md);
  padding: 10px 12px; display: flex; flex-direction: column; gap: 2px;
}
.metric-label { font-size: 11px; font-weight: 600; color: var(--neutral-500); text-transform: uppercase; letter-spacing: .04em; }
.metric-val { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }
.metric-val small { font-size: 13px; font-weight: 500; color: var(--neutral-500); }
.metric-tag { font-size: 11px; font-weight: 600; padding: 2px 6px; border-radius: 6px; align-self: flex-start; }
.bmi-low    { background: #dbeafe; color: #1d4ed8; }
.bmi-ok     { background: #dcfce7; color: #166534; }
.bmi-warn   { background: #fef9c3; color: #854d0e; }
.bmi-danger { background: #fee2e2; color: #991b1b; }

/* Medidas corporais */
.measures-title { font-size: 13px; font-weight: 700; color: var(--neutral-700); margin: 0 0 10px; }
.measures-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 8px;
  margin-bottom: 12px;
}
.measure-item {
  display: flex; justify-content: space-between; align-items: center;
  background: var(--neutral-50); border-radius: 8px;
  padding: 7px 12px;
}
.measure-part { font-size: 12px; font-weight: 600; color: var(--neutral-700); text-transform: capitalize; }
.measure-val  { font-size: 13px; font-weight: 700; color: var(--neutral-900); }

/* Notas */
.acard-notes {
  display: flex; align-items: flex-start; gap: 8px;
  font-size: 13px; color: var(--neutral-600);
  background: #fffbeb; border: 1px solid #fde68a;
  border-radius: var(--radius-md); padding: 10px 12px;
}

/* Misc */
.empty-state {
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  text-align: center; padding: 48px 24px;
  background: #fff; border: 1px solid var(--neutral-200);
  border-radius: var(--radius-lg); color: var(--neutral-500); font-size: 14px;
}
.empty-icon { font-size: 2.5rem; color: var(--neutral-300); }
.empty-hint { font-size: 13px; }

.exercise-select { width: 100%; margin-bottom: 16px; }
.chart-empty { text-align: center; padding: 40px; color: var(--neutral-400); font-size: 14px; }
.empty-msg { color: var(--neutral-400); font-size: 13px; text-align: center; padding: 2rem 0; }
</style>
