<template>
  <AppLayout>
    <div class="view-wrap">
      <h1 class="page-title">Minhas Cobranças</h1>

      <div v-if="loading" class="empty-state">Carregando...</div>
      <div v-else-if="charges.length === 0" class="empty-state">Nenhuma cobrança encontrada.</div>
      <div v-else class="table-scroll">
        <DataTable paginator :rows="10" :rowsPerPageOptions="[10, 25, 50]" :value="charges" stripedRows>
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
          <Column header="Pago em" style="min-width:120px">
            <template #body="{ data }">
              <span v-if="data.paidAt">{{ formatDate(data.paidAt) }}</span>
              <span v-else class="text-muted">—</span>
            </template>
          </Column>
          <Column header="Forma" style="min-width:110px">
            <template #body="{ data }">{{ payMethodLabel(data.paymentMethod) }}</template>
          </Column>
          <Column header="Ações" style="min-width:100px">
            <template #body="{ data }">
              <Button
                v-if="data.status !== 'PAID' && data.status !== 'CANCELLED'"
                icon="pi pi-credit-card" text size="small" label="Pagar"
                @click="openPay(data)" />
            </template>
          </Column>
        </DataTable>
      </div>
    </div>

    <!-- Dialog: Registrar Pagamento -->
    <Dialog v-model:visible="showPayDialog" header="Registrar Pagamento" :modal="true" style="width: min(380px, 95vw)">
      <div class="pay-form">
        <div v-if="selectedCharge" class="charge-summary">
          <div class="charge-summary-item">
            <span class="charge-summary-label">Valor</span>
            <span class="charge-summary-value">R$ {{ selectedCharge.amount.toFixed(2) }}</span>
          </div>
          <div class="charge-summary-item">
            <span class="charge-summary-label">Vencimento</span>
            <span class="charge-summary-value">{{ formatDate(selectedCharge.dueDate) }}</span>
          </div>
        </div>
        <div class="form-field">
          <label class="form-label">Forma de pagamento *</label>
          <Select v-model="payMethod" :options="payMethodOptions" optionLabel="label" optionValue="value"
            placeholder="Selecione a forma de pagamento" style="width:100%" />
        </div>
        <div class="form-actions">
          <Button label="Cancelar" outlined @click="showPayDialog = false" />
          <Button label="Confirmar pagamento" :loading="paying" :disabled="!payMethod" @click="confirmPay" />
        </div>
      </div>
    </Dialog>
  </AppLayout>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import Select from 'primevue/select'
import { useAuthStore } from '@/stores/auth.store'
import { useBillingStore } from '@/stores/billing.store'
import billingService from '@/services/billing.service'
import type { PaymentCharge } from '@/types/api'
import { chargeStatusLabel, chargeStatusSeverity, payMethodLabel as payMethodLabelMap, payMethodOptions } from '@/utils/labels'
import { useToast } from 'primevue/usetoast'

const auth = useAuthStore()
const billingStore = useBillingStore()
const toast = useToast()
const charges = ref<PaymentCharge[]>([])
const loading = ref(true)

const showPayDialog = ref(false)
const selectedCharge = ref<PaymentCharge | null>(null)
const payMethod = ref('')
const paying = ref(false)


onMounted(async () => {
  if (auth.userId) charges.value = await billingService.listCharges(auth.userId)
  loading.value = false
})

function openPay(charge: PaymentCharge) {
  selectedCharge.value = charge
  payMethod.value = ''
  showPayDialog.value = true
}

async function confirmPay() {
  if (!selectedCharge.value || !payMethod.value) return
  paying.value = true
  try {
    const updated = await billingService.payCharge(selectedCharge.value.id, { paymentMethod: payMethod.value })
    const idx = charges.value.findIndex(c => c.id === updated.id)
    if (idx !== -1) charges.value[idx] = updated
    showPayDialog.value = false
    await billingStore.fetchPaymentStatus()
  } catch (e: any) {
    toast.add({ severity: 'error', summary: 'Erro', detail: e?.response?.data?.message ?? 'Não foi possível registrar o pagamento', life: 4000 })
  } finally {
    paying.value = false
  }
}

function formatDate(d: string) { return new Date(d).toLocaleDateString('pt-BR', { timeZone: 'UTC' }) }
function statusLabel(s: string) { return chargeStatusLabel[s] ?? s }
function statusSeverity(s: string) { return chargeStatusSeverity[s] ?? 'secondary' }
function payMethodLabel(m: string | null) { return m ? (payMethodLabelMap[m] ?? m) : '—' }
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 20px; max-width: 800px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }
.table-scroll { overflow-x: auto; border-radius: var(--radius-lg); }
.empty-state { text-align: center; padding: 40px; color: var(--neutral-500); font-size: 14px; }
.text-muted { color: var(--neutral-400); font-size: 13px; }

/* Dialog de pagamento */
.pay-form { display: flex; flex-direction: column; gap: 20px; padding: 8px 0 4px; }
.charge-summary {
  display: flex; gap: 16px;
  background: var(--neutral-50); border-radius: var(--radius-md);
  padding: 14px 16px;
}
.charge-summary-item { display: flex; flex-direction: column; gap: 3px; flex: 1; }
.charge-summary-label { font-size: 11px; font-weight: 600; color: var(--neutral-500); text-transform: uppercase; letter-spacing: .04em; }
.charge-summary-value { font-size: 18px; font-weight: 700; color: var(--neutral-900); font-family: var(--font-display); }
.form-field { display: flex; flex-direction: column; gap: 6px; }
.form-label { font-size: 13px; font-weight: 600; color: var(--neutral-700); }
.form-actions { display: flex; justify-content: flex-end; gap: 8px; padding-top: 4px; }
</style>
