<!-- frontend/src/views/student/EvolutionView.vue -->
<template>
  <AppLayout>
    <div class="p-6 max-w-4xl">
      <h1 class="text-2xl font-bold mb-6">Evolução</h1>

      <div v-if="assessmentStore.loading" class="text-center py-8">Carregando...</div>
      <div v-else-if="assessmentStore.evolution.length === 0" class="text-surface-400">
        Nenhuma avaliação registrada.
      </div>
      <div v-else class="grid gap-6">
        <div class="card p-4">
          <h2 class="text-lg font-semibold mb-3">Histórico de Avaliações</h2>
          <DataTable :value="assessmentStore.evolution" stripedRows>
            <Column field="assessedAt" header="Data" />
            <Column header="Peso (kg)">
              <template #body="{ data }">{{ data.weightKg ?? '—' }}</template>
            </Column>
            <Column header="% Gordura">
              <template #body="{ data }">{{ data.bodyFatPct ?? '—' }}</template>
            </Column>
            <Column header="IMC">
              <template #body="{ data }">{{ data.bmi ?? '—' }}</template>
            </Column>
          </DataTable>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useAssessmentStore } from '@/stores/assessment.store'
import { useAuthStore } from '@/stores/auth.store'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'

const assessmentStore = useAssessmentStore()
const authStore = useAuthStore()

onMounted(() => assessmentStore.fetchAssessments(authStore.userId!))
</script>
