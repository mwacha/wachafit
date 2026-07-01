<template>
  <AppLayout>
    <div class="p-6 max-w-3xl">
      <h1 class="text-2xl font-bold mb-6">Minhas Cobranças</h1>

      <div v-if="loading" class="text-center py-8">Carregando...</div>
      <div v-else-if="charges.length === 0" class="text-surface-400 text-center py-8">
        Nenhuma cobrança encontrada.
      </div>

      <DataTable v-else :value="charges" stripedRows>
        <Column field="dueDate" header="Vencimento">
          <template #body="{ data }">{{ formatDate(data.dueDate) }}</template>
        </Column>
        <Column header="Valor">
          <template #body="{ data }">R$ {{ data.amount.toFixed(2) }}</template>
        </Column>
        <Column header="Status">
          <template #body="{ data }">
            <Tag :value="statusLabel(data.status)" :severity="statusSeverity(data.status)" />
          </template>
        </Column>
        <Column field="paymentMethod" header="Pagamento" />
      </DataTable>
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
