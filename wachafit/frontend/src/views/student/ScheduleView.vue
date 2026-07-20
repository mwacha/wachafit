<!-- frontend/src/views/student/ScheduleView.vue -->
<template>
  <AppLayout>
    <div class="view-wrap">
      <h1 class="page-title">Aulas Disponíveis</h1>

      <div class="filter-row">
        <DatePicker v-model="filterDate" placeholder="Data" dateFormat="yy-mm-dd" showButtonBar
          @update:modelValue="loadSchedules" />
        <Select v-model="filterType" :options="scheduleTypeOptions" optionLabel="label" optionValue="value"
          placeholder="Tipo" showClear @update:modelValue="loadSchedules" />
      </div>

      <div v-if="scheduleStore.loading" class="empty-state">Carregando...</div>
      <div v-else class="slot-list">
        <div v-for="s in openSchedules" :key="s.id" class="slot-card">
          <div class="slot-info">
            <div class="slot-type">{{ s.type === 'CLASS' ? 'Aula em grupo' : 'Sessão individual' }}</div>
            <div class="slot-time">
              {{ new Date(s.startsAt).toLocaleString('pt-BR') }} — {{ new Date(s.endsAt).toLocaleTimeString('pt-BR') }}
            </div>
          </div>
          <Button label="Reservar" size="small" @click="book(s.id)" :loading="booking === s.id" />
        </div>
        <div v-if="openSchedules.length === 0" class="empty-state">Nenhum horário disponível.</div>
      </div>
      <p v-if="bookError" class="error-msg">{{ bookError }}</p>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useScheduleStore } from '@/stores/schedule.store'
import { useBookingStore } from '@/stores/booking.store'
import { scheduleTypeOptions } from '@/utils/labels'
import Button from 'primevue/button'
import DatePicker from 'primevue/datepicker'
import Select from 'primevue/select'

const scheduleStore = useScheduleStore()
const bookingStore = useBookingStore()
const filterDate = ref<Date | null>(null)
const filterType = ref<string | null>(null)
const booking = ref<string | null>(null)
const bookError = ref<string | null>(null)

onMounted(() => loadSchedules())

function loadSchedules() {
  scheduleStore.fetchSchedules({
    date: filterDate.value ? filterDate.value.toISOString().split('T')[0] : undefined,
    type: filterType.value ?? undefined,
  })
}

const openSchedules = computed(() => scheduleStore.schedules.filter(s => s.status === 'OPEN'))

async function book(scheduleId: string) {
  booking.value = scheduleId; bookError.value = null
  try { await bookingStore.createBooking(scheduleId) }
  catch (e: any) { bookError.value = e.response?.data?.message ?? 'Erro ao reservar' }
  finally { booking.value = null }
}
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 20px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }
.filter-row { display: flex; gap: 10px; flex-wrap: wrap; }
.filter-row :deep(.p-datepicker-input),
.filter-row :deep(.p-select) { min-width: 140px; flex: 1; }
.slot-list { display: flex; flex-direction: column; gap: 10px; }
.slot-card {
  background: #fff; border: 1px solid var(--neutral-200);
  border-radius: var(--radius-lg); padding: 14px 16px;
  display: flex; align-items: center; justify-content: space-between;
  gap: 12px; flex-wrap: wrap;
  box-shadow: var(--shadow-card);
}
.slot-info { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.slot-type { font-weight: 600; font-size: 14px; color: var(--neutral-900); }
.slot-time { font-size: 13px; color: var(--neutral-500); }
.empty-state {
  text-align: center; padding: 32px 16px;
  color: var(--neutral-500); font-size: 14px;
  background: #fff; border: 1px solid var(--neutral-200); border-radius: var(--radius-lg);
}
.error-msg { font-size: 13px; color: var(--error-text); margin-top: 4px; }
</style>
