<template>
  <AppLayout>
    <div class="view-wrap">
      <div class="page-header">
        <h1 class="page-title">Planos de Matrícula</h1>
        <Button icon="pi pi-plus" label="Novo Plano" @click="openCreate" />
      </div>

      <div v-if="loading" class="empty-state">Carregando...</div>

      <div v-else class="table-scroll">
        <DataTable paginator :rows="10" :rowsPerPageOptions="[10, 25, 50]" :value="plans" stripedRows>
          <template #empty>Nenhum plano cadastrado.</template>
          <Column field="name" header="Nome" style="min-width:140px" />
          <Column header="Duração" style="min-width:110px">
            <template #body="{ data }">{{ data.durationMonths }} mes(es)</template>
          </Column>
          <Column header="Preço" style="min-width:100px">
            <template #body="{ data }">R$ {{ data.price.toFixed(2) }}</template>
          </Column>
          <Column header="Status" style="min-width:90px">
            <template #body="{ data }">
              <Tag :value="data.active ? 'Ativo' : 'Inativo'" :severity="data.active ? 'success' : 'secondary'" />
            </template>
          </Column>
          <Column header="Ações" style="min-width:100px">
            <template #body="{ data }">
              <Button icon="pi pi-pencil" text size="small" @click="openEdit(data)" />
              <Button v-if="data.active" icon="pi pi-trash" text severity="danger" size="small" @click="deactivate(data.id)" />
            </template>
          </Column>
        </DataTable>
      </div>

      <Dialog v-model:visible="showDialog" :header="editing ? 'Editar Plano' : 'Novo Plano'" :modal="true" style="width: min(420px, 95vw)">
        <form @submit.prevent="save" class="plan-form">
          <div class="form-field">
            <label class="form-label">Nome do plano *</label>
            <InputText v-model="form.name" placeholder="Ex: Plano Mensal" class="w-full" required />
          </div>
          <div class="form-field">
            <label class="form-label">Descrição</label>
            <InputText v-model="form.description" placeholder="Descrição opcional" class="w-full" />
          </div>
          <div class="form-row">
            <div class="form-field">
              <label class="form-label">Duração (meses) *</label>
              <InputNumber v-model="form.durationMonths" :min="1" class="w-full" required />
            </div>
            <div class="form-field">
              <label class="form-label">Preço (R$) *</label>
              <InputNumber v-model="form.price" :min="0.01" :minFractionDigits="2" mode="currency" currency="BRL" locale="pt-BR" class="w-full" required />
            </div>
          </div>
          <div class="form-field">
            <label class="form-label">Máx. aulas por semana</label>
            <InputNumber v-model="form.maxClassesPerWeek" :min="1" placeholder="Sem limite" class="w-full" />
          </div>
          <div class="form-actions">
            <Button type="button" label="Cancelar" outlined @click="showDialog = false" />
            <Button type="submit" label="Salvar" :loading="saving" />
          </div>
        </form>
      </Dialog>
    </div>
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
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import membershipService from '@/services/membership.service'
import type { MembershipPlan } from '@/types/api'

const plans = ref<MembershipPlan[]>([])
const loading = ref(true)
const showDialog = ref(false)
const editing = ref<MembershipPlan | null>(null)
const saving = ref(false)
const form = ref({ name: '', description: '', durationMonths: 1, price: 0, maxClassesPerWeek: null as number | null })

onMounted(async () => {
  plans.value = await membershipService.listPlans()
  loading.value = false
})

function openCreate() {
  editing.value = null
  form.value = { name: '', description: '', durationMonths: 1, price: 0, maxClassesPerWeek: null }
  showDialog.value = true
}

function openEdit(plan: MembershipPlan) {
  editing.value = plan
  form.value = { name: plan.name, description: plan.description ?? '', durationMonths: plan.durationMonths, price: plan.price, maxClassesPerWeek: plan.maxClassesPerWeek }
  showDialog.value = true
}

async function save() {
  saving.value = true
  try {
    if (editing.value) {
      const updated = await membershipService.updatePlan(editing.value.id, form.value)
      const idx = plans.value.findIndex(p => p.id === updated.id)
      if (idx !== -1) plans.value[idx] = updated
    } else {
      const created = await membershipService.createPlan(form.value)
      plans.value.unshift(created)
    }
    showDialog.value = false
  } finally { saving.value = false }
}

async function deactivate(id: string) {
  await membershipService.deactivatePlan(id)
  const idx = plans.value.findIndex(p => p.id === id)
  if (idx !== -1) plans.value[idx] = { ...plans.value[idx], active: false }
}
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 20px }
.page-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }
.table-scroll { overflow-x: auto; border-radius: var(--radius-lg); }
.empty-state { text-align: center; padding: 40px; color: var(--neutral-500); }

.plan-form { display: flex; flex-direction: column; gap: 20px; padding: 8px 0 4px; }
.form-field { display: flex; flex-direction: column; gap: 6px; flex: 1; }
.form-label { font-size: 13px; font-weight: 600; color: var(--neutral-700); }
.form-row { display: flex; gap: 14px; }
.form-actions { display: flex; justify-content: flex-end; gap: 8px; padding-top: 4px; }
</style>
