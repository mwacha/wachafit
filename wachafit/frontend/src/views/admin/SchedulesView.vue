<!-- frontend/src/views/admin/SchedulesView.vue -->
<template>
  <AppLayout>
    <div class="view-wrap">
      <div class="page-header">
        <h1 class="page-title">Agenda</h1>
        <Button label="Novo horário" icon="pi pi-plus" @click="showCreate = true" />
      </div>

      <div class="filter-row">
        <DatePicker v-model="filterDate" placeholder="Filtrar por data" dateFormat="yy-mm-dd"
          @update:modelValue="loadSchedules" showButtonBar />
        <Select v-model="filterType" :options="scheduleTypeOptions" optionLabel="label" optionValue="value"
          placeholder="Tipo" showClear @update:modelValue="loadSchedules" />
      </div>

      <div class="table-scroll">
        <DataTable :value="scheduleStore.schedules" :loading="scheduleStore.loading" stripedRows>
          <template #empty>Nenhuma aula agendada.</template>
          <Column header="Tipo" style="min-width:100px">
            <template #body="{ data }">{{ scheduleTypeLabel[data.type] ?? data.type }}</template>
          </Column>
          <Column header="Início" style="min-width:160px">
            <template #body="{ data }">{{ formatDate(data.startsAt) }}</template>
          </Column>
          <Column header="Fim" style="min-width:160px">
            <template #body="{ data }">{{ formatDate(data.endsAt) }}</template>
          </Column>
          <Column header="Status" style="min-width:100px">
            <template #body="{ data }">{{ scheduleStatusLabel[data.status] ?? data.status }}</template>
          </Column>
          <Column header="Ações" style="min-width:80px">
            <template #body="{ data }">
              <Button v-if="data.status !== 'CANCELLED'" icon="pi pi-times" severity="danger" text
                @click="cancelSchedule(data.id)" />
            </template>
          </Column>
        </DataTable>
      </div>

      <Dialog v-model:visible="showCreate" header="Novo Horário" :modal="true" style="width: min(460px, 95vw)">
        <form @submit.prevent="submitCreate" class="schedule-form">
          <div class="form-field">
            <label class="form-label">Tipo de aula</label>
            <Select v-model="form.type" :options="scheduleTypeOptions" optionLabel="label" optionValue="value" placeholder="Selecione o tipo" class="w-full" required />
          </div>
          <div class="form-field">
            <label class="form-label">Início</label>
            <DatePicker v-model="form.startsAt" showTime hourFormat="24" dateFormat="yy-mm-dd" placeholder="Data e hora de início" class="w-full" required />
          </div>
          <div class="form-field">
            <label class="form-label">Fim</label>
            <DatePicker v-model="form.endsAt" showTime hourFormat="24" dateFormat="yy-mm-dd" placeholder="Data e hora de fim" class="w-full" required />
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
import { ref, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useScheduleStore } from '@/stores/schedule.store'
import { useAuthStore } from '@/stores/auth.store'
import { scheduleTypeLabel, scheduleStatusLabel, scheduleTypeOptions } from '@/utils/labels'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Dialog from 'primevue/dialog'
import DatePicker from 'primevue/datepicker'
import Select from 'primevue/select'

const scheduleStore = useScheduleStore()
const authStore = useAuthStore()
const showCreate = ref(false)
const saving = ref(false)
const formError = ref<string | null>(null)
const filterDate = ref<Date | null>(null)
const filterType = ref<string | null>(null)
const form = ref({ type: '', startsAt: null as Date | null, endsAt: null as Date | null })

onMounted(() => loadSchedules())

function loadSchedules() {
  scheduleStore.fetchSchedules({
    date: filterDate.value ? filterDate.value.toISOString().split('T')[0] : undefined,
    type: filterType.value ?? undefined,
  })
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleString('pt-BR')
}

async function cancelSchedule(id: string) {
  await scheduleStore.cancelSchedule(id)
}

async function submitCreate() {
  saving.value = true; formError.value = null
  try {
    await scheduleStore.createSchedule({
      trainerId: authStore.userId!,
      type: form.value.type,
      startsAt: form.value.startsAt!.toISOString(),
      endsAt: form.value.endsAt!.toISOString(),
    })
    showCreate.value = false
  } catch (e: any) {
    formError.value = e.response?.data?.message ?? 'Erro ao criar horário'
  } finally { saving.value = false }
}
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 20px; }
.page-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }
.filter-row { display: flex; gap: 10px; flex-wrap: wrap; }
.filter-row :deep(.p-datepicker-input),
.filter-row :deep(.p-select) { min-width: 140px; flex: 1; }
.table-scroll { overflow-x: auto; border-radius: var(--radius-lg); }

.schedule-form { display: flex; flex-direction: column; gap: 20px; padding: 8px 0 4px; }
.form-field { display: flex; flex-direction: column; gap: 6px; }
.form-label { font-size: 13px; font-weight: 600; color: var(--neutral-700); }
.form-error { font-size: 13px; color: #ef4444; margin: -8px 0; }
.form-actions { display: flex; justify-content: flex-end; gap: 8px; padding-top: 4px; }
</style>
