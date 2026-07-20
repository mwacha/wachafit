<!-- frontend/src/views/receptionist/ChargesView.vue -->
<template>
  <AppLayout>
    <div class="view-wrap">
      <h1 class="page-title">Cobranças</h1>

      <!-- Filtro -->
      <div class="search-bar">
        <div class="search-field">
          <label class="field-label">Buscar por nome ou e-mail</label>
          <InputText v-model="search" placeholder="Digite para filtrar..." />
        </div>
      </div>

      <!-- Lista de alunos -->
      <DataTable
        :value="filteredStudents"
        :loading="loadingStudents"
        :rows="10"
        :paginator="true"
        paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink"
        stripedRows
        selectionMode="single"
        v-model:selection="selectedStudent"
        @row-select="onStudentSelect"
        :rowClass="rowClass"
      >
        <template #empty>Nenhum aluno encontrado.</template>
        <Column field="name" header="Nome" style="min-width:160px" />
        <Column field="email" header="E-mail" style="min-width:200px" />
        <Column header="Status" style="min-width:90px">
          <template #body="{ data }">
            <Tag :severity="data.active ? 'success' : 'secondary'"
                 :value="data.active ? 'Ativo' : 'Inativo'" />
          </template>
        </Column>
      </DataTable>

      <!-- Cobranças do aluno selecionado -->
      <template v-if="selectedStudent">
        <div class="student-header">
          <div class="student-info">
            <i class="pi pi-user-circle student-icon" />
            <div>
              <p class="student-name">{{ selectedStudent.name }}</p>
              <p class="student-email">{{ selectedStudent.email }}</p>
            </div>
          </div>
          <Button label="Nova cobrança" icon="pi pi-plus" size="small" @click="openNewCharge" />
        </div>

        <div v-if="loadingCharges" class="empty-state">Carregando cobranças...</div>
        <div v-else-if="charges.length === 0" class="empty-state">Nenhuma cobrança cadastrada.</div>
        <div v-else class="table-scroll">
          <DataTable :value="charges" stripedRows>
            <Column field="dueDate" header="Vencimento" style="min-width:120px">
              <template #body="{ data }">{{ formatDate(data.dueDate) }}</template>
            </Column>
            <Column header="Valor" style="min-width:100px">
              <template #body="{ data }">R$ {{ data.amount.toFixed(2) }}</template>
            </Column>
            <Column header="Status" style="min-width:110px">
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
            <Column header="Forma" style="min-width:130px">
              <template #body="{ data }">{{ payMethodLabel(data.paymentMethod) }}</template>
            </Column>
            <Column header="Ações" style="min-width:80px">
              <template #body="{ data }">
                <Button v-if="data.status !== 'PAID' && data.status !== 'CANCELLED'"
                        icon="pi pi-times" text size="small" severity="danger"
                        @click="doCancel(data.id)" />
              </template>
            </Column>
          </DataTable>
        </div>
      </template>

      <!-- Dialog: Nova Cobrança -->
      <Dialog v-model:visible="showNewCharge" header="Nova Cobrança" :modal="true" style="width: min(380px, 95vw)">
        <form @submit.prevent="submitCharge" class="flex flex-col gap-3 pt-2">
          <div class="field">
            <label class="field-label">Valor (R$) *</label>
            <InputNumber v-model="chargeForm.amount" mode="currency" currency="BRL" locale="pt-BR"
              :min="0.01" class="w-full" required />
          </div>
          <div class="field">
            <label class="field-label">Data de vencimento *</label>
            <InputText v-model="chargeForm.dueDate" type="date" class="w-full" required />
          </div>
          <p v-if="chargeError" class="error-msg">{{ chargeError }}</p>
          <div class="flex justify-end gap-2 mt-1">
            <Button type="button" label="Cancelar" outlined @click="showNewCharge = false" />
            <Button type="submit" label="Criar cobrança" :loading="savingCharge" />
          </div>
        </form>
      </Dialog>

    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import billingService from '@/services/billing.service'
import { userService } from '@/services/user.service'
import type { PaymentCharge, AdminUser } from '@/types/api'
import { chargeStatusLabel, chargeStatusSeverity, payMethodLabel as payMethodLabelMap } from '@/utils/labels'

