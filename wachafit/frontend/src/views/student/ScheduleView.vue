<!-- frontend/src/views/student/ScheduleView.vue -->
<template>
  <AppLayout>
    <div class="p-6">
      <h1 class="text-2xl font-bold mb-6">Aulas Disponíveis</h1>

      <div class="flex gap-3 mb-4">
        <DatePicker v-model="filterDate" placeholder="Data" dateFormat="yy-mm-dd" showButtonBar
          @update:modelValue="loadSchedules" />
        <Select v-model="filterType" :options="['CLASS','PERSONAL']" placeholder="Tipo" showClear
          @update:modelValue="loadSchedules" />
      </div>

      <div v-if="scheduleStore.loading" class="text-center py-8">Carregando...</div>
      <div v-else class="grid gap-3">
        <div v-for="s in openSchedules" :key="s.id"
             class="card flex items-center justify-between p-4">
          <div>
            <div class="font-semibold">{{ s.type === 'CLASS' ? 'Aula em grupo' : 'Sessão individual' }}</div>
            <div class="text-sm text-surface-500">
              {{ new Date(s.startsAt).toLocaleString('pt-BR') }} — {{ new Date(s.endsAt).toLocaleTimeString('pt-BR') }}
            </div>
          </div>
          <Button label="Reservar" size="small" @click="book(s.id)" :loading="booking === s.id" />
        </div>
        <div v-if="openSchedules.length === 0" class="text-surface-400 text-sm">Nenhum horário disponível.</div>
      </div>
      <p v-if="bookError" class="text-red-500 text-sm mt-3">{{ bookError }}</p>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useScheduleStore } from '@/stores/schedule.store'
import { useBookingStore } from '@/stores/booking.store'
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
