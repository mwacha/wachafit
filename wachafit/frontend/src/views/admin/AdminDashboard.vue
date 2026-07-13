<template>
  <AppLayout>
    <div class="dash">
      <div v-if="loading" class="empty-state">Carregando...</div>
      <div v-else class="kpi-grid">
        <div class="kpi-card" v-for="kpi in kpis" :key="kpi.label">
          <span class="kpi-label">{{ kpi.label }}</span>
          <span class="kpi-value">{{ kpi.value }}</span>
          <span v-if="kpi.sub" class="kpi-sub">{{ kpi.sub }}</span>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useAdminStore } from '@/stores/admin.store'
import reportService from '@/services/report.service'
import { scheduleService } from '@/services/schedule.service'

const adminStore = useAdminStore()
const loading = ref(true)
const activeStudents = ref(0)
const overdueCount = ref(0)
const schedulesToday = ref(0)

onMounted(async () => {
  const today = new Date().toISOString().split('T')[0]
  await Promise.all([
    adminStore.fetchUsers(),
    reportService.getSubscriptionStats().then(s => { activeStudents.value = s.active }),
    reportService.getOverdue().then(o => { overdueCount.value = o.length }),
    scheduleService.list({ date: today }).then(s => {
      schedulesToday.value = s.filter(sc => sc.status !== 'CANCELLED').length
    }),
  ])
  loading.value = false
})

const trainersCount = computed(() =>
  adminStore.users.filter(u => u.role === 'TRAINER' && u.active).length
)

const kpis = computed(() => [
  { label: 'Alunos ativos',    value: activeStudents.value,  sub: overdueCount.value > 0 ? `${overdueCount.value} inadimplentes` : null },
  { label: 'Profissionais',    value: trainersCount.value,   sub: null },
  { label: 'Aulas hoje',       value: schedulesToday.value,  sub: null },
  { label: 'Inadimplentes',    value: overdueCount.value,    sub: null },
])
</script>

<style scoped>
.dash { display: flex; flex-direction: column; gap: 20px; }

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.kpi-card {
  background: #fff; border: 1px solid var(--neutral-200);
  border-radius: var(--radius-lg); padding: 18px 16px;
  display: flex; flex-direction: column; gap: 6px;
  box-shadow: var(--shadow-card);
  transition: box-shadow 0.2s, transform 0.2s;
}
.kpi-card:hover { box-shadow: 0 4px 20px rgba(0,0,0,0.08); transform: translateY(-2px); }

.kpi-label {
  font-size: 11px; font-weight: 500; color: var(--neutral-500);
  text-transform: uppercase; letter-spacing: 0.06em;
}
.kpi-value {
  font-family: var(--font-display); font-size: 28px; font-weight: 700;
  color: var(--neutral-900); line-height: 1.1;
}
.kpi-sub { font-size: 11px; color: var(--neutral-400); }

.empty-state {
  text-align: center; padding: 48px; color: var(--neutral-500); font-size: 14px;
}
</style>
