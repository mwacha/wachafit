<template>
  <AppLayout>
    <div class="p-6 max-w-4xl">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold">Planos de Matrícula</h1>
        <Button icon="pi pi-plus" label="Novo Plano" @click="openCreate" />
      </div>

      <div v-if="loading" class="text-center py-8">Carregando...</div>

      <DataTable v-else :value="plans" stripedRows>
        <Column field="name" header="Nome" />
        <Column header="Duração">
          <template #body="{ data }">{{ data.durationMonths }} mes(es)</template>
        </Column>
        <Column header="Preço">
          <template #body="{ data }">R$ {{ data.price.toFixed(2) }}</template>
        </Column>
        <Column header="Status">
          <template #body="{ data }">
            <Tag :value="data.active ? 'Ativo' : 'Inativo'" :severity="data.active ? 'success' : 'secondary'" />
          </template>
        </Column>
        <Column header="Ações">
          <template #body="{ data }">
            <Button icon="pi pi-pencil" text size="small" @click="openEdit(data)" />
            <Button v-if="data.active" icon="pi pi-trash" text severity="danger" size="small" @click="deactivate(data.id)" />
          </template>
        </Column>
      </DataTable>

      <Dialog v-model:visible="showDialog" :header="editing ? 'Editar Plano' : 'Novo Plano'" :modal="true" style="width:420px">
        <form @submit.prevent="save" class="flex flex-col gap-3 pt-2">
          <InputText v-model="form.name" placeholder="Nome do plano" required />
          <InputText v-model="form.description" placeholder="Descrição (opcional)" />
          <InputNumber v-model="form.durationMonths" placeholder="Duração (meses)" :min="1" required />
          <InputNumber v-model="form.price" placeholder="Preço (R$)" :min="0.01" :minFractionDigits="2" required />
          <InputNumber v-model="form.maxClassesPerWeek" placeholder="Máx. aulas/semana (opcional)" :min="1" />
          <div class="flex gap-2 justify-end mt-2">
            <Button type="button" label="Cancelar" class="p-button-text" @click="showDialog = false" />
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
