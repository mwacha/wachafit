<!-- frontend/src/views/student/CalendarView.vue -->
<template>
  <AppLayout>
    <div class="view-wrap">
      <h1 class="page-title">Meu Calendário</h1>

      <!-- Navegação semanal -->
      <div class="week-nav">
        <Button icon="pi pi-chevron-left" text @click="prevWeek" />
        <span class="week-label">{{ weekLabel }}</span>
        <Button icon="pi pi-chevron-right" text @click="nextWeek" />
        <Button label="Hoje" text size="small" @click="goToday" />
      </div>

      <!-- Grade semanal -->
      <div v-if="bookingStore.loading" class="empty-state">Carregando...</div>
      <div v-else class="week-grid">
        <div v-for="day in weekDays" :key="day.iso" class="day-col">
          <div :class="['day-header', { today: day.isToday }]">
            <span class="day-name">{{ day.name }}</span>
            <span class="day-num">{{ day.num }}</span>
          </div>
          <div class="slots">
            <div
              v-for="b in slotsForDay(day.iso)"
              :key="b.id"
              :class="['slot-card', `status-${b.status.toLowerCase()}`]"
            >
              <div class="slot-time">{{ formatTime(b.startsAt) }} – {{ formatTime(b.endsAt) }}</div>
              <div class="slot-type">{{ b.type === 'CLASS' ? (b.groupClassName || 'Aula coletiva') : 'Sessão individual' }}</div>
              <div v-if="b.trainerName" class="slot-trainer">{{ b.trainerName }}</div>
            </div>
            <div v-if="slotsForDay(day.iso).length === 0" class="day-empty">—</div>
          </div>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useBookingStore } from '@/stores/booking.store'
import Button from 'primevue/button'

const bookingStore = useBookingStore()

const weekStart = ref(mondayOf(new Date()))

function mondayOf(d: Date): Date {
  const day = new Date(d); day.setHours(0, 0, 0, 0)
  const dow = day.getDay()
  const diff = dow === 0 ? -6 : 1 - dow
  day.setDate(day.getDate() + diff)
  return day
}

function addDays(d: Date, n: number): Date {
  const r = new Date(d); r.setDate(r.getDate() + n); return r
}

const weekDays = computed(() => {
  const today = new Date(); today.setHours(0, 0, 0, 0)
  const names = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom']
  return Array.from({ length: 7 }, (_, i) => {
    const d = addDays(weekStart.value, i)
    return {
      iso: d.toISOString().split('T')[0],
      name: names[i],
      num: d.getDate(),
      isToday: d.getTime() === today.getTime(),
    }
  })
})

const weekLabel = computed(() => {
  const from = weekDays.value[0]
  const to   = weekDays.value[6]
  return `${from.num}/${String(weekDays.value[0].iso.split('-')[1]).padStart(2, '0')} — ${to.num}/${String(weekDays.value[6].iso.split('-')[1]).padStart(2, '0')}`
})

function prevWeek() { weekStart.value = addDays(weekStart.value, -7) }
function nextWeek() { weekStart.value = addDays(weekStart.value,  7) }
function goToday()  { weekStart.value = mondayOf(new Date()) }

function slotsForDay(iso: string) {
  return bookingStore.bookings
    .filter(b => b.startsAt.startsWith(iso))
    .sort((a, b) => a.startsAt.localeCompare(b.startsAt))
}

function formatTime(iso: string) {
  return new Date(iso).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
}

onMounted(() => bookingStore.fetchMyBookings())
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 16px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }

.week-nav { display: flex; align-items: center; gap: 4px; }
.week-label { font-size: 14px; font-weight: 600; color: var(--neutral-700); min-width: 130px; text-align: center; }

.week-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
  overflow-x: auto;
}

.day-col { display: flex; flex-direction: column; gap: 6px; min-width: 110px; }

.day-header {
  text-align: center; padding: 6px 4px; border-radius: var(--radius-md);
  background: var(--neutral-100);
}
.day-header.today { background: var(--blue-500); color: #fff; }
.day-name { display: block; font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: .06em; }
.day-num  { display: block; font-size: 18px; font-weight: 700; line-height: 1.2; }

.slots { display: flex; flex-direction: column; gap: 6px; }

.slot-card {
  background: #fff; border: 1.5px solid var(--neutral-200);
  border-radius: var(--radius-md); padding: 8px 10px;
  box-shadow: var(--shadow-card); font-size: 12px;
}
.status-confirmed { border-left: 3px solid #10b981; }
.status-pending   { border-left: 3px solid #f59e0b; }
.status-cancelled { border-left: 3px solid #9ca3af; opacity: 0.5; }

.slot-time    { font-weight: 700; color: var(--neutral-800); font-size: 11px; }
.slot-type    { font-size: 12px; color: var(--neutral-600); margin-top: 2px; }
.slot-trainer { font-size: 11px; color: var(--neutral-400); margin-top: 2px; }

.day-empty { text-align: center; color: var(--neutral-300); font-size: 18px; padding: 12px 0; }

.empty-state {
  text-align: center; padding: 48px; color: var(--neutral-500); font-size: 14px;
  background: #fff; border: 1px solid var(--neutral-200);
  border-radius: var(--radius-lg);
}
</style>
