<template>
  <AppLayout>
    <div class="view-wrap">
      <h1 class="page-title">Minhas Cobranças</h1>

      <div v-if="loading" class="empty-state">Carregando...</div>
      <div v-else-if="charges.length === 0" class="empty-state">Nenhuma cobrança encontrada.</div>
      <div v-else class="table-scroll">
        <DataTable :value="charges" stripedRows>
          <Column field="dueDate" header="Vencimento" style="min-width:120px">
            <template #body="{ data }">{{ formatDate(data.dueDate) }}</template>
          </Column>
          <Column header="Valor" style="min-width:90px">
            <template #body="{ data }">R$ {{ data.amount.toFixed(2) }}</template>
          </Column>
          <Column header="Status" style="min-width:100px">
            <template #body="{ data }">
              <Tag :value="statusLabel(data.status)" :severity="statusSeverity(data.status)" />
            </template>
          </Column>
          <Column field="paymentMethod" header="Pagamento" style="min-width:110px" />
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
import Tag from 'primevue/tag'
import { useAuthStore } from '@/stores/auth.store'
import billingService from '@/services/billing.service'
import type { PaymentCharge } from '@/types/api'

const auth = useAuthStore()
const charges = ref<PaymentCharge[]>([])
const loading = ref(true)

onMounted(async () => {
  if (auth.userId) charges.value = await billingService.listCharges(auth.userId)
  loading.value = false
})

function formatDate(d: string) { return new Date(d).toLocaleDateString('pt-BR') }
function statusLabel(s: string) {
  return { PENDING: 'Pendente', PAID: 'Pago', OVERDUE: 'Vencido', CANCELLED: 'Cancelado' }[s] ?? s
}
function statusSeverity(s: string) {
  return { PENDING: 'warn', PAID: 'success', OVERDUE: 'danger', CANCELLED: 'secondary' }[s] ?? 'secondary'
}
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 20px; max-width: 800px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }
.table-scroll { overflow-x: auto; border-radius: var(--radius-lg); }
.empty-state { text-align: center; padding: 40px; color: var(--neutral-500); font-size: 14px; }
</style>
