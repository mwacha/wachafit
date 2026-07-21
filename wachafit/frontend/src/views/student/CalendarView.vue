<!-- frontend/src/views/student/CalendarView.vue -->
<template>
  <AppLayout>
    <div class="view-wrap">
      <h1 class="page-title">Meu Calendário</h1>

      <div class="week-nav">
        <Button icon="pi pi-chevron-left" text @click="prevWeek" />
        <span class="week-label">{{ weekLabel }}</span>
        <Button icon="pi pi-chevron-right" text @click="nextWeek" />
        <Button label="Hoje" text size="small" @click="goToday" />
      </div>

      <div v-if="loading" class="empty-state">Carregando...</div>
      <div v-else class="week-grid">
        <div v-for="day in weekDays" :key="day.iso" class="day-col">
          <div :class="['day-header', { today: day.isToday }]">
            <span class="day-name">{{ day.name }}</span>
            <span class="day-num">{{ day.num }}</span>
          </div>
          <div class="slots">
            <!-- Aulas de turma (matrícula) -->
            <div v-for="slot in classSlots(day)" :key="slot.classId"
                 class="slot-card slot-class">
              <div class="slot-time">{{ slot.startTime }} – {{ slot.endTime }}</div>
              <div class="slot-name">{{ slot.className }}</div>
              <div class="slot-trainer">{{ slot.trainerName }}</div>
            </div>
            <!-- Sessões individuais (reservas PERSONAL) -->
            <div v-for="b in personalSlots(day.iso)" :key="b.id"
                 :class="['slot-card', `status-${b.status.toLowerCase()}`]">
              <div class="slot-time">{{ formatTime(b.startsAt) }} – {{ formatTime(b.endsAt) }}</div>
              <div class="slot-name">{{ b.trainerName ? `Personal — ${b.trainerName}` : 'Sessão individual' }}</div>
            </div>
            <div v-if="classSlots(day).length === 0 && personalSlots(day.iso).length === 0" class="day-empty">—</div>
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
import { groupClassService } from '@/services/groupclass.service'
import type { EnrolledClass } from '@/types/api'
import Button from 'primevue/button'

const bookingStore = useBookingStore()
const enrollments = ref<EnrolledClass[]>([])
const loading = ref(true)

onMounted(async () => {
  await Promise.all([
    bookingStore.fetchMyBookings(),
    groupClassService.myEnrollments().then(data => { enrollments.value = data }),
  ])
  loading.value = false
})

// Mapeamento: getDay() → chave de dia
const DAY_KEYS = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT']

const weekStart = ref(mondayOf(new Date()))

function mondayOf(d: Date): Date {
  const day = new Date(d); day.setHours(0, 0, 0, 0)
  const dow = day.getDay()
  day.setDate(day.getDate() + (dow === 0 ? -6 : 1 - dow))
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
    return { date: d, iso: d.toISOString().split('T')[0], name: names[i], num: d.getDate(), isToday: d.getTime() === today.getTime() }
  })
})

const weekLabel = computed(() => {
  const [from, to] = [weekDays.value[0], weekDays.value[6]]
  return `${from.num}/${from.iso.slice(5, 7)} — ${to.num}/${to.iso.slice(5, 7)}`
})

function prevWeek() { weekStart.value = addDays(weekStart.value, -7) }
function nextWeek() { weekStart.value = addDays(weekStart.value,  7) }
function goToday()  { weekStart.value = mondayOf(new Date()) }

function classSlots(day: { date: Date; iso: string }) {
  const key = DAY_KEYS[day.date.getDay()]
  return enrollments.value
    .filter(e => e.daysOfWeek.includes(key))
    .sort((a, b) => (a.startTime ?? '').localeCompare(b.startTime ?? ''))
}

function personalSlots(iso: string) {
  return bookingStore.bookings
    .filter(b => b.startsAt.startsWith(iso))
    .sort((a, b) => a.startsAt.localeCompare(b.startsAt))
}

function formatTime(iso: string) {
  return new Date(iso).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
}

</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 16px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }

.week-nav { display: flex; align-items: center; gap: 4px; }
.week-label { font-size: 14px; font-weight: 600; color: var(--neutral-700); min-width: 130px; text-align: center; }

.week-grid { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); gap: 8px; overflow-x: auto; }
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
.slot-class  { border-left: 3px solid var(--blue-500); }
.status-confirmed { border-left: 3px solid #10b981; }
.status-pending   { border-left: 3px solid #f59e0b; }
.status-cancelled { border-left: 3px solid #9ca3af; opacity: 0.5; }

.slot-time    { font-weight: 700; color: var(--neutral-800); font-size: 11px; }
.slot-name    { font-size: 12px; color: var(--neutral-700); margin-top: 2px; font-weight: 600; }
.slot-trainer { font-size: 11px; color: var(--neutral-400); margin-top: 2px; }

.day-empty { text-align: center; color: var(--neutral-300); font-size: 18px; padding: 12px 0; }
.empty-state {
  text-align: center; padding: 48px; color: var(--neutral-500); font-size: 14px;
  background: #fff; border: 1px solid var(--neutral-200); border-radius: var(--radius-lg);
}
</style>
