<!-- frontend/src/views/admin/SchedulesView.vue -->
<template>
  <AppLayout>
    <div class="view-wrap">
      <div class="page-header">
        <h1 class="page-title">Agenda</h1>
        <Button label="Novo horário" icon="pi pi-plus" @click="openCreate" />
      </div>

      <!-- Calendário de navegação -->
      <div class="cal-wrap">
        <DatePicker v-model="selectedDate" inline showButtonBar
          @update:modelValue="loadSchedules" />
      </div>

      <!-- Lista do dia selecionado -->
      <div class="day-header-row">
        <span class="day-label">{{ dayLabel }}</span>
        <span class="day-count">{{ scheduleStore.schedules.length }} sessão(ões)</span>
      </div>

      <div class="table-scroll">
        <DataTable :value="scheduleStore.schedules" :loading="scheduleStore.loading"
          stripedRows paginator :rows="10" :rowsPerPageOptions="[10, 25, 50]">
          <template #empty>Nenhuma sessão personal agendada para este dia.</template>
          <Column header="Início" style="min-width:160px">
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

      <!-- Dialog: novo horário PERSONAL -->
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

const selectedDate = ref<Date>(new Date())
const showCreate = ref(false)
const saving = ref(false)
const formError = ref<string | null>(null)
const form = ref({ startsAt: null as Date | null, endsAt: null as Date | null })

const dayLabel = computed(() =>
  selectedDate.value.toLocaleDateString('pt-BR', { weekday: 'long', day: '2-digit', month: 'long', year: 'numeric' })
)

onMounted(() => loadSchedules())

function loadSchedules() {
  const date = selectedDate.value instanceof Date
    ? selectedDate.value.toISOString().split('T')[0]
    : undefined
  scheduleStore.fetchSchedules({ date, type: 'PERSONAL' })
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
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 20px; }
.page-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }

.cal-wrap {
  display: flex;
  justify-content: flex-start;
}
.cal-wrap :deep(.p-datepicker) {
  border: 1px solid var(--neutral-200);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.day-header-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 4px 2px;
}
.day-label {
  font-size: 15px; font-weight: 600; color: var(--neutral-800);
  text-transform: capitalize;
}
.day-count { font-size: 13px; color: var(--neutral-500); }

.table-scroll { overflow-x: auto; border-radius: var(--radius-lg); }

.schedule-form { display: flex; flex-direction: column; gap: 20px; padding: 8px 0 4px; }
.form-field { display: flex; flex-direction: column; gap: 6px; }
.form-label { font-size: 13px; font-weight: 600; color: var(--neutral-700); }
.form-error { font-size: 13px; color: #ef4444; margin: -8px 0; }
.form-actions { display: flex; justify-content: flex-end; gap: 8px; padding-top: 4px; }
</style>
