<template>
  <AppLayout>
    <div class="view-wrap">
      <h1 class="page-title">Receita Mensal</h1>

      <div class="filter-row">
        <div class="filter-field">
          <label class="field-label">De (aaaa-mm)</label>
          <InputText v-model="from" placeholder="2026-01" />
        </div>
        <div class="filter-field">
          <label class="field-label">Até (aaaa-mm)</label>
          <InputText v-model="to" placeholder="2026-06" />
        </div>
        <Button label="Buscar" @click="load" :loading="loading" />
      </div>

      <div v-if="loading" class="empty-state">Carregando...</div>
      <div v-else-if="data.length === 0" class="empty-state">Nenhum dado no período.</div>
      <div v-else class="table-scroll">
        <DataTable :value="data" stripedRows>
          <Column field="month" header="Mês" style="min-width:100px" />
          <Column header="Total Recebido" style="min-width:140px">
            <template #body="{ data }">R$ {{ data.total.toFixed(2) }}</template>
          </Column>
          <Column field="chargesCount" header="Cobranças" style="min-width:110px" />
        </DataTable>
      </div>
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

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 20px; max-width: 900px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }
.filter-row { display: flex; gap: 12px; flex-wrap: wrap; align-items: flex-end; }
.filter-field { display: flex; flex-direction: column; gap: 4px; min-width: 130px; }
.field-label { font-size: 13px; font-weight: 600; color: var(--neutral-800); }
.table-scroll { overflow-x: auto; border-radius: var(--radius-lg); }
.empty-state { text-align: center; padding: 40px; color: var(--neutral-500); font-size: 14px; }
</style>
