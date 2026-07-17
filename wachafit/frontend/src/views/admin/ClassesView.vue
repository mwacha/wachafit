<!-- frontend/src/views/admin/ClassesView.vue -->
<template>
  <AppLayout>
    <div class="view-wrap">
      <div class="page-header">
        <h1 class="page-title">Turmas</h1>
        <Button label="Nova turma" icon="pi pi-plus" @click="openCreate" />
      </div>

      <div class="table-scroll">
        <DataTable :value="adminStore.classes" :loading="adminStore.loading" stripedRows>
          <template #empty>Nenhuma turma cadastrada.</template>
          <Column field="name" header="Nome" style="min-width:140px" />
          <Column header="Tipo" style="min-width:130px">
            <template #body="{ data }">
              <Tag :severity="data.scheduleType === 'FIXED' ? 'info' : 'secondary'"
                   :value="data.scheduleType === 'FIXED' ? 'Aula Fixa' : 'Horário Livre'" />
            </template>
          </Column>
          <Column header="Horário / Duração" style="min-width:160px">
            <template #body="{ data }">
              <span v-if="data.scheduleType === 'FIXED' && data.startTime && data.endTime">
                {{ data.startTime.slice(0,5) }} – {{ data.endTime.slice(0,5) }}
              </span>
              <span v-else>{{ data.durationMinutes }} min</span>
            </template>
          </Column>
          <Column field="capacity" header="Vagas" style="min-width:80px" />
          <Column header="Status" style="min-width:90px">
            <template #body="{ data }">
              <Tag :severity="data.active ? 'success' : 'danger'" :value="data.active ? 'Ativa' : 'Inativa'" />
            </template>
          </Column>
          <Column header="Ações" style="min-width:100px">
            <template #body="{ data }">
              <Button icon="pi pi-pencil" text @click="openEdit(data)" />
              <Button v-if="data.active" icon="pi pi-trash" severity="danger" text @click="deactivate(data.id)" />
            </template>
          </Column>
        </DataTable>
      </div>

      <!-- Dialog: Nova Turma -->
      <Dialog v-model:visible="showCreate" header="Nova Turma" :modal="true" style="width: min(480px, 95vw)">
        <form @submit.prevent="submitCreate" class="flex flex-col gap-3 pt-2">
          <div class="field">
            <label class="field-label">Nome *</label>
            <InputText v-model="form.name" class="w-full" required />
          </div>
          <div class="field">
            <label class="field-label">Tipo de turma *</label>
            <div class="type-toggle">
              <button type="button"
                :class="['toggle-btn', { active: form.scheduleType === 'FLEX' }]"
                @click="form.scheduleType = 'FLEX'">
                <i class="pi pi-clock" /> Horário Livre
              </button>
              <button type="button"
                :class="['toggle-btn', { active: form.scheduleType === 'FIXED' }]"
                @click="form.scheduleType = 'FIXED'">
                <i class="pi pi-calendar" /> Aula Fixa
              </button>
            </div>
          </div>

          <template v-if="form.scheduleType === 'FIXED'">
            <div class="grid grid-cols-2 gap-3">
              <div class="field">
                <label class="field-label">Horário início *</label>
                <InputText v-model="form.startTime" type="time" class="w-full" required />
              </div>
              <div class="field">
                <label class="field-label">Horário fim *</label>
                <InputText v-model="form.endTime" type="time" class="w-full" required />
              </div>
            </div>
          </template>

          <template v-else>
            <div class="field">
              <label class="field-label">Duração (minutos) *</label>
              <InputNumber v-model="form.durationMinutes" :min="1" class="w-full" required />
            </div>
          </template>

          <div class="field">
            <label class="field-label">Capacidade (vagas) *</label>
            <InputNumber v-model="form.capacity" :min="1" class="w-full" required />
          </div>

          <p v-if="formError" class="text-red-500 text-sm">{{ formError }}</p>
          <div class="flex justify-end gap-2 mt-1">
            <Button type="button" label="Cancelar" outlined @click="showCreate = false" />
            <Button type="submit" label="Criar" :loading="saving" />
          </div>
        </form>
      </Dialog>

      <!-- Dialog: Editar Turma -->
      <Dialog v-model:visible="showEdit" header="Editar Turma" :modal="true" style="width: min(480px, 95vw)">
        <form @submit.prevent="submitEdit" class="flex flex-col gap-3 pt-2">
          <div class="field">
            <label class="field-label">Nome *</label>
            <InputText v-model="editForm.name" class="w-full" required />
          </div>
          <div class="field">
            <label class="field-label">Tipo de turma *</label>
            <div class="type-toggle">
              <button type="button"
                :class="['toggle-btn', { active: editForm.scheduleType === 'FLEX' }]"
                @click="editForm.scheduleType = 'FLEX'">
                <i class="pi pi-clock" /> Horário Livre
              </button>
              <button type="button"
                :class="['toggle-btn', { active: editForm.scheduleType === 'FIXED' }]"
                @click="editForm.scheduleType = 'FIXED'">
                <i class="pi pi-calendar" /> Aula Fixa
              </button>
            </div>
          </div>

          <template v-if="editForm.scheduleType === 'FIXED'">
            <div class="grid grid-cols-2 gap-3">
              <div class="field">
                <label class="field-label">Horário início *</label>
                <InputText v-model="editForm.startTime" type="time" class="w-full" required />
              </div>
              <div class="field">
                <label class="field-label">Horário fim *</label>
                <InputText v-model="editForm.endTime" type="time" class="w-full" required />
              </div>
            </div>
          </template>

          <template v-else>
            <div class="field">
              <label class="field-label">Duração (minutos) *</label>
              <InputNumber v-model="editForm.durationMinutes" :min="1" class="w-full" required />
            </div>
          </template>

          <div class="field">
            <label class="field-label">Capacidade (vagas) *</label>
            <InputNumber v-model="editForm.capacity" :min="1" class="w-full" required />
          </div>

          <p v-if="formError" class="text-red-500 text-sm">{{ formError }}</p>
          <div class="flex justify-end gap-2 mt-1">
            <Button type="button" label="Cancelar" outlined @click="showEdit = false" />
            <Button type="submit" label="Salvar" :loading="saving" />
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
import { useAuthStore } from '@/stores/auth.store'
import { groupClassService } from '@/services/groupclass.service'
import type { GroupClass } from '@/types/api'
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
const showEdit = ref(false)
const editingId = ref<string | null>(null)
const saving = ref(false)
const formError = ref<string | null>(null)

