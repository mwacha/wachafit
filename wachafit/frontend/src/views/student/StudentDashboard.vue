<template>
  <AppLayout>
    <div class="dash">
      <div v-if="loading" class="empty-state">Carregando...</div>
      <div v-else class="kpi-grid">
        <div class="kpi-card">
          <span class="kpi-label">Minhas reservas</span>
          <span class="kpi-value">{{ totalBookings }}</span>
        </div>
        <div class="kpi-card">
          <span class="kpi-label">Confirmadas</span>
          <span class="kpi-value">{{ confirmedBookings }}</span>
        </div>
        <div class="kpi-card">
          <span class="kpi-label">Pendentes</span>
          <span class="kpi-value">{{ pendingBookings }}</span>
        </div>
        <div class="kpi-card kpi-highlight" v-if="nextBooking">
          <span class="kpi-label kpi-label-white">Próxima aula</span>
          <span class="kpi-value kpi-value-white">{{ nextBookingTime }}</span>
          <span class="kpi-badge badge-white">{{ nextBookingType }}</span>
        </div>
        <div class="kpi-card kpi-neutral" v-else>
          <span class="kpi-label">Próxima aula</span>
          <span class="kpi-value kpi-muted">—</span>
          <span class="kpi-sub">Sem aulas agendadas</span>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useBookingStore } from '@/stores/booking.store'

const bookingStore = useBookingStore()
const loading = ref(true)

onMounted(async () => {
  await bookingStore.fetchMyBookings()
  loading.value = false
})

const totalBookings = computed(() => bookingStore.bookings.length)
const confirmedBookings = computed(() => bookingStore.bookings.filter(b => b.status === 'CONFIRMED').length)
const pendingBookings = computed(() => bookingStore.bookings.filter(b => b.status === 'PENDING').length)

const nextBooking = computed(() => {
  const now = new Date().toISOString()
  return bookingStore.bookings
    .filter(b => b.status !== 'CANCELLED' && b.startsAt > now)
    .sort((a, b) => a.startsAt.localeCompare(b.startsAt))[0] ?? null
})

const nextBookingTime = computed(() => {
  if (!nextBooking.value) return '—'
  return new Date(nextBooking.value.startsAt).toLocaleString('pt-BR', {
    weekday: 'short', day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit',
  })
})

const nextBookingType = computed(() => {
  if (!nextBooking.value) return ''
  return nextBooking.value.type === 'CLASS'
    ? (nextBooking.value.groupClassName || 'Aula coletiva')
    : 'Personal'
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
.kpi-neutral { background: var(--neutral-50); }

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
  display: inline-flex; font-size: 11px; font-weight: 700;
  padding: 3px 8px; border-radius: 6px; align-self: flex-start;
}
.badge-white { background: rgba(255,255,255,0.18); color: #fff; }
.kpi-sub { font-size: 11px; color: var(--neutral-400); }

.empty-state { text-align: center; padding: 48px; color: var(--neutral-500); font-size: 14px; }
</style>
