<!-- frontend/src/views/admin/ClassesView.vue -->
<template>
  <AppLayout>
    <div class="p-6">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold">Turmas</h1>
        <Button label="Nova turma" icon="pi pi-plus" @click="showCreate = true" />
      </div>

      <DataTable :value="adminStore.classes" :loading="adminStore.loading" stripedRows>
        <Column field="name" header="Nome" />
        <Column field="capacity" header="Capacidade" />
        <Column field="durationMinutes" header="Duração (min)" />
        <Column header="Status">
          <template #body="{ data }">
            <Tag :severity="data.active ? 'success' : 'danger'" :value="data.active ? 'Ativa' : 'Inativa'" />
          </template>
        </Column>
        <Column header="Ações">
          <template #body="{ data }">
            <Button v-if="data.active" icon="pi pi-trash" severity="danger" text @click="deactivate(data.id)" />
          </template>
        </Column>
      </DataTable>

      <Dialog v-model:visible="showCreate" header="Nova Turma" :modal="true" style="width: 420px">
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
