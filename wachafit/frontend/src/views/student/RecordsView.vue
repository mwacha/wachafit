<!-- frontend/src/views/student/RecordsView.vue -->
<template>
  <AppLayout>
    <div class="view-wrap">
      <h1 class="page-title">Recordes Pessoais</h1>
      <DataTable :value="workoutStore.records" :loading="workoutStore.loading" stripedRows
        emptyMessage="Nenhum recorde registrado.">
        <Column header="Exercício" style="min-width:160px">
          <template #body="{ data }">{{ exerciseNames[data.exerciseId] ?? '—' }}</template>
        </Column>
        <Column header="Carga" style="min-width:100px">
          <template #body="{ data }">{{ data.recordLoadKg }} kg</template>
        </Column>
        <Column header="Conquistado em" style="min-width:130px">
          <template #body="{ data }">{{ data.achievedAt }}</template>
        </Column>
      </DataTable>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useWorkoutStore } from '@/stores/workout.store'
import { useAuthStore } from '@/stores/auth.store'
import { exerciseService } from '@/services/exercise.service'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'

const workoutStore = useWorkoutStore()
const authStore = useAuthStore()
const exerciseNames = ref<Record<string, string>>({})

onMounted(async () => {
  await workoutStore.fetchRecords(authStore.userId!)
  const exercises = await exerciseService.search()
  exerciseNames.value = Object.fromEntries(exercises.map(e => [e.id, e.name]))
})
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 16px; max-width: 700px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }
</script>
