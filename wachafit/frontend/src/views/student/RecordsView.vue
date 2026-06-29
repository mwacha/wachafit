<!-- frontend/src/views/student/RecordsView.vue -->
<template>
  <AppLayout>
    <div class="p-6">
      <h1 class="text-2xl font-bold mb-6">Recordes Pessoais</h1>
      <DataTable :value="workoutStore.records" :loading="workoutStore.loading" stripedRows>
        <Column header="Exercício">
          <template #body="{ data }">{{ data.exerciseId.slice(0, 8) }}...</template>
        </Column>
        <Column header="Carga">
          <template #body="{ data }">{{ data.recordLoadKg }} kg</template>
        </Column>
        <Column header="Conquistado em">
          <template #body="{ data }">{{ data.achievedAt }}</template>
        </Column>
      </DataTable>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useWorkoutStore } from '@/stores/workout.store'
import { useAuthStore } from '@/stores/auth.store'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'

const workoutStore = useWorkoutStore()
const authStore = useAuthStore()
onMounted(() => workoutStore.fetchRecords(authStore.userId!))
</script>