const allStudents = ref<AdminUser[]>([])
const loadingStudents = ref(true)
const search = ref('')

const selectedStudent = ref<AdminUser | null>(null)
const charges = ref<PaymentCharge[]>([])
const loadingCharges = ref(false)

const showNewCharge = ref(false)
const savingCharge = ref(false)
const chargeError = ref('')
const chargeForm = ref({ amount: null as number | null, dueDate: '' })


onMounted(async () => {
  try {
    allStudents.value = await userService.list({ role: 'STUDENT' })
  } finally {
    loadingStudents.value = false
  }
})

const filteredStudents = computed(() => {
  const q = search.value.toLowerCase()
  if (!q) return allStudents.value
  return allStudents.value.filter(u =>
    u.name.toLowerCase().includes(q) || u.email.toLowerCase().includes(q)
  )
})

function rowClass(data: AdminUser) {
  return selectedStudent.value?.id === data.id ? 'row-selected' : ''
}

async function onStudentSelect(event: { data: AdminUser }) {
  selectedStudent.value = event.data
  charges.value = []
  loadingCharges.value = true
  try {
    charges.value = await billingService.listCharges(event.data.id)
  } finally {
    loadingCharges.value = false
  }
}

function openNewCharge() {
  chargeForm.value = { amount: null, dueDate: new Date().toISOString().slice(0, 10) }
  chargeError.value = ''
  showNewCharge.value = true
}

async function submitCharge() {
  if (!selectedStudent.value || !chargeForm.value.amount || !chargeForm.value.dueDate) return
  savingCharge.value = true; chargeError.value = ''
  try {
    const created = await billingService.createCharge(selectedStudent.value.id, {
      amount: chargeForm.value.amount,
      dueDate: chargeForm.value.dueDate,
    })
    charges.value.unshift(created)
    showNewCharge.value = false
  } catch (e: any) {
    chargeError.value = e.response?.data?.message ?? 'Erro ao criar cobrança.'
  } finally { savingCharge.value = false }
}

async function doCancel(id: string) {
  await billingService.cancelCharge(id)
  const idx = charges.value.findIndex(c => c.id === id)
  if (idx !== -1) charges.value[idx] = { ...charges.value[idx], status: 'CANCELLED' }
}

function formatDate(d: string) {
  return new Date(d).toLocaleDateString('pt-BR', { timeZone: 'UTC' })
}
function statusLabel(s: string) { return chargeStatusLabel[s] ?? s }
function statusSeverity(s: string) { return chargeStatusSeverity[s] ?? 'secondary' }
function payMethodLabel(m: string | null) { return m ? (payMethodLabelMap[m] ?? m) : '—' }
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 20px; max-width: 960px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }

.search-bar { display: flex; align-items: flex-end; gap: 12px; flex-wrap: wrap; }
.search-field { display: flex; flex-direction: column; gap: 4px; flex: 1; min-width: 240px; max-width: 400px; }
.field-label { font-size: 12px; font-weight: 600; color: var(--neutral-700); }

:deep(.row-selected td) {
  background: var(--blue-50) !important;
  font-weight: 600;
}

.student-header {
  display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px;
  background: #fff; border: 1px solid var(--neutral-200); border-radius: var(--radius-lg);
  padding: 14px 18px;
}
.student-info { display: flex; align-items: center; gap: 12px; }
.student-icon { font-size: 32px; color: var(--blue-500); }
.student-name { font-weight: 700; font-size: 15px; color: var(--neutral-900); margin: 0; }
.student-email { font-size: 12px; color: var(--neutral-500); margin: 0; }

.charge-summary {
  display: flex; justify-content: space-between;
  background: var(--neutral-50); border-radius: var(--radius-md);
  padding: 10px 14px; font-size: 14px; font-weight: 600;
}

.table-scroll { overflow-x: auto; border-radius: var(--radius-lg); }
.empty-state {
  text-align: center; padding: 40px 24px;
  color: var(--neutral-500); font-size: 14px;
  background: #fff; border: 1px solid var(--neutral-200); border-radius: var(--radius-lg);
}
.field { display: flex; flex-direction: column; gap: 5px; }
.text-muted { color: var(--neutral-400); font-size: 13px; }
.error-msg { color: #ef4444; font-size: 13px; }
</style>
