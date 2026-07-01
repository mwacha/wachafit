<template>
  <AppLayout>
    <div class="p-6 max-w-4xl">
      <h1 class="text-2xl font-bold mb-6">Comissões de Profissionais</h1>

      <div class="card p-4 mb-6" style="display:flex; gap:16px; align-items:flex-end">
        <div>
          <label style="font-size:13px; font-weight:600; margin-bottom:4px; display:block">De</label>
          <InputText v-model="from" type="date" />
        </div>
        <div>
          <label style="font-size:13px; font-weight:600; margin-bottom:4px; display:block">Até</label>
          <InputText v-model="to" type="date" />
        </div>
        <Button label="Buscar" @click="load" :loading="loading" />
      </div>

      <div v-if="loading" class="text-center py-8">Carregando...</div>
      <div v-else-if="data.length === 0" class="text-surface-400 text-center py-8">Nenhum dado no período.</div>
      <DataTable v-else :value="data" stripedRows>
        <Column field="name" header="Profissional" />
        <Column field="commissionType" header="Tipo" />
        <Column field="classesCount" header="Aulas" />
        <Column header="Comissão">
          <template #body="{ data }">R$ {{ data.commissionDue.toFixed(2) }}</template>
        </Column>
      </DataTable>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import reportService from '@/services/report.service'
import type { TrainerCommission } from '@/types/api'

const data = ref<TrainerCommission[]>([])
const loading = ref(false)
const now = new Date()
const from = ref(new Date(now.getFullYear(), now.getMonth(), 1).toISOString().slice(0, 10))
const to = ref(now.toISOString().slice(0, 10))

async function load() {
  loading.value = true
  try { data.value = await reportService.getTrainerCommissions(from.value, to.value) }
  finally { loading.value = false }
}
</script>
