<!-- frontend/src/views/admin/SchedulesView.vue -->
<template>
  <AppLayout>
    <div class="view-wrap">
      <div class="page-header">
        <h1 class="page-title">Agenda</h1>
        <Button label="Novo horário" icon="pi pi-plus" @click="openCreate" />
      </div>

      <!-- Navegação de semana -->
      <div class="week-nav-card">
        <div class="week-nav-header">
          <button class="nav-btn" @click="prevWeek"><i class="pi pi-chevron-left" /></button>
          <span class="month-label">{{ monthLabel }}</span>
          <button class="nav-btn" @click="nextWeek"><i class="pi pi-chevron-right" /></button>
          <button class="today-btn" @click="goToday">Hoje</button>
        </div>
        <div class="week-days">
          <button v-for="day in weekDays" :key="day.iso"
            :class="['day-chip', { selected: day.iso === selectedIso, today: day.isToday }]"
            @click="selectDay(day)">
            <span class="chip-name">{{ day.name }}</span>
            <span class="chip-num">{{ day.num }}</span>
          </button>
        </div>
      </div>

      <!-- Cabeçalho do dia -->
      <div class="day-info-row">
        <span class="day-full-label">{{ selectedDayLabel }}</span>
        <span class="session-count" v-if="!scheduleStore.loading">
          {{ scheduleStore.schedules.length }} sessão(ões)
        </span>
      </div>

      <!-- Lista de sessões do dia -->
      <div class="table-scroll">
        <DataTable :value="scheduleStore.schedules" :loading="scheduleStore.loading"
          stripedRows paginator :rows="10" :rowsPerPageOptions="[10, 25, 50]">
          <template #empty>Nenhuma sessão personal agendada para este dia.</template>
          <Column header="Início" style="min-width:120px">
            <template #body="{ data }">{{ formatTime(data.startsAt) }}</template>
          </Column>
          <Column header="Fim" style="min-width:120px">
            <template #body="{ data }">{{ formatTime(data.endsAt) }}</template>
          </Column>
          <Column header="Status" style="min-width:100px">
            <template #body="{ data }">{{ scheduleStatusLabel[data.status] ?? data.status }}</template>
          </Column>
          <Column header="Ações" style="min-width:80px">
            <template #body="{ data }">
              <Button v-if="data.status !== 'CANCELLED'" icon="pi pi-times" severity="danger"
                text size="small" @click="cancelSchedule(data.id)" />
            </template>
          </Column>
        </DataTable>
      </div>

      <!-- Dialog: novo horário -->
      <Dialog v-model:visible="showCreate" header="Novo Horário Personal" :modal="true"
        style="width: min(440px, 95vw)">
        <form @submit.prevent="submitCreate" class="schedule-form">
          <div class="form-field">
            <label class="form-label">Início</label>
            <DatePicker v-model="form.startsAt" showTime hourFormat="24" dateFormat="yy-mm-dd"
              placeholder="Data e hora de início" class="w-full" required />
          </div>
          <div class="form-field">
            <label class="form-label">Fim</label>
            <DatePicker v-model="form.endsAt" showTime hourFormat="24" dateFormat="yy-mm-dd"
              placeholder="Data e hora de fim" class="w-full" required />
          </div>
          <p v-if="formError" class="form-error">{{ formError }}</p>
          <div class="form-actions">
            <Button type="button" label="Cancelar" outlined @click="showCreate = false" />
            <Button type="submit" label="Criar horário" :loading="saving" />
          </div>
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
import { scheduleStatusLabel } from '@/utils/labels'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Dialog from 'primevue/dialog'
import DatePicker from 'primevue/datepicker'

const scheduleStore = useScheduleStore()
const authStore = useAuthStore()

const today = new Date()
today.setHours(0, 0, 0, 0)

const weekStart = ref(mondayOf(new Date()))
const selectedDate = ref(new Date(today))

const showCreate = ref(false)
const saving = ref(false)
const formError = ref<string | null>(null)
const form = ref({ startsAt: null as Date | null, endsAt: null as Date | null })

function mondayOf(d: Date): Date {
  const r = new Date(d); r.setHours(0, 0, 0, 0)
  const dow = r.getDay()
  r.setDate(r.getDate() + (dow === 0 ? -6 : 1 - dow))
  return r
}
function addDays(d: Date, n: number): Date {
  const r = new Date(d); r.setDate(r.getDate() + n); return r
}
function toIso(d: Date) { return d.toISOString().split('T')[0] }

const DAY_NAMES = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom']

const weekDays = computed(() =>
  Array.from({ length: 7 }, (_, i) => {
    const d = addDays(weekStart.value, i)
    return { date: d, iso: toIso(d), name: DAY_NAMES[i], num: d.getDate(), isToday: d.getTime() === today.getTime() }
  })
)

const selectedIso = computed(() => toIso(selectedDate.value))

