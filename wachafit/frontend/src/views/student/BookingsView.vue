<!-- frontend/src/views/student/BookingsView.vue -->
<template>
  <AppLayout>
    <div class="view-wrap">
      <h1 class="page-title">Minhas Reservas</h1>
      <div class="table-scroll">
        <DataTable :value="bookingStore.bookings" :loading="bookingStore.loading" stripedRows>
          <Column field="scheduleId" header="Horário" style="min-width:180px" />
          <Column header="Status" style="min-width:100px">
            <template #body="{ data }">{{ bookingStatusLabel[data.status] ?? data.status }}</template>
          </Column>
          <Column header="Reservado em" style="min-width:160px">
            <template #body="{ data }">{{ new Date(data.bookedAt).toLocaleString('pt-BR') }}</template>
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
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'

const bookingStore = useBookingStore()
onMounted(() => bookingStore.fetchMyBookings())

async function cancel(id: string) { await bookingStore.cancelBooking(id) }
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 20px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }
.table-scroll { overflow-x: auto; border-radius: var(--radius-lg); }
.empty-state {
  display: flex; flex-direction: column; align-items: center; gap: 10px;
  padding: 48px 24px; background: #fff;
  border: 1px solid var(--neutral-200); border-radius: var(--radius-lg);
  text-align: center; color: var(--neutral-500);
}
.empty-icon { font-size: 32px; color: var(--neutral-300); }
</style>
