<!-- frontend/src/views/trainer/ScheduleView.vue -->
<template>
  <AppLayout>
    <div class="view-wrap">
      <!-- Header -->
      <div class="page-header">
        <h1 class="page-title">Minha Agenda</h1>
        <Button label="Novo horário" icon="pi pi-plus" @click="showCreate = true" />
      </div>

      <!-- Navegação semanal -->
      <div class="week-nav">
        <Button icon="pi pi-chevron-left" text @click="prevWeek" />
        <span class="week-label">{{ weekLabel }}</span>
        <Button icon="pi pi-chevron-right" text @click="nextWeek" />
        <Button label="Hoje" text size="small" @click="goToday" />
      </div>

      <!-- Grade semanal -->
      <div v-if="scheduleStore.loading" class="empty-state">Carregando...</div>
      <div v-else class="week-grid">
        <div v-for="day in weekDays" :key="day.iso" class="day-col">
          <!-- Cabeçalho do dia -->
          <div :class="['day-header', { today: day.isToday }]">
            <span class="day-name">{{ day.name }}</span>
            <span class="day-num">{{ day.num }}</span>
          </div>

          <!-- Slots do dia -->
          <div class="slots">
            <div v-for="s in slotsForDay(day.iso)" :key="s.id"
              :class="['slot-card', `slot-${s.type.toLowerCase()}`, { cancelled: s.status === 'CANCELLED' }]">
              <div class="slot-time">{{ formatTime(s.startsAt) }} – {{ formatTime(s.endsAt) }}</div>
              <div class="slot-type-label">{{ s.type === 'CLASS' ? (s.groupClassName || 'Aula coletiva') : 'Personal' }}</div>

              <!-- Alunos confirmados -->
              <div v-if="s.bookedStudents?.length" class="student-list">
                <span v-for="b in s.bookedStudents" :key="b.studentId"
                  :class="['student-chip', b.status === 'PENDING' ? 'chip-pending' : 'chip-confirmed']">
                  {{ b.studentName }}
                </span>
              </div>
              <div v-else-if="s.status !== 'CANCELLED'" class="no-students">Sem alunos</div>

              <!-- Cancelar -->
              <button v-if="s.status !== 'CANCELLED'" class="cancel-btn"
                @click.stop="cancelSchedule(s.id)" title="Cancelar horário">
                <i class="pi pi-times" />
              </button>
            </div>

            <div v-if="slotsForDay(day.iso).length === 0" class="day-empty">—</div>
          </div>
        </div>
      </div>

      <!-- Dialog novo horário -->
      <Dialog v-model:visible="showCreate" header="Novo Horário" :modal="true"
        :style="{ width: 'min(440px, 95vw)' }">
        <form @submit.prevent="submitCreate" class="flex flex-col gap-3 pt-2">
          <Select v-model="form.type" :options="scheduleTypeOptions" optionLabel="label" optionValue="value" placeholder="Tipo" required />
          <DatePicker v-model="form.startsAt" showTime hourFormat="24" dateFormat="yy-mm-dd"
            placeholder="Início" required />
          <DatePicker v-model="form.endsAt" showTime hourFormat="24" dateFormat="yy-mm-dd"
            placeholder="Fim" required />
          <p v-if="createError" class="text-red-500 text-sm">{{ createError }}</p>
          <Button type="submit" label="Criar" :loading="saving" />
        </form>
      </Dialog>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useScheduleStore } from '@/stores/schedule.store'
import { useAuthStore } from '@/stores/auth.store'
import { scheduleTypeOptions } from '@/utils/labels'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import DatePicker from 'primevue/datepicker'
import Select from 'primevue/select'

const scheduleStore = useScheduleStore()
const authStore = useAuthStore()
const showCreate = ref(false)
const saving = ref(false)
const createError = ref<string | null>(null)
const form = ref({ type: '', startsAt: null as Date | null, endsAt: null as Date | null })

// semana atual — segunda-feira da semana
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
  return `${from.num}/${String(weekDays.value[0].iso.split('-')[1]).padStart(2,'0')} — ${to.num}/${String(weekDays.value[6].iso.split('-')[1]).padStart(2,'0')}`
})

function prevWeek() { weekStart.value = addDays(weekStart.value, -7); loadWeek() }
function nextWeek() { weekStart.value = addDays(weekStart.value,  7); loadWeek() }
function goToday()  { weekStart.value = mondayOf(new Date()); loadWeek() }

function loadWeek() {
  const fromDt = new Date(weekStart.value); fromDt.setHours(0, 0, 0, 0)
  const toDt   = addDays(weekStart.value, 6); toDt.setHours(23, 59, 59, 999)
  scheduleStore.fetchSchedules({
    trainerId: authStore.userId ?? undefined,
    from: fromDt.toISOString(),
    to:   toDt.toISOString(),
  })
}

function slotsForDay(iso: string) {
  return scheduleStore.schedules.filter(s => s.startsAt.startsWith(iso))
    .sort((a, b) => a.startsAt.localeCompare(b.startsAt))
}

function formatTime(iso: string) {
  return new Date(iso).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
}

async function cancelSchedule(id: string) {
  await scheduleStore.cancelSchedule(id)
}

async function submitCreate() {
  saving.value = true; createError.value = null
  try {
    await scheduleStore.createSchedule({
      trainerId: authStore.userId!,
      type: form.value.type,
      startsAt: form.value.startsAt!.toISOString(),
      endsAt: form.value.endsAt!.toISOString(),
    })
    showCreate.value = false
    loadWeek()
  } catch (e: any) {
    createError.value = e.response?.data?.message ?? 'Erro ao criar horário'
  } finally { saving.value = false }
}

onMounted(() => loadWeek())
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }

.week-nav {
  display: flex; align-items: center; gap: 4px;
}
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
  position: relative;
  background: #fff; border: 1.5px solid var(--neutral-200);
  border-radius: var(--radius-md); padding: 8px 10px;
  box-shadow: var(--shadow-card); font-size: 12px;
}
.slot-class    { border-left: 3px solid #3b82f6; }
.slot-personal { border-left: 3px solid #10b981; }
.cancelled     { opacity: .5; }

.slot-time { font-weight: 700; color: var(--neutral-800); font-size: 11px; }
.slot-type-label { font-size: 12px; color: var(--neutral-600); margin-bottom: 6px; }

.student-list { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 4px; }
.student-chip {
  font-size: 10px; font-weight: 600; padding: 2px 7px;
  border-radius: 10px;
}
.chip-confirmed { background: #dcfce7; color: #166534; }
.chip-pending   { background: #fef9c3; color: #854d0e; }
.no-students    { font-size: 10px; color: var(--neutral-400); margin-top: 4px; }

.cancel-btn {
  position: absolute; top: 5px; right: 5px;
  background: none; border: none; cursor: pointer;
  color: var(--neutral-400); font-size: 10px; padding: 2px;
  border-radius: 4px;
}
.cancel-btn:hover { color: #dc2626; background: #fee2e2; }

.day-empty { text-align: center; color: var(--neutral-300); font-size: 18px; padding: 12px 0; }

.empty-state {
  text-align: center; padding: 48px; color: var(--neutral-500); font-size: 14px;
  background: #fff; border: 1px solid var(--neutral-200);
  border-radius: var(--radius-lg);
}
</style>
