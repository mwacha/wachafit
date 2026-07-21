<!-- frontend/src/views/admin/UsersView.vue -->
<template>
  <AppLayout>
    <div class="view-wrap">
      <div class="page-header">
        <h1 class="page-title">Usuários</h1>
        <Button label="Novo usuário" icon="pi pi-plus" @click="showCreate = true" />
      </div>

      <div class="table-scroll">
        <DataTable paginator :rows="10" :rowsPerPageOptions="[10, 25, 50]" :value="adminStore.users" :loading="adminStore.loading" stripedRows>
          <template #empty>Nenhum usuário cadastrado.</template>
          <Column field="name" header="Nome" style="min-width:140px" />
          <Column field="email" header="Email" style="min-width:180px" />
          <Column header="Perfil" style="min-width:100px">
            <template #body="{ data }">{{ roleLabel[data.role] ?? data.role }}</template>
          </Column>
          <Column header="Status" style="min-width:90px">
            <template #body="{ data }">
              <Tag :severity="data.active ? 'success' : 'danger'" :value="data.active ? 'Ativo' : 'Inativo'" />
            </template>
          </Column>
          <Column header="Ações" style="min-width:120px">
            <template #body="{ data }">
              <Button icon="pi pi-pencil" text @click="openEdit(data)" />
              <Button v-if="data.active" icon="pi pi-ban" severity="danger" text @click="deactivate(data.id)" />
              <Button v-else icon="pi pi-check" severity="success" text @click="activate(data.id)" />
            </template>
          </Column>
        </DataTable>
      </div>

      <Dialog v-model:visible="showCreate" header="Novo Usuário" :modal="true" style="width: min(440px, 95vw)">
        <form @submit.prevent="submitCreate" class="user-form">
          <div class="form-field">
            <label class="form-label">Nome *</label>
            <InputText v-model="form.name" placeholder="Nome completo" style="width:100%" required />
          </div>
          <div class="form-field">
            <label class="form-label">E-mail *</label>
            <InputText v-model="form.email" type="email" placeholder="email@exemplo.com" style="width:100%" required />
          </div>
          <div class="form-field">
            <label class="form-label">Senha *</label>
            <Password v-model="form.password" placeholder="Senha" :feedback="false" style="width:100%" required />
          </div>
          <div class="form-field">
            <label class="form-label">Perfil *</label>
            <Select v-model="form.role" :options="roleOptions" optionLabel="label" optionValue="value"
              placeholder="Selecione o perfil" style="width:100%" required />
          </div>
          <p v-if="formError" class="error-msg">{{ formError }}</p>
          <div class="form-actions">
            <Button type="button" label="Cancelar" outlined @click="showCreate = false" />
            <Button type="submit" label="Criar usuário" :loading="saving" />
          </div>
        </form>
      </Dialog>

      <Dialog v-model:visible="showEdit" header="Editar Usuário" :modal="true" style="width: min(440px, 95vw)">
        <form @submit.prevent="submitEdit" class="user-form">
          <div class="form-field">
            <label class="form-label">Nome *</label>
            <InputText v-model="editForm.name" placeholder="Nome completo" style="width:100%" required />
          </div>
          <div class="form-field">
            <label class="form-label">Perfil *</label>
            <Select v-model="editForm.role" :options="roleOptions" optionLabel="label" optionValue="value"
              placeholder="Selecione o perfil" style="width:100%" required />
          </div>
          <p v-if="formError" class="error-msg">{{ formError }}</p>
          <div class="form-actions">
            <Button type="button" label="Cancelar" outlined @click="showEdit = false" />
            <Button type="submit" label="Salvar alterações" :loading="saving" />
          </div>
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
import { roleLabel, roleOptions } from '@/utils/labels'
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
const showEdit = ref(false)
const editingId = ref<string | null>(null)
const saving = ref(false)
const formError = ref<string | null>(null)
const form = ref({ name: '', email: '', password: '', role: '' })
const editForm = ref({ name: '', role: '' })

onMounted(() => adminStore.fetchUsers())

async function deactivate(id: string) {
  await userService.deactivate(id)
  await adminStore.fetchUsers()
}

async function activate(id: string) {
  await userService.activate(id)
  await adminStore.fetchUsers()
}

function openEdit(user: { id: string; name: string; role: string }) {
  editingId.value = user.id
  editForm.value = { name: user.name, role: user.role }
  formError.value = null
  showEdit.value = true
}

async function submitEdit() {
  if (!editingId.value) return
  saving.value = true; formError.value = null
  try {
    await userService.update(editingId.value, editForm.value)
    showEdit.value = false
    await adminStore.fetchUsers()
  } catch (e: any) {
    formError.value = e.response?.data?.message ?? 'Erro ao salvar'
  } finally { saving.value = false }
}

async function submitCreate() {
  if (form.value.password.length < 8) {
    formError.value = 'A senha deve ter no mínimo 8 caracteres.'; return
  }
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

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 20px; }
.page-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }
.table-scroll { overflow-x: auto; border-radius: var(--radius-lg); }
.user-form { display: flex; flex-direction: column; gap: 18px; padding: 8px 0 4px; }
.form-field { display: flex; flex-direction: column; gap: 6px; }
.form-label { font-size: 13px; font-weight: 600; color: var(--neutral-700); }
.form-actions { display: flex; justify-content: flex-end; gap: 8px; padding-top: 4px; }
.error-msg { color: #ef4444; font-size: 13px; }
</style>
