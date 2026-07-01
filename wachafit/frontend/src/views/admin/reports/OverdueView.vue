<template>
  <AppLayout>
    <div class="p-6 max-w-4xl">
      <h1 class="text-2xl font-bold mb-6">Alunos Inadimplentes</h1>

      <div v-if="loading" class="text-center py-8">Carregando...</div>
      <div v-else-if="data.length === 0" class="text-surface-400 text-center py-8">
        Nenhum aluno inadimplente.
      </div>
      <DataTable v-else :value="data" stripedRows>
        <Column field="name" header="Aluno" />
        <Column header="Total Devido">
          <template #body="{ data }">R$ {{ data.totalDue.toFixed(2) }}</template>
        </Column>
        <Column field="daysOverdue" header="Dias em Atraso" />
      </DataTable>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import reportService from '@/services/report.service'
import type { OverdueStudent } from '@/types/api'

const data = ref<OverdueStudent[]>([])
const loading = ref(true)

onMounted(async () => {
  data.value = await reportService.getOverdue()
  loading.value = false
})
</script>
