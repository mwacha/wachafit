<!-- frontend/src/views/student/BookingsView.vue -->
<template>
  <AppLayout>
    <div class="view-wrap">
      <h1 class="page-title">Minhas Reservas</h1>
      <div class="table-scroll">
        <DataTable :value="bookingStore.bookings" :loading="bookingStore.loading" stripedRows>
          <template #empty>Nenhuma reserva encontrada.</template>
          <Column header="Aula / Sessão" style="min-width:160px">
            <template #body="{ data }">
              <span class="class-name">{{ data.groupClassName ?? 'Sessão individual' }}</span>
            </template>
          </Column>
          <Column header="Data e horário" style="min-width:180px">
            <template #body="{ data }">
              {{ formatDateTime(data.startsAt) }}
              <span class="time-sep">–</span>
              {{ formatTime(data.endsAt) }}
            </template>
          </Column>
          <Column header="Status" style="min-width:110px">
            <template #body="{ data }">
              <Tag :severity="statusSeverity(data.status)" :value="bookingStatusLabel[data.status] ?? data.status" />
            </template>
          </Column>
          <Column header="Reservado em" style="min-width:160px">
            <template #body="{ data }">{{ formatDateTime(data.bookedAt) }}</template>
          </Column>
          <Column header="Ações" style="min-width:100px">
            <template #body="{ data }">
              <Button v-if="data.status !== 'CANCELLED'" icon="pi pi-times" severity="danger" text
                label="Cancelar" size="small" @click="cancel(data.id)" />
            </template>
          </Column>
        </DataTable>
      </div>
      <div v-if="!bookingStore.loading && bookingStore.bookings.length === 0" class="empty-state">
        <i class="pi pi-bookmark empty-icon" />
        <p>Nenhuma reserva ainda.</p>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useBookingStore } from '@/stores/booking.store'
import { bookingStatusLabel } from '@/utils/labels'
import { useToast } from 'primevue/usetoast'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'

const bookingStore = useBookingStore()
const toast = useToast()

onMounted(() => bookingStore.fetchMyBookings())

async function cancel(id: string) {
  try {
    await bookingStore.cancelBooking(id)
    toast.add({ severity: 'info', summary: 'Reserva cancelada', detail: 'Sua reserva foi cancelada.', life: 3000 })
  } catch (e: any) {
    toast.add({ severity: 'error', summary: 'Erro', detail: e?.response?.data?.message ?? 'Não foi possível cancelar a reserva', life: 4000 })
  }
}

function formatDateTime(iso: string) {
  return new Date(iso).toLocaleString('pt-BR', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

function formatTime(iso: string) {
  return new Date(iso).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
}

function statusSeverity(status: string) {
  if (status === 'CANCELLED') return 'secondary'
  return 'success'
}
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 20px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }
.table-scroll { overflow-x: auto; border-radius: var(--radius-lg); }
.class-name { font-weight: 600; color: var(--neutral-800); }
.time-sep { color: var(--neutral-400); margin: 0 2px; }
.empty-state {
  display: flex; flex-direction: column; align-items: center; gap: 10px;
  padding: 48px 24px; background: #fff;
  border: 1px solid var(--neutral-200); border-radius: var(--radius-lg);
  text-align: center; color: var(--neutral-500);
}
.empty-icon { font-size: 32px; color: var(--neutral-300); }
</style>
