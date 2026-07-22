<template>
  <AppLayout>
    <div class="dash">

      <!-- KPIs -->
      <div class="kpi-grid">
        <div class="kpi-card kpi-highlight">
          <span class="kpi-label kpi-label-white">Alunos ativos</span>
          <span class="kpi-value kpi-value-white">{{ activeStudents }}</span>
          <span class="kpi-sub kpi-sub-white">{{ overdueCount > 0 ? `${overdueCount} inadimplentes` : 'sem inadimplência' }}</span>
        </div>
        <div class="kpi-card">
          <span class="kpi-label">Profissionais</span>
          <span class="kpi-value">{{ trainersCount }}</span>
          <span class="kpi-sub">ativos</span>
        </div>
        <div class="kpi-card">
          <span class="kpi-label">Aulas hoje</span>
          <span class="kpi-value">{{ schedulesToday }}</span>
          <span class="kpi-sub">agendadas</span>
        </div>
        <div class="kpi-card" :class="overdueCount > 0 ? 'kpi-danger' : ''">
          <span class="kpi-label">Inadimplentes</span>
          <span class="kpi-value">{{ overdueCount }}</span>
          <span class="kpi-sub">cobranças vencidas</span>
        </div>
      </div>

      <!-- Gráficos linha 1 -->
      <div class="charts-row">

        <!-- Matrículas nos últimos 12 meses -->
        <div class="chart-card chart-wide">
          <div class="chart-header">
            <div>
              <h2 class="chart-title">Novas Matrículas</h2>
              <p class="chart-sub">Últimos 12 meses</p>
            </div>
            <span :class="['trend-badge', enrollmentTrend >= 0 ? 'trend-up' : 'trend-down']">
              <i :class="enrollmentTrend >= 0 ? 'pi pi-arrow-up' : 'pi pi-arrow-down'" />
              {{ Math.abs(enrollmentTrend) }}% vs mês anterior
            </span>
          </div>
          <Chart v-if="enrollmentData" type="line" :data="enrollmentData" :options="lineOptions" class="chart-canvas" />
          <div v-else class="chart-loading">Carregando...</div>
        </div>

        <!-- Receita mensal -->
        <div class="chart-card">
          <div class="chart-header">
            <div>
              <h2 class="chart-title">Receita Mensal</h2>
              <p class="chart-sub">Últimos 12 meses</p>
            </div>
          </div>
          <Chart v-if="revenueData" type="bar" :data="revenueData" :options="barOptions" class="chart-canvas" />
          <div v-else class="chart-loading">Carregando...</div>
        </div>

      </div>

      <!-- Ranking de frequência -->
      <div class="chart-card">
        <div class="chart-header">
          <div>
            <h2 class="chart-title">Ranking de Frequência</h2>
            <p class="chart-sub">Top 10 alunos — últimos 30 dias</p>
          </div>
        </div>
        <div v-if="!attendanceData" class="chart-loading">Carregando...</div>
        <div v-else-if="ranking.length === 0" class="chart-loading">Nenhuma aula confirmada no período.</div>
        <div v-else class="ranking-list">
          <div v-for="(item, i) in ranking" :key="item.studentName" class="ranking-item">
            <span :class="['rank-pos', i < 3 ? `pos-${i}` : '']">{{ i + 1 }}º</span>
            <span class="rank-name">{{ item.studentName }}</span>
            <div class="rank-bar-wrap">
              <div class="rank-bar" :style="{ width: `${(item.bookingCount / ranking[0].bookingCount) * 100}%` }" />
            </div>
            <span class="rank-count">{{ item.bookingCount }} aulas</span>
          </div>
        </div>
      </div>

    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import Chart from 'primevue/chart'
import { useAdminStore } from '@/stores/admin.store'
import reportService from '@/services/report.service'
import { scheduleService } from '@/services/schedule.service'
import type { EnrollmentTrend, AttendanceRank } from '@/types/api'

const adminStore = useAdminStore()
const loading = ref(true)
const activeStudents = ref(0)
const overdueCount = ref(0)
const schedulesToday = ref(0)
const ranking = ref<AttendanceRank[]>([])

const enrollmentData = ref<any>(null)
const revenueData = ref<any>(null)
const attendanceData = ref<boolean>(false)

const trainersCount = computed(() =>
  adminStore.users.filter(u => (u.role === 'TRAINER' || u.role === 'PROFESSOR') && u.active).length
)

// % variação última vs penúltima entrada de matrícula
const enrollmentTrend = ref(0)

const lineOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false } },
  scales: {
    y: { beginAtZero: true, ticks: { stepSize: 1 }, grid: { color: '#f1f5f9' } },
    x: { grid: { display: false } },
  },
}

const barOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false } },
  scales: {
    y: { beginAtZero: true, ticks: { callback: (v: number) => `R$ ${v}` }, grid: { color: '#f1f5f9' } },
    x: { grid: { display: false } },
  },
}

function monthLabel(ym: string) {
  const [y, m] = ym.split('-')
  const names = ['Jan','Fev','Mar','Abr','Mai','Jun','Jul','Ago','Set','Out','Nov','Dez']
  return `${names[parseInt(m) - 1]}/${y.slice(2)}`
}

