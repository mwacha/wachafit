<template>
  <AppLayout>
    <div class="p-6 max-w-4xl">
      <h1 class="text-2xl font-bold mb-6">Receita Mensal</h1>

      <div class="card p-4 mb-6" style="display:flex; gap:16px; align-items:flex-end">
        <div>
          <label style="font-size:13px; font-weight:600; margin-bottom:4px; display:block">De (aaaa-mm)</label>
          <InputText v-model="from" placeholder="2026-01" />
        </div>
        <div>
          <label style="font-size:13px; font-weight:600; margin-bottom:4px; display:block">Até (aaaa-mm)</label>
          <InputText v-model="to" placeholder="2026-06" />
        </div>
        <Button label="Buscar" @click="load" :loading="loading" />
      </div>

      <div v-if="loading" class="text-center py-8">Carregando...</div>
      <div v-else-if="data.length === 0" class="text-surface-400 text-center py-8">Nenhum dado no período.</div>
      <DataTable v-else :value="data" stripedRows>
        <Column field="month" header="Mês" />
        <Column header="Total Recebido">
          <template #body="{ data }">R$ {{ data.total.toFixed(2) }}</template>
        </Column>
        <Column field="chargesCount" header="Cobranças" />
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
import type { RevenueReport } from '@/types/api'

const data = ref<RevenueReport[]>([])
const loading = ref(false)
const now = new Date()
const from = ref(`${now.getFullYear()}-01`)
const to = ref(`${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`)

async function load() {
  loading.value = true
  try { data.value = await reportService.getRevenue(from.value, to.value) }
  finally { loading.value = false }
}
</script>