const monthLabel = computed(() => {
  const months = weekDays.value.map(d => d.date.getMonth())
  const unique = [...new Set(months)]
  const fmt = (m: number) => new Date(weekStart.value.getFullYear(), m).toLocaleDateString('pt-BR', { month: 'long' })
  const year = weekStart.value.getFullYear()
  return unique.length === 1
    ? `${fmt(unique[0])} ${year}`
    : `${fmt(unique[0])} / ${fmt(unique[1])} ${year}`
})

const selectedDayLabel = computed(() =>
  selectedDate.value.toLocaleDateString('pt-BR', { weekday: 'long', day: '2-digit', month: 'long', year: 'numeric' })
)

function prevWeek() { weekStart.value = addDays(weekStart.value, -7) }
function nextWeek() { weekStart.value = addDays(weekStart.value, 7) }
function goToday() {
  weekStart.value = mondayOf(new Date())
  selectedDate.value = new Date(today)
  loadSchedules()
}

function selectDay(day: { date: Date; iso: string }) {
  selectedDate.value = new Date(day.date)
  loadSchedules()
}

function loadSchedules() {
  scheduleStore.fetchSchedules({ date: toIso(selectedDate.value), type: 'PERSONAL' })
}

function formatTime(iso: string) {
  return new Date(iso).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
}

async function cancelSchedule(id: string) {
  await scheduleStore.cancelSchedule(id)
}

function openCreate() {
  form.value = { startsAt: null, endsAt: null }
  formError.value = null
  showCreate.value = true
}

async function submitCreate() {
  saving.value = true; formError.value = null
  try {
    await scheduleStore.createSchedule({
      trainerId: authStore.userId!,
      type: 'PERSONAL',
      startsAt: form.value.startsAt!.toISOString(),
      endsAt: form.value.endsAt!.toISOString(),
    })
    showCreate.value = false
    loadSchedules()
  } catch (e: any) {
    formError.value = e.response?.data?.message ?? 'Erro ao criar horário'
  } finally { saving.value = false }
}

onMounted(() => loadSchedules())
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 20px; }
.page-header { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }

/* Semana */
.week-nav-card {
  background: #fff;
  border: 1px solid var(--neutral-200);
  border-radius: var(--radius-lg);
  padding: 16px;
  display: flex; flex-direction: column; gap: 12px;
  box-shadow: var(--shadow-card);
}
.week-nav-header {
  display: flex; align-items: center; gap: 8px;
}
.nav-btn {
  width: 32px; height: 32px;
  background: none; border: 1px solid var(--neutral-200);
  border-radius: var(--radius-md); cursor: pointer;
  color: var(--neutral-600); font-size: 13px;
  display: flex; align-items: center; justify-content: center;
  transition: background 0.15s;
}
.nav-btn:hover { background: var(--neutral-100); }
.month-label {
  flex: 1; text-align: center;
  font-size: 14px; font-weight: 700; color: var(--neutral-800);
  text-transform: capitalize;
}
.today-btn {
  padding: 4px 12px; font-size: 12px; font-weight: 600;
  background: none; border: 1px solid var(--neutral-300);
  border-radius: var(--radius-md); cursor: pointer; color: var(--neutral-600);
  transition: background 0.15s;
}
.today-btn:hover { background: var(--neutral-100); }

.week-days {
  display: grid; grid-template-columns: repeat(7, 1fr); gap: 6px;
}
.day-chip {
  display: flex; flex-direction: column; align-items: center;
  padding: 8px 4px; border-radius: var(--radius-md);
  border: 1.5px solid transparent; cursor: pointer;
  background: var(--neutral-50); transition: all 0.15s;
}
.day-chip:hover { background: var(--neutral-100); }
.day-chip.today { border-color: var(--blue-400); }
.day-chip.selected { background: var(--blue-500); border-color: var(--blue-500); }
.day-chip.selected .chip-name,
.day-chip.selected .chip-num { color: #fff; }

.chip-name { font-size: 10px; font-weight: 600; text-transform: uppercase; letter-spacing: .06em; color: var(--neutral-500); }
.chip-num  { font-size: 18px; font-weight: 700; color: var(--neutral-800); line-height: 1.3; }

/* Cabeçalho do dia */
.day-info-row {
  display: flex; align-items: center; justify-content: space-between; padding: 0 2px;
}
.day-full-label { font-size: 14px; font-weight: 600; color: var(--neutral-700); text-transform: capitalize; }
.session-count  { font-size: 13px; color: var(--neutral-400); }

.table-scroll { overflow-x: auto; border-radius: var(--radius-lg); }

/* Form */
.schedule-form { display: flex; flex-direction: column; gap: 20px; padding: 8px 0 4px; }
.form-field { display: flex; flex-direction: column; gap: 6px; }
.form-label { font-size: 13px; font-weight: 600; color: var(--neutral-700); }
.form-error { font-size: 13px; color: #ef4444; margin: -8px 0; }
.form-actions { display: flex; justify-content: flex-end; gap: 8px; padding-top: 4px; }
</style>