const defaultForm = () => ({ name: '', capacity: 10, scheduleType: 'FLEX', durationMinutes: 60, startTime: '', endTime: '' })
const form = ref(defaultForm())
const editForm = ref(defaultForm())

onMounted(() => adminStore.fetchClasses())

function openCreate() {
  form.value = defaultForm()
  formError.value = null
  showCreate.value = true
}

function openEdit(cls: GroupClass) {
  editingId.value = cls.id
  editForm.value = {
    name: cls.name,
    capacity: cls.capacity,
    scheduleType: cls.scheduleType ?? 'FLEX',
    durationMinutes: cls.durationMinutes,
    startTime: cls.startTime?.slice(0, 5) ?? '',
    endTime: cls.endTime?.slice(0, 5) ?? '',
  }
  formError.value = null
  showEdit.value = true
}

function buildPayload(f: typeof form.value) {
  if (f.scheduleType === 'FIXED') {
    return { name: f.name, capacity: f.capacity, scheduleType: 'FIXED', startTime: f.startTime, endTime: f.endTime }
  }
  return { name: f.name, capacity: f.capacity, scheduleType: 'FLEX', durationMinutes: f.durationMinutes }
}

async function submitCreate() {
  saving.value = true; formError.value = null
  try {
    await groupClassService.create({ ...buildPayload(form.value), trainerId: authStore.userId! })
    showCreate.value = false
    await adminStore.fetchClasses()
  } catch (e: any) {
    formError.value = e.response?.data?.message ?? 'Erro ao criar turma'
  } finally { saving.value = false }
}

async function submitEdit() {
  if (!editingId.value) return
  saving.value = true; formError.value = null
  try {
    await groupClassService.update(editingId.value, buildPayload(editForm.value))
    showEdit.value = false
    await adminStore.fetchClasses()
  } catch (e: any) {
    formError.value = e.response?.data?.message ?? 'Erro ao salvar'
  } finally { saving.value = false }
}

async function deactivate(id: string) {
  await groupClassService.deactivate(id)
  await adminStore.fetchClasses()
}
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 20px; }
.page-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }
.table-scroll { overflow-x: auto; border-radius: var(--radius-lg); }
.field { display: flex; flex-direction: column; gap: 5px; }
.field-label { font-size: 12px; font-weight: 600; color: var(--neutral-700); }

.type-toggle { display: flex; gap: 8px; }
.toggle-btn {
  flex: 1; display: flex; align-items: center; justify-content: center; gap: 6px;
  padding: 9px 12px; border-radius: var(--radius-md);
  border: 1.5px solid var(--neutral-200); background: #fff;
  font-size: 13px; font-weight: 500; color: var(--neutral-600);
  cursor: pointer; transition: all .15s;
}
.toggle-btn:hover { border-color: var(--blue-400); color: var(--blue-600); }
.toggle-btn.active {
  border-color: var(--blue-500); background: var(--blue-50);
  color: var(--blue-700); font-weight: 600;
}
</style>
