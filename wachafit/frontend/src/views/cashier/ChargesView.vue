<template>
  <AppLayout>
    <div class="p-6 max-w-4xl">
      <h1 class="text-2xl font-bold mb-6">Cobranças</h1>

      <div class="card p-4 mb-4" style="display:flex; gap:16px; align-items:flex-end">
        <div style="flex:1">
          <label style="font-size:13px; font-weight:600; margin-bottom:4px; display:block">Email do Aluno</label>
          <InputText v-model="searchEmail" placeholder="buscar por email..." class="w-full" @keydown.enter="search" />
        </div>
        <Button label="Buscar" @click="search" :loading="loading" />
      </div>

      <div v-if="loading" class="text-center py-8">Carregando...</div>
      <div v-else-if="searched && charges.length === 0" class="text-surface-400 text-center py-8">
        Nenhuma cobrança encontrada.
      </div>

      <DataTable v-else-if="charges.length > 0" :value="charges" stripedRows>
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
        <Column header="Ações">
          <template #body="{ data }">
            <Button v-if="data.status !== 'PAID' && data.status !== 'CANCELLED'"
                    icon="pi pi-check" text size="small" label="Pagar" @click="openPay(data)" />
          </template>
        </Column>
      </DataTable>

      <Dialog v-model:visible="showPayDialog" header="Registrar Pagamento" :modal="true" style="width:360px">
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
