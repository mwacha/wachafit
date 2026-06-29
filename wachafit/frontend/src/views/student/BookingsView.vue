<!-- frontend/src/views/student/BookingsView.vue -->
<template>
  <AppLayout>
    <div class="p-6">
      <h1 class="text-2xl font-bold mb-6">Minhas Reservas</h1>
      <DataTable :value="bookingStore.bookings" :loading="bookingStore.loading" stripedRows>
        <Column field="scheduleId" header="Horário ID" />
        <Column field="status" header="Status" />
        <Column field="bookedAt" header="Reservado em">
          <template #body="{ data }">{{ new Date(data.bookedAt).toLocaleString('pt-BR') }}</template>
        </Column>
        <Column header="Ações">
          <template #body="{ data }">
            <Button v-if="data.status !== 'CANCELLED'" icon="pi pi-times" severity="danger" text
              label="Cancelar" size="small" @click="cancel(data.id)" />
          </template>
        </Column>
      </DataTable>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useBookingStore } from '@/stores/booking.store'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'

const bookingStore = useBookingStore()
onMounted(() => bookingStore.fetchMyBookings())

async function cancel(id: string) { await bookingStore.cancelBooking(id) }
</script>
