<!-- frontend/src/views/exercises/ExercisesView.vue -->
<template>
  <AppLayout>
    <div class="view-wrap">
      <div class="page-header">
        <h1 class="page-title">Biblioteca de Exercícios</h1>
        <Button icon="pi pi-plus" label="Novo exercício" @click="openCreate" />
      </div>

      <!-- Filtros -->
      <div class="filters-row">
        <span class="p-input-icon-left search-wrap">
          <i class="pi pi-search" />
          <InputText v-model="searchQ" placeholder="Buscar por nome..." @input="onSearch" />
        </span>
        <Dropdown v-model="selectedGroup" :options="muscleGroups" placeholder="Grupo muscular"
          showClear @change="onSearch" style="width:200px" />
      </div>

      <DataTable paginator :rows="10" :rowsPerPageOptions="[10, 25, 50]" :value="exercises" :loading="loading" stripedRows>
        <template #empty>Nenhum exercício encontrado.</template>
        <Column field="name" header="Nome" style="min-width:160px" />
        <Column field="muscleGroup" header="Grupo Muscular" style="min-width:140px" />
        <Column field="description" header="Descrição" style="min-width:200px">
          <template #body="{ data }">{{ data.description ?? '—' }}</template>
        </Column>
        <Column header="Vídeo" style="min-width:80px">
          <template #body="{ data }">
            <a v-if="data.videoUrl" :href="data.videoUrl" target="_blank" class="video-link">
              <i class="pi pi-external-link" /> Ver
            </a>
            <span v-else>—</span>
          </template>
        </Column>
        <Column header="Status" style="min-width:90px">
          <template #body="{ data }">
            <Tag :severity="data.active ? 'success' : 'secondary'" :value="data.active ? 'Ativo' : 'Inativo'" />
          </template>
        </Column>
        <Column header="Ações" style="min-width:120px">
          <template #body="{ data }">
            <div class="flex gap-1">
              <Button icon="pi pi-pencil" text size="small" title="Editar" @click="openEdit(data)" />
              <Button v-if="auth.role === 'ADMIN' && data.active"
                icon="pi pi-ban" text severity="danger" size="small" title="Desativar"
                :loading="deactivatingId === data.id"
                @click="deactivate(data.id)" />
            </div>
          </template>
        </Column>
      </DataTable>

      <p v-if="successMsg" class="success-msg">{{ successMsg }}</p>

      <!-- Dialog criar/editar -->
      <Dialog v-model:visible="showDialog" :header="editingId ? 'Editar exercício' : 'Novo exercício'"
        :modal="true" style="width: min(460px, 95vw)">
        <form @submit.prevent="submitForm" class="exercise-form">
          <div class="form-field">
            <label class="form-label">Nome *</label>
            <InputText v-model="form.name" placeholder="Ex: Supino reto" class="w-full" required />
          </div>
          <div class="form-field">
            <label class="form-label">Grupo muscular *</label>
            <Dropdown v-model="form.muscleGroup" :options="muscleGroups" placeholder="Selecione o grupo" class="w-full" required />
          </div>
          <div class="form-field">
            <label class="form-label">Descrição</label>
            <Textarea v-model="form.description" rows="3" placeholder="Descrição do exercício (opcional)" class="w-full" autoResize />
          </div>
          <div class="form-field">
            <label class="form-label">URL do vídeo</label>
            <InputText v-model="form.videoUrl" placeholder="https://..." class="w-full" />
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
import { ref, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { exerciseService } from '@/services/exercise.service'
import { useAuthStore } from '@/stores/auth.store'
import type { Exercise } from '@/types/api'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'
import Dropdown from 'primevue/dropdown'

const auth = useAuthStore()
const exercises = ref<Exercise[]>([])
const loading = ref(false)
const searchQ = ref('')
const selectedGroup = ref<string | null>(null)
const showDialog = ref(false)
const saving = ref(false)
const editingId = ref<string | null>(null)
const deactivatingId = ref<string | null>(null)
const form = ref({ name: '', muscleGroup: '', description: '', videoUrl: '' })

const successMsg = ref('')
function showSuccess(msg: string) {
  successMsg.value = msg
  setTimeout(() => { successMsg.value = '' }, 3000)
}

const muscleGroups = [
  'Peito', 'Costas', 'Ombros', 'Bíceps', 'Tríceps',
  'Antebraço', 'Abdômen', 'Glúteos', 'Quadríceps',
  'Isquiotibiais', 'Panturrilha', 'Cardio', 'Funcional',
]

let searchTimer: ReturnType<typeof setTimeout>

onMounted(() => loadExercises())

async function loadExercises() {
  loading.value = true
  try {
    exercises.value = await exerciseService.search({
      q: searchQ.value || undefined,
      muscleGroup: selectedGroup.value || undefined,
    })
  } finally { loading.value = false }
}

function onSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(loadExercises, 300)
}

function openCreate() {
  editingId.value = null
  form.value = { name: '', muscleGroup: '', description: '', videoUrl: '' }
  showDialog.value = true
}

function openEdit(ex: Exercise) {
  editingId.value = ex.id
  form.value = { name: ex.name, muscleGroup: ex.muscleGroup, description: ex.description ?? '', videoUrl: ex.videoUrl ?? '' }
  showDialog.value = true
}

async function submitForm() {
  saving.value = true
  try {
    const payload = {
      name: form.value.name,
      muscleGroup: form.value.muscleGroup,
      description: form.value.description || undefined,
      videoUrl: form.value.videoUrl || undefined,
    }
    if (editingId.value) {
      const updated = await exerciseService.update(editingId.value, payload)
      const idx = exercises.value.findIndex(e => e.id === editingId.value)
      if (idx !== -1) exercises.value[idx] = updated
      showDialog.value = false
      showSuccess('Exercício atualizado.')
    } else {
      const created = await exerciseService.create(payload)
      exercises.value.unshift(created)
      showDialog.value = false
      showSuccess('Exercício criado.')
    }
  } finally { saving.value = false }
}

async function deactivate(id: string) {
  deactivatingId.value = id
  try {
    await exerciseService.deactivate(id)
    const idx = exercises.value.findIndex(e => e.id === id)
    if (idx !== -1) exercises.value[idx] = { ...exercises.value[idx], active: false }
    showSuccess('Exercício desativado.')
  } finally { deactivatingId.value = null }
}
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 16px; max-width: 900px; }
.page-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }
.filters-row { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; }
.search-wrap { position: relative; }
.video-link { color: var(--blue-500); font-size: 13px; display: flex; align-items: center; gap: 4px; }
.success-msg { color: #22c55e; font-size: 0.875rem; margin-top: 0; }

.exercise-form { display: flex; flex-direction: column; gap: 20px; padding: 8px 0 4px; }
.form-field { display: flex; flex-direction: column; gap: 6px; }
.form-label { font-size: 13px; font-weight: 600; color: var(--neutral-700); }
.form-actions { display: flex; justify-content: flex-end; gap: 8px; padding-top: 4px; }
</style>
