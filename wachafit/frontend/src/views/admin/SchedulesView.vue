<!-- frontend/src/views/admin/SchedulesView.vue -->
<template>
  <AppLayout>
    <div class="p-6">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold">Agenda</h1>
        <Button label="Novo horário" icon="pi pi-plus" @click="showCreate = true" />
      </div>

      <div class="flex gap-3 mb-4">
        <DatePicker v-model="filterDate" placeholder="Filtrar por data" dateFormat="yy-mm-dd"
          @update:modelValue="loadSchedules" showButtonBar />
        <Select v-model="filterType" :options="['CLASS','PERSONAL']" placeholder="Tipo" showClear
          @update:modelValue="loadSchedules" />
      </div>

      <DataTable :value="scheduleStore.schedules" :loading="scheduleStore.loading" stripedRows>
        <Column field="type" header="Tipo" />
        <Column header="Início">
          <template #body="{ data }">{{ formatDate(data.startsAt) }}</template>
        </Column>
        <Column header="Fim">
          <template #body="{ data }">{{ formatDate(data.endsAt) }}</template>
        </Column>
        <Column field="status" header="Status" />
        <Column header="Ações">
          <template #body="{ data }">
            <Button v-if="data.status !== 'CANCELLED'" icon="pi pi-times" severity="danger" text
              @click="cancelSchedule(data.id)" />
          </template>
        </Column>
      </DataTable>

      <Dialog v-model:visible="showCreate" header="Novo Horário" :modal="true" style="width: 460px">
        <form @submit.prevent="submitCreate" class="flex flex-col gap-3">
          <Select v-model="form.type" :options="['CLASS','PERSONAL']" placeholder="Tipo" required />
          <label class="text-sm font-medium">Início</label>
          <DatePicker v-model="form.startsAt" showTime hourFormat="24" dateFormat="yy-mm-dd" required />
          <label class="text-sm font-medium">Fim</label>
          <DatePicker v-model="form.endsAt" showTime hourFormat="24" dateFormat="yy-mm-dd" required />
          <p v-if="formError" class="text-red-500 text-sm">{{ formError }}</p>
          <Button type="submit" label="Criar" :loading="saving" />
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
