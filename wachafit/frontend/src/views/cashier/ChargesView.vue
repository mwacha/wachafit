<template>
  <AppLayout>
    <div class="view-wrap">
      <h1 class="page-title">Cobranças</h1>

      <div class="search-bar">
        <div class="search-field">
          <label class="field-label">Email do Aluno</label>
          <InputText v-model="searchEmail" placeholder="buscar por email..." @keydown.enter="search" />
        </div>
        <Button label="Buscar" @click="search" :loading="loading" />
      </div>

      <div v-if="loading" class="empty-state">Carregando...</div>
      <div v-else-if="searched && charges.length === 0" class="empty-state">
        Nenhuma cobrança encontrada.
      </div>

      <div v-else-if="charges.length > 0" class="table-scroll">
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
          <Column header="Ações" style="min-width:90px">
            <template #body="{ data }">
              <Button v-if="data.status !== 'PAID' && data.status !== 'CANCELLED'"
                      icon="pi pi-check" text size="small" label="Pagar" @click="openPay(data)" />
            </template>
          </Column>
        </DataTable>
      </div>

      <Dialog v-model:visible="showPayDialog" header="Registrar Pagamento" :modal="true" style="width: min(360px, 95vw)">
        <div class="flex flex-col gap-3 pt-2">
          <Select v-model="payMethod" :options="payMethods" placeholder="Forma de pagamento" />
          <div class="flex gap-2 justify-end">
            <Button label="Cancelar" class="p-button-text" @click="showPayDialog = false" />
            <Button label="Confirmar" :loading="paying" @click="confirmPay" />
          </div>
        </div>
      </Dialog>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import billingService from '@/services/billing.service'
import { userService } from '@/services/user.service'
import type { PaymentCharge } from '@/types/api'

const charges = ref<PaymentCharge[]>([])
const loading = ref(false)
const searched = ref(false)
const searchEmail = ref('')
const showPayDialog = ref(false)
const selectedCharge = ref<PaymentCharge | null>(null)
const payMethod = ref('')
const paying = ref(false)
const payMethods = ['CASH', 'PIX', 'CREDIT_CARD', 'DEBIT_CARD', 'TRANSFER']

async function search() {
  if (!searchEmail.value) return
  loading.value = true; searched.value = true
  try {
    const users = await userService.list({ role: 'STUDENT' })
    const student = users.find((u) => u.email.toLowerCase().includes(searchEmail.value.toLowerCase()))
    charges.value = student ? await billingService.listCharges(student.id) : []
  } finally { loading.value = false }
}

function openPay(charge: PaymentCharge) {
  selectedCharge.value = charge; payMethod.value = ''; showPayDialog.value = true
}

async function confirmPay() {
  if (!selectedCharge.value || !payMethod.value) return
  paying.value = true
  try {
    const updated = await billingService.payCharge(selectedCharge.value.id, { paymentMethod: payMethod.value })
    const idx = charges.value.findIndex(c => c.id === updated.id)
    if (idx !== -1) charges.value[idx] = updated
    showPayDialog.value = false
  } finally { paying.value = false }
}

function formatDate(d: string) { return new Date(d).toLocaleDateString('pt-BR') }
function statusLabel(s: string) {
  return { PENDING: 'Pendente', PAID: 'Pago', OVERDUE: 'Vencido', CANCELLED: 'Cancelado' }[s] ?? s
}
function statusSeverity(s: string) {
  return { PENDING: 'warn', PAID: 'success', OVERDUE: 'danger', CANCELLED: 'secondary' }[s] ?? 'secondary'
}
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 20px; max-width: 900px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }
.search-bar { display: flex; align-items: flex-end; gap: 12px; flex-wrap: wrap; }
.search-field { display: flex; flex-direction: column; gap: 4px; flex: 1; min-width: 200px; }
.field-label { font-size: 13px; font-weight: 600; color: var(--neutral-800); }
.search-field :deep(.p-inputtext) { width: 100%; }
.table-scroll { overflow-x: auto; border-radius: var(--radius-lg); }
.empty-state {
  text-align: center; padding: 40px 24px;
  color: var(--neutral-500); font-size: 14px;
  background: #fff; border: 1px solid var(--neutral-200);
  border-radius: var(--radius-lg);
}
</style>
