<template>
  <AppLayout>
    <div class="dash">
      <div v-if="loading" class="empty-state">Carregando...</div>
      <div v-else class="kpi-grid">
        <div class="kpi-card kpi-highlight">
          <span class="kpi-label kpi-label-white">Aulas hoje</span>
          <span class="kpi-value kpi-value-white">{{ schedulesToday }}</span>
          <span class="kpi-badge badge-white">{{ schedulesToday === 1 ? 'Aula' : 'Aulas' }}</span>
        </div>
        <div class="kpi-card">
          <span class="kpi-label">Próxima aula</span>
          <span class="kpi-value" :class="nextScheduleTime ? '' : 'kpi-muted'">
            {{ nextScheduleTime || '—' }}
          </span>
          <span v-if="nextScheduleName" class="kpi-sub">{{ nextScheduleName }}</span>
          <span v-else class="kpi-sub">Sem aulas agendadas</span>
        </div>
        <div class="kpi-card">
          <span class="kpi-label">Alunos</span>
          <span class="kpi-value">{{ studentsCount }}</span>
          <span class="kpi-sub">cadastrados ativos</span>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { scheduleService } from '@/services/schedule.service'
import { useAuthStore } from '@/stores/auth.store'
import { useAdminStore } from '@/stores/admin.store'

const authStore = useAuthStore()
const adminStore = useAdminStore()
const loading = ref(true)
const todaySchedules = ref<Awaited<ReturnType<typeof scheduleService.list>>>([])

onMounted(async () => {
  const today = new Date().toISOString().split('T')[0]
  try {
    await Promise.all([
      scheduleService.list({ trainerId: authStore.userId ?? undefined, date: today }).then(s => {
        todaySchedules.value = s.filter(sc => sc.status !== 'CANCELLED')
      }).catch(() => {}),
      adminStore.fetchUsers().catch(() => {}),
    ])
  } finally {
    loading.value = false
  }
})

const schedulesToday = computed(() => todaySchedules.value.length)

const studentsCount = computed(() =>
  adminStore.users.filter(u => u.role === 'STUDENT' && u.active).length
)

const nextScheduleTime = computed(() => {
  const now = new Date().toISOString()
  const next = todaySchedules.value
    .filter(s => s.startsAt > now)
    .sort((a, b) => a.startsAt.localeCompare(b.startsAt))[0]
  if (!next) return null
  return new Date(next.startsAt).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
})

const nextScheduleName = computed(() => {
  const now = new Date().toISOString()
  const next = todaySchedules.value
    .filter(s => s.startsAt > now)
    .sort((a, b) => a.startsAt.localeCompare(b.startsAt))[0]
  if (!next) return null
  return next.type === 'CLASS' ? (next.groupClassName || 'Aula coletiva') : 'Sessão individual'
})
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

.kpi-highlight {
  background: linear-gradient(135deg, var(--blue-500), var(--blue-700));
  border-color: transparent; box-shadow: var(--shadow-btn);
}

.kpi-label {
  font-size: 11px; font-weight: 500; color: var(--neutral-500);
  text-transform: uppercase; letter-spacing: 0.06em;
}
.kpi-label-white { color: rgba(255,255,255,0.65); }
.kpi-value {
  font-family: var(--font-display); font-size: 28px; font-weight: 700;
  color: var(--neutral-900); line-height: 1.1;
}
.kpi-value-white { color: #fff; }
.kpi-muted { color: var(--neutral-400); }
.kpi-badge {
  display: inline-flex; align-items: center;
  font-size: 11px; font-weight: 700;
  padding: 3px 8px; border-radius: 6px; align-self: flex-start;
}
.badge-white { background: rgba(255,255,255,0.18); color: #fff; }
.kpi-sub { font-size: 11px; color: var(--neutral-400); }

.empty-state { text-align: center; padding: 48px; color: var(--neutral-500); font-size: 14px; }
</style>
