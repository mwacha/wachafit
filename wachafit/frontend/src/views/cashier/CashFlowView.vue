<template>
  <AppLayout>
    <div class="view-wrap">
      <h1 class="page-title">Fluxo de Caixa</h1>

      <div class="filter-row">
        <div class="filter-field">
          <label class="field-label">De</label>
          <InputText v-model="from" type="date" />
        </div>
        <div class="filter-field">
          <label class="field-label">Até</label>
          <InputText v-model="to" type="date" />
        </div>
        <Button label="Buscar" @click="load" :loading="loading" />
      </div>

      <div v-if="summary" class="summary-grid">
        <div class="summary-card">
          <p class="summary-label">Total Recebido</p>
          <p class="summary-value received">R$ {{ summary.received.toFixed(2) }}</p>
        </div>
        <div class="summary-card">
          <p class="summary-label">Pendente</p>
          <p class="summary-value pending">R$ {{ summary.pending.toFixed(2) }}</p>
        </div>
        <div class="summary-card">
          <p class="summary-label">Vencido</p>
          <p class="summary-value overdue">R$ {{ summary.overdue.toFixed(2) }}</p>
        </div>
      </div>

      <div v-if="loading" class="empty-state">Carregando...</div>
      <div v-else-if="data.length === 0" class="empty-state">Nenhum lançamento no período.</div>
      <div v-else class="table-scroll">
        <DataTable paginator :rows="10" :rowsPerPageOptions="[10, 25, 50]" :value="data" stripedRows>
          <Column field="date" header="Data" style="min-width:110px">
            <template #body="{ data }">{{ formatDate(data.date) }}</template>
          </Column>
          <Column header="Recebido" style="min-width:110px">
            <template #body="{ data }">R$ {{ data.received.toFixed(2) }}</template>
          </Column>
          <Column header="Pendente" style="min-width:100px">
            <template #body="{ data }">R$ {{ data.pending.toFixed(2) }}</template>
          </Column>
          <Column header="Vencido" style="min-width:100px">
            <template #body="{ data }">R$ {{ data.overdue.toFixed(2) }}</template>
          </Column>
        </DataTable>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import reportService from '@/services/report.service'
import type { CashFlowDay } from '@/types/api'

const data = ref<CashFlowDay[]>([])
const loading = ref(false)
const now = new Date()
const from = ref(new Date(now.getFullYear(), now.getMonth(), 1).toISOString().slice(0, 10))
const to = ref(now.toISOString().slice(0, 10))

const summary = computed(() => data.value.length === 0 ? null : ({
  received: data.value.reduce((s, d) => s + d.received, 0),
  pending: data.value.reduce((s, d) => s + d.pending, 0),
  overdue: data.value.reduce((s, d) => s + d.overdue, 0),
}))

async function load() {
  loading.value = true
  try { data.value = await reportService.getCashFlow(from.value, to.value) }
  finally { loading.value = false }
}

function formatDate(d: string) { return new Date(d).toLocaleDateString('pt-BR') }
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 20px; max-width: 900px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }
.filter-row { display: flex; gap: 12px; flex-wrap: wrap; align-items: flex-end; }
.filter-field { display: flex; flex-direction: column; gap: 4px; min-width: 130px; }
.field-label { font-size: 13px; font-weight: 600; color: var(--neutral-800); }
.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
  gap: 12px;
}
.summary-card {
  background: #fff; border: 1px solid var(--neutral-200);
  border-radius: var(--radius-lg); padding: 16px;
  text-align: center; box-shadow: var(--shadow-card);
}
.summary-label { font-size: 12px; color: var(--neutral-500); margin-bottom: 4px; }
.summary-value { font-size: 20px; font-weight: 700; }
.received { color: #16a34a; }
.pending  { color: #d97706; }
.overdue  { color: #dc2626; }
.table-scroll { overflow-x: auto; border-radius: var(--radius-lg); }
.empty-state { text-align: center; padding: 40px; color: var(--neutral-500); font-size: 14px; }
</style>
