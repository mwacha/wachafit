<template>
  <AppLayout>
    <div class="view-wrap">
      <h1 class="page-title">Alunos Inadimplentes</h1>

      <div v-if="loading" class="empty-state">Carregando...</div>
      <div v-else-if="data.length === 0" class="empty-state">Nenhum aluno inadimplente.</div>
      <div v-else class="table-scroll">
        <DataTable :value="data" stripedRows>
          <Column field="name" header="Aluno" style="min-width:140px" />
          <Column header="Total Devido" style="min-width:130px">
            <template #body="{ data }">R$ {{ data.totalDue.toFixed(2) }}</template>
          </Column>
          <Column field="daysOverdue" header="Dias em Atraso" style="min-width:130px" />
        </DataTable>
      </div>
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

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 20px; max-width: 900px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }
.table-scroll { overflow-x: auto; border-radius: var(--radius-lg); }
.empty-state { text-align: center; padding: 40px; color: var(--neutral-500); font-size: 14px; }
</style>
