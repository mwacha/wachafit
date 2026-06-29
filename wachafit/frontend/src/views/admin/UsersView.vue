<!-- frontend/src/views/admin/UsersView.vue -->
<template>
  <AppLayout>
    <div class="p-6">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold">Usuários</h1>
        <Button label="Novo usuário" icon="pi pi-plus" @click="showCreate = true" />
      </div>

      <DataTable :value="adminStore.users" :loading="adminStore.loading" stripedRows>
        <Column field="name" header="Nome" />
        <Column field="email" header="Email" />
        <Column field="role" header="Perfil" />
        <Column header="Status">
          <template #body="{ data }">
            <Tag :severity="data.active ? 'success' : 'danger'" :value="data.active ? 'Ativo' : 'Inativo'" />
          </template>
        </Column>
        <Column header="Ações">
          <template #body="{ data }">
            <Button v-if="data.active" icon="pi pi-ban" severity="danger" text @click="deactivate(data.id)" />
            <Button v-else icon="pi pi-check" severity="success" text @click="activate(data.id)" />
          </template>
        </Column>
      </DataTable>

      <Dialog v-model:visible="showCreate" header="Novo Usuário" :modal="true" style="width: 420px">
        <form @submit.prevent="submitCreate" class="flex flex-col gap-3">
          <InputText v-model="form.name" placeholder="Nome" required />
          <InputText v-model="form.email" type="email" placeholder="Email" required />
          <Password v-model="form.password" placeholder="Senha" :feedback="false" required />
          <Select v-model="form.role" :options="['ADMIN','TRAINER','STUDENT']" placeholder="Perfil" required />
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
import { userService } from '@/services/user.service'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Select from 'primevue/select'

const adminStore = useAdminStore()
const showCreate = ref(false)
const saving = ref(false)
const formError = ref<string | null>(null)
const form = ref({ name: '', email: '', password: '', role: '' })

onMounted(() => adminStore.fetchUsers())

async function deactivate(id: string) {
  await userService.deactivate(id)
  await adminStore.fetchUsers()
}

async function activate(id: string) {
  await userService.activate(id)
  await adminStore.fetchUsers()
}

async function submitCreate() {
  saving.value = true; formError.value = null
  try {
    await userService.create(form.value as any)
    showCreate.value = false
    form.value = { name: '', email: '', password: '', role: '' }
    await adminStore.fetchUsers()
  } catch (e: any) {
    formError.value = e.response?.data?.message ?? 'Erro ao criar usuário'
  } finally { saving.value = false }
}
</script>
