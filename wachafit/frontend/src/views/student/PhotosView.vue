<!-- frontend/src/views/student/PhotosView.vue -->
<template>
  <AppLayout>
    <div class="p-6">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold">Fotos de Progresso</h1>
        <Button label="Adicionar foto" icon="pi pi-upload" @click="triggerFileInput" />
      </div>

      <!-- barra de ações de comparação -->
      <div v-if="selectedPhotos.length > 0" class="flex items-center gap-3 mb-4 p-3 bg-blue-50 rounded-lg">
        <span class="text-sm text-blue-700 font-medium">
          {{ selectedPhotos.length === 1 ? '1 foto selecionada' : '2 fotos selecionadas' }}
        </span>
        <Button
          v-if="selectedPhotos.length === 2"
          label="Comparar"
          icon="pi pi-arrows-h"
          size="small"
          @click="showCompare = true"
        />
        <Button
          label="Cancelar"
          severity="secondary"
          text
          size="small"
          @click="clearSelection"
        />
      </div>

      <input ref="fileInput" type="file" accept="image/*" class="hidden" @change="handleFileSelect" />

      <div v-if="progressStore.loading" class="text-center py-8">Carregando...</div>
      <div v-else class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
        <div
          v-for="photo in progressStore.photos"
          :key="photo.id"
          class="relative group cursor-pointer rounded-lg"
          :class="isSelected(photo) ? 'ring-2 ring-blue-500' : ''"
          @click="toggleSelect(photo)"
        >
          <img :src="photo.fileUrl" :alt="photo.notes ?? 'Foto'" class="w-full h-40 object-cover rounded-lg" />
          <div class="absolute bottom-0 left-0 right-0 bg-black/50 text-white text-xs p-1 rounded-b-lg">
            {{ photo.takenAt }}
          </div>
          <!-- checkmark de seleção -->
          <div
            v-if="isSelected(photo)"
            class="absolute top-1 left-1 bg-blue-500 rounded-full w-5 h-5 flex items-center justify-center"
          >
            <i class="pi pi-check text-white text-xs" />
          </div>
          <!-- botão delete: oculto quando selecionado -->
          <Button
            v-show="!isSelected(photo)"
            icon="pi pi-trash"
            severity="danger"
            text
            size="small"
            class="absolute top-1 right-1 opacity-0 group-hover:opacity-100 transition-opacity"
            @click.stop="deletePhoto(photo.id)"
          />
        </div>
        <div v-if="progressStore.photos.length === 0" class="col-span-4 text-surface-400 text-sm">
          Nenhuma foto registrada.
        </div>
      </div>
    </div>

    <!-- Dialog de comparação lado a lado -->
    <Dialog
      v-model:visible="showCompare"
      header="Comparação de Fotos"
      :modal="true"
      style="width: min(860px, 95vw)"
    >
      <div class="grid grid-cols-2 gap-6">
        <!-- Antes (foto mais antiga) -->
        <div v-if="comparePhotos[0]" class="flex flex-col gap-2">
          <span class="text-xs font-semibold text-surface-500 uppercase tracking-wide">Antes</span>
          <span class="text-sm text-surface-600">{{ comparePhotos[0].takenAt }}</span>
          <img
            :src="comparePhotos[0].fileUrl"
            :alt="comparePhotos[0].notes ?? 'Foto antes'"
            class="w-full h-64 object-cover rounded-lg"
          />
          <p v-if="comparePhotos[0].notes" class="text-sm text-surface-500">
            {{ comparePhotos[0].notes }}
          </p>
        </div>
        <!-- Depois (foto mais recente) -->
        <div v-if="comparePhotos[1]" class="flex flex-col gap-2">
          <span class="text-xs font-semibold text-surface-500 uppercase tracking-wide">Depois</span>
          <span class="text-sm text-surface-600">{{ comparePhotos[1].takenAt }}</span>
          <img
            :src="comparePhotos[1].fileUrl"
            :alt="comparePhotos[1].notes ?? 'Foto depois'"
            class="w-full h-64 object-cover rounded-lg"
          />
          <p v-if="comparePhotos[1].notes" class="text-sm text-surface-500">
            {{ comparePhotos[1].notes }}
          </p>
        </div>
      </div>

      <template #footer>
        <Button label="Limpar seleção" severity="secondary" text @click="clearSelection" />
        <Button label="Fechar" @click="showCompare = false" />
      </template>
    </Dialog>

  </AppLayout>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useProgressStore } from '@/stores/progress.store'
import { useAuthStore } from '@/stores/auth.store'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import type { Photo } from '@/types/api'

const progressStore = useProgressStore()
const authStore = useAuthStore()
const fileInput = ref<HTMLInputElement | null>(null)
const selectedPhotos = ref<Photo[]>([])
const showCompare = ref(false)

function isSelected(photo: Photo): boolean {
  return selectedPhotos.value.some(p => p.id === photo.id)
}

function toggleSelect(photo: Photo) {
  const idx = selectedPhotos.value.findIndex(p => p.id === photo.id)
  if (idx >= 0) {
    selectedPhotos.value.splice(idx, 1)
  } else if (selectedPhotos.value.length < 2) {
    selectedPhotos.value.push(photo)
  }
}

const comparePhotos = computed(() =>
  [...selectedPhotos.value].sort(
    (a, b) => new Date(a.takenAt).getTime() - new Date(b.takenAt).getTime()
  )
)

function clearSelection() {
  selectedPhotos.value = []
  showCompare.value = false
}

onMounted(() => progressStore.fetchPhotos(authStore.userId!))

function triggerFileInput() { fileInput.value?.click() }

async function handleFileSelect(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  await progressStore.uploadPhoto(authStore.userId!, file)
  input.value = ''
}

async function deletePhoto(id: string) { await progressStore.deletePhoto(id) }
</script>
