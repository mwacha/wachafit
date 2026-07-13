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

      <DataTable :value="exercises" :loading="loading" stripedRows
        emptyMessage="Nenhum exercício encontrado.">
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

      <!-- Dialog criar/editar -->
      <Dialog v-model:visible="showDialog" :header="editingId ? 'Editar exercício' : 'Novo exercício'"
        :modal="true" style="width: min(460px, 95vw)">
        <form @submit.prevent="submitForm" class="flex flex-col gap-3 pt-2">
          <div class="flex flex-col gap-1">
            <label class="field-label">Nome *</label>
            <InputText v-model="form.name" required />
          </div>
          <div class="flex flex-col gap-1">
            <label class="field-label">Grupo muscular *</label>
            <InputText v-model="form.muscleGroup" required />
          </div>
          <div class="flex flex-col gap-1">
            <label class="field-label">Descrição</label>
            <Textarea v-model="form.description" rows="2" />
          </div>
          <div class="flex flex-col gap-1">
            <label class="field-label">URL do vídeo</label>
            <InputText v-model="form.videoUrl" placeholder="https://..." />
          </div>
          <div class="flex justify-end gap-2 mt-2">
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
    } else {
      const created = await exerciseService.create(payload)
      exercises.value.unshift(created)
    }
    showDialog.value = false
  } finally { saving.value = false }
}

async function deactivate(id: string) {
  deactivatingId.value = id
  try {
    await exerciseService.deactivate(id)
    const idx = exercises.value.findIndex(e => e.id === id)
    if (idx !== -1) exercises.value[idx] = { ...exercises.value[idx], active: false }
  } finally { deactivatingId.value = null }
}
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 16px; max-width: 900px; }
.page-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }
.filters-row { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; }
.search-wrap { position: relative; }
.field-label { font-size: 12px; font-weight: 600; color: var(--neutral-600); }
.video-link { color: var(--blue-500); font-size: 13px; display: flex; align-items: center; gap: 4px; }
</style>