function buildEnrollmentChart(trend: EnrollmentTrend[]) {
  const labels = trend.map(t => monthLabel(t.month))
  const values = trend.map(t => t.newEnrollments)

  // tendência: última vs penúltima
  if (values.length >= 2) {
    const last = values[values.length - 1]
    const prev = values[values.length - 2]
    enrollmentTrend.value = prev === 0 ? 0 : Math.round(((last - prev) / prev) * 100)
  }

  enrollmentData.value = {
    labels,
    datasets: [{
      label: 'Matrículas',
      data: values,
      fill: true,
      tension: 0.4,
      borderColor: '#3b82f6',
      backgroundColor: 'rgba(59,130,246,0.10)',
      pointBackgroundColor: '#3b82f6',
      pointRadius: 4,
    }],
  }
}

function buildRevenueChart(data: { month: string; total: number }[]) {
  revenueData.value = {
    labels: data.map(d => monthLabel(d.month)),
    datasets: [{
      label: 'Receita',
      data: data.map(d => d.total),
      backgroundColor: 'rgba(16,185,129,0.75)',
      borderRadius: 6,
    }],
  }
}

onMounted(async () => {
  const today = new Date()
  const todayStr = today.toISOString().split('T')[0]
  const from12m = `${today.getFullYear() - (today.getMonth() === 0 ? 1 : 0)}-01`
  const toNow = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}`

  await Promise.all([
    adminStore.fetchUsers(),
    reportService.getSubscriptionStats()
      .then(s => { activeStudents.value = s.active }).catch(() => {}),
    reportService.getOverdue()
      .then(o => { overdueCount.value = o.length }).catch(() => {}),
    scheduleService.list({ date: todayStr })
      .then(s => { schedulesToday.value = s.filter(sc => sc.status !== 'CANCELLED').length }).catch(() => {}),
    reportService.getEnrollmentTrend(12)
      .then(buildEnrollmentChart).catch(() => { enrollmentData.value = { labels: [], datasets: [] } }),
    reportService.getRevenue(from12m, toNow)
      .then(buildRevenueChart).catch(() => { revenueData.value = { labels: [], datasets: [] } }),
    reportService.getAttendanceRanking(30, 10)
      .then(r => { ranking.value = r; attendanceData.value = true }).catch(() => { attendanceData.value = true }),
  ])
  loading.value = false
})
</script>

<style scoped>
.dash { display: flex; flex-direction: column; gap: 20px; }

/* KPIs */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
}
.kpi-card {
  background: #fff; border: 1px solid var(--neutral-200);
  border-radius: var(--radius-lg); padding: 18px 16px;
  display: flex; flex-direction: column; gap: 4px;
  box-shadow: var(--shadow-card);
  transition: box-shadow 0.2s, transform 0.2s;
}
.kpi-card:hover { box-shadow: 0 4px 20px rgba(0,0,0,0.08); transform: translateY(-2px); }
.kpi-highlight {
  background: linear-gradient(135deg, var(--blue-500), var(--blue-700));
  border-color: transparent; box-shadow: var(--shadow-btn);
}
.kpi-danger { border-left: 3px solid #ef4444; }
.kpi-label {
  font-size: 11px; font-weight: 600; color: var(--neutral-500);
  text-transform: uppercase; letter-spacing: 0.06em;
}
.kpi-label-white { color: rgba(255,255,255,0.7); }
.kpi-value {
  font-family: var(--font-display); font-size: 30px; font-weight: 700;
  color: var(--neutral-900); line-height: 1.1;
}
.kpi-value-white { color: #fff; }
.kpi-sub { font-size: 11px; color: var(--neutral-400); }
.kpi-sub-white { color: rgba(255,255,255,0.6); }

/* Gráficos */
.charts-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
}
@media (max-width: 768px) {
  .charts-row { grid-template-columns: 1fr; }
}

.chart-card {
  background: #fff; border: 1px solid var(--neutral-200);
  border-radius: var(--radius-lg); padding: 20px;
  box-shadow: var(--shadow-card);
}
.chart-header {
  display: flex; align-items: flex-start; justify-content: space-between;
  margin-bottom: 16px; gap: 12px; flex-wrap: wrap;
}
.chart-title {
  font-size: 15px; font-weight: 700; color: var(--neutral-900); margin: 0;
}
.chart-sub { font-size: 12px; color: var(--neutral-500); margin: 2px 0 0; }

.chart-canvas { height: 220px; }

.chart-loading {
  height: 220px; display: flex; align-items: center; justify-content: center;
  color: var(--neutral-400); font-size: 13px;
}

.trend-badge {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 12px; font-weight: 600; padding: 4px 10px;
  border-radius: 20px; white-space: nowrap;
}
.trend-up   { background: #dcfce7; color: #16a34a; }
.trend-down { background: #fee2e2; color: #dc2626; }

/* Ranking */
.ranking-list { display: flex; flex-direction: column; gap: 10px; }
.ranking-item {
  display: flex; align-items: center; gap: 12px;
}
.rank-pos {
  font-size: 13px; font-weight: 700; color: var(--neutral-400);
  min-width: 26px; text-align: right;
}
.pos-0 { color: #f59e0b; }
.pos-1 { color: #94a3b8; }
.pos-2 { color: #b45309; }
.rank-name { font-size: 13px; font-weight: 600; color: var(--neutral-800); min-width: 140px; }
.rank-bar-wrap {
  flex: 1; height: 8px; background: var(--neutral-100);
  border-radius: 4px; overflow: hidden;
}
.rank-bar { height: 100%; background: #3b82f6; border-radius: 4px; transition: width 0.4s; }
.rank-count { font-size: 12px; color: var(--neutral-500); white-space: nowrap; min-width: 60px; text-align: right; }
</style>
