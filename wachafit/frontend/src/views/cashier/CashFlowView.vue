<template>
  <AppLayout>
    <div class="p-6 max-w-4xl">
      <h1 class="text-2xl font-bold mb-6">Fluxo de Caixa</h1>

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

      <div v-if="summary" style="display:grid; grid-template-columns:1fr 1fr 1fr; gap:16px; margin-bottom:24px">
        <div class="card p-4 text-center">
          <p style="font-size:12px; color:#6b7280; margin-bottom:4px">Total Recebido</p>
          <p style="font-size:20px; font-weight:700; color:#16a34a">R$ {{ summary.received.toFixed(2) }}</p>
        </div>
        <div class="card p-4 text-center">
          <p style="font-size:12px; color:#6b7280; margin-bottom:4px">Pendente</p>
          <p style="font-size:20px; font-weight:700; color:#d97706">R$ {{ summary.pending.toFixed(2) }}</p>
        </div>
        <div class="card p-4 text-center">
          <p style="font-size:12px; color:#6b7280; margin-bottom:4px">Vencido</p>
          <p style="font-size:20px; font-weight:700; color:#dc2626">R$ {{ summary.overdue.toFixed(2) }}</p>
        </div>
      </div>

      <div v-if="loading" class="text-center py-8">Carregando...</div>
      <DataTable v-else-if="data.length > 0" :value="data" stripedRows>
        <Column field="date" header="Data">
          <template #body="{ data }">{{ formatDate(data.date) }}</template>
        </Column>
        <Column header="Recebido">
          <template #body="{ data }">R$ {{ data.received.toFixed(2) }}</template>
        </Column>
        <Column header="Pendente">
          <template #body="{ data }">R$ {{ data.pending.toFixed(2) }}</template>
        </Column>
        <Column header="Vencido">
          <template #body="{ data }">R$ {{ data.overdue.toFixed(2) }}</template>
        </Column>
      </DataTable>
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
