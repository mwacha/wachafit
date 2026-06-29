<!-- frontend/src/views/trainer/ScheduleView.vue -->
<template>
  <AppLayout>
    <div class="p-6">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold">Minha Agenda</h1>
        <Button label="Novo horário" icon="pi pi-plus" @click="showCreate = true" />
      </div>

      <DatePicker v-model="filterDate" placeholder="Selecione uma data" dateFormat="yy-mm-dd"
        showButtonBar @update:modelValue="loadSchedules" class="mb-4" />

      <DataTable :value="scheduleStore.schedules" :loading="scheduleStore.loading" stripedRows>
        <Column field="type" header="Tipo" />
        <Column header="Início">
          <template #body="{ data }">{{ new Date(data.startsAt).toLocaleString('pt-BR') }}</template>
        </Column>
        <Column header="Fim">
          <template #body="{ data }">{{ new Date(data.endsAt).toLocaleString('pt-BR') }}</template>
        </Column>
        <Column field="status" header="Status" />
        <Column header="Ações">
          <template #body="{ data }">
            <Button v-if="data.status !== 'CANCELLED'" icon="pi pi-times" severity="danger" text
              @click="scheduleStore.cancelSchedule(data.id)" />
          </template>
        </Column>
      </DataTable>

      <Dialog v-model:visible="showCreate" header="Novo Horário" :modal="true" style="width: 440px">
        <form @submit.prevent="submitCreate" class="flex flex-col gap-3">
          <Select v-model="form.type" :options="['CLASS','PERSONAL']" placeholder="Tipo" required />
          <DatePicker v-model="form.startsAt" showTime hourFormat="24" dateFormat="yy-mm-dd" placeholder="Início" required />
          <DatePicker v-model="form.endsAt" showTime hourFormat="24" dateFormat="yy-mm-dd" placeholder="Fim" required />
          <p v-if="error" class="text-red-500 text-sm">{{ error }}</p>
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
const error = ref<string | null>(null)
const filterDate = ref<Date | null>(null)
const form = ref({ type: '', startsAt: null as Date | null, endsAt: null as Date | null })

onMounted(() => loadSchedules())

function loadSchedules() {
  scheduleStore.fetchSchedules({
    trainerId: authStore.userId ?? undefined,
    date: filterDate.value ? filterDate.value.toISOString().split('T')[0] : undefined,
  })
}

async function submitCreate() {
  saving.value = true; error.value = null
  try {
    await scheduleStore.createSchedule({
      trainerId: authStore.userId!,
      type: form.value.type,
      startsAt: form.value.startsAt!.toISOString(),
      endsAt: form.value.endsAt!.toISOString(),
    })
    showCreate.value = false
  } catch (e: any) {
    error.value = e.response?.data?.message ?? 'Erro ao criar horário'
  } finally { saving.value = false }
}
</script>
