<!-- frontend/src/views/admin/ClassesView.vue -->
<template>
  <AppLayout>
    <div class="view-wrap">
      <div class="page-header">
        <h1 class="page-title">Turmas</h1>
        <Button label="Nova turma" icon="pi pi-plus" @click="showCreate = true" />
      </div>

      <div class="table-scroll">
        <DataTable :value="adminStore.classes" :loading="adminStore.loading" stripedRows>
          <Column field="name" header="Nome" style="min-width:140px" />
          <Column field="capacity" header="Capacidade" style="min-width:110px" />
          <Column field="durationMinutes" header="Duração (min)" style="min-width:120px" />
          <Column header="Status" style="min-width:90px">
            <template #body="{ data }">
              <Tag :severity="data.active ? 'success' : 'danger'" :value="data.active ? 'Ativa' : 'Inativa'" />
            </template>
          </Column>
          <Column header="Ações" style="min-width:80px">
            <template #body="{ data }">
              <Button v-if="data.active" icon="pi pi-trash" severity="danger" text @click="deactivate(data.id)" />
            </template>
          </Column>
        </DataTable>
      </div>

      <Dialog v-model:visible="showCreate" header="Nova Turma" :modal="true" style="width: min(420px, 95vw)">
        <form @submit.prevent="submitCreate" class="flex flex-col gap-3">
          <InputText v-model="form.name" placeholder="Nome" required />
          <InputNumber v-model="form.capacity" placeholder="Capacidade" :min="1" required />
          <InputNumber v-model="form.durationMinutes" placeholder="Duração (minutos)" :min="1" required />
          <p v-if="formError" class="text-red-500 text-sm">{{ formError }}</p>
          <Button type="submit" label="Criar" :loading="saving" />
        </form>
      </Dialog>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useAdminStore } from '@/stores/admin.store'
import { useAuthStore } from '@/stores/auth.store'
import { groupClassService } from '@/services/groupclass.service'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'

const adminStore = useAdminStore()
const authStore = useAuthStore()
const showCreate = ref(false)
const saving = ref(false)
const formError = ref<string | null>(null)
const form = ref({ name: '', capacity: 10, durationMinutes: 60 })

onMounted(() => adminStore.fetchClasses())

async function deactivate(id: string) {
  await groupClassService.deactivate(id)
  await adminStore.fetchClasses()
}

async function submitCreate() {
  saving.value = true; formError.value = null
  try {
    await groupClassService.create({ ...form.value, trainerId: authStore.userId! })
    showCreate.value = false
    await adminStore.fetchClasses()
  } catch (e: any) {
    formError.value = e.response?.data?.message ?? 'Erro ao criar turma'
  } finally { saving.value = false }
}
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 20px; }
.page-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }
.table-scroll { overflow-x: auto; border-radius: var(--radius-lg); }
</style>
