<!-- frontend/src/views/student/EvolutionView.vue -->
<template>
  <AppLayout>
    <div class="view-wrap">
      <div class="page-header">
        <h1 class="page-title">Evolução</h1>
        <Button icon="pi pi-download" label="Baixar PDF" outlined :loading="downloadingPdf" @click="downloadEvolutionPdf" />
      </div>

      <div v-if="assessmentStore.loading" class="empty-state">
        <i class="pi pi-spin pi-spinner empty-icon" />
        <p>Carregando...</p>
      </div>

      <div v-else-if="assessmentStore.evolution.length === 0" class="empty-state">
        <i class="pi pi-chart-line empty-icon" />
        <p>Nenhuma avaliação registrada ainda.</p>
        <p class="empty-hint">Peça ao seu personal para registrar sua primeira avaliação.</p>
      </div>

      <template v-else>
        <!-- Gráfico de linha -->
        <div class="section-card">
          <h2 class="section-title">Progresso ao longo do tempo</h2>
          <div class="metric-toggles">
            <button v-for="m in metrics" :key="m.key"
              :class="['toggle-btn', { active: m.visible }]"
              :style="m.visible ? { background: m.color + '22', borderColor: m.color, color: m.color } : {}"
              @click="m.visible = !m.visible">
              {{ m.label }}
            </button>
          </div>
          <Chart type="line" :data="chartData" :options="chartOptions" class="evolution-chart" />
        </div>

        <!-- Tabela histórica -->
        <div class="section-card">
          <h2 class="section-title">Histórico de avaliações</h2>
          <div class="table-scroll">
            <DataTable :value="assessmentStore.evolution" stripedRows>
              <Column field="assessedAt" header="Data" style="min-width:110px" />
              <Column header="Peso (kg)" style="min-width:100px">
                <template #body="{ data }">{{ data.weightKg ?? '—' }}</template>
              </Column>
              <Column header="% Gordura" style="min-width:100px">
                <template #body="{ data }">{{ data.bodyFatPct ?? '—' }}</template>
              </Column>
              <Column header="IMC" style="min-width:80px">
                <template #body="{ data }">{{ data.bmi ?? '—' }}</template>
              </Column>
            </DataTable>
          </div>
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
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Chart from 'primevue/chart'
import api from '@/services/api'

const assessmentStore = useAssessmentStore()
const authStore = useAuthStore()
const downloadingPdf = ref(false)

const metrics = reactive([
  { key: 'weightKg',   label: 'Peso (kg)',  color: '#3b82f6', visible: true },
  { key: 'bodyFatPct', label: '% Gordura',  color: '#f97316', visible: true },
  { key: 'bmi',        label: 'IMC',        color: '#8b5cf6', visible: false },
])

const chartData = computed(() => {
  const pts = assessmentStore.evolution
  return {
    labels: pts.map(p => p.assessedAt),
    datasets: metrics
      .filter(m => m.visible)
      .map(m => ({
        label: m.label,
        data: pts.map(p => (p as any)[m.key]),
        borderColor: m.color,
        backgroundColor: m.color + '22',
        fill: false,
        tension: 0.3,
        pointRadius: 5,
        pointHoverRadius: 7,
        spanGaps: true,
      })),
  }
})

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    tooltip: { mode: 'index', intersect: false },
  },
  scales: {
    x: { grid: { color: '#f1f5f9' } },
    y: { grid: { color: '#f1f5f9' }, beginAtZero: false },
  },
}

onMounted(() => assessmentStore.fetchAssessments(authStore.userId!))

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
.view-wrap { display: flex; flex-direction: column; gap: 20px; max-width: 860px; }
.page-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }

.section-card {
  background: #fff; border: 1px solid var(--neutral-200);
  border-radius: var(--radius-lg); padding: 20px 20px 16px;
  box-shadow: var(--shadow-card);
}
.section-title { font-size: 15px; font-weight: 600; color: var(--neutral-800); margin-bottom: 12px; }

.metric-toggles { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 14px; }
.toggle-btn {
  padding: 4px 14px; border-radius: 20px; border: 1.5px solid var(--neutral-200);
  background: transparent; color: var(--neutral-500);
  font-size: 12px; font-weight: 600; cursor: pointer; transition: all 0.15s;
}
.toggle-btn:hover { border-color: var(--neutral-400); }

.evolution-chart { height: 280px; }
.table-scroll { overflow-x: auto; border-radius: var(--radius-md); }

.empty-state {
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  text-align: center; padding: 48px 24px;
  background: #fff; border: 1px solid var(--neutral-200);
  border-radius: var(--radius-lg); color: var(--neutral-500); font-size: 14px;
}
.empty-icon { font-size: 2.5rem; color: var(--neutral-300); }
.empty-hint { font-size: 13px; }
</style>
