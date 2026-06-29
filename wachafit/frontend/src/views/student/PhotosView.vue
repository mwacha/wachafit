<!-- frontend/src/views/student/PhotosView.vue -->
<template>
  <AppLayout>
    <div class="p-6">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold">Fotos de Progresso</h1>
        <Button label="Adicionar foto" icon="pi pi-upload" @click="triggerFileInput" />
      </div>
      <input ref="fileInput" type="file" accept="image/*" class="hidden" @change="handleFileSelect" />

      <div v-if="progressStore.loading" class="text-center py-8">Carregando...</div>
      <div v-else class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
        <div v-for="photo in progressStore.photos" :key="photo.id" class="relative group">
          <img :src="photo.fileUrl" :alt="photo.notes ?? 'Foto'" class="w-full h-40 object-cover rounded-lg" />
          <div class="absolute bottom-0 left-0 right-0 bg-black/50 text-white text-xs p-1 rounded-b-lg">
            {{ photo.takenAt }}
          </div>
          <Button icon="pi pi-trash" severity="danger" text size="small"
            class="absolute top-1 right-1 opacity-0 group-hover:opacity-100 transition-opacity"
            @click="deletePhoto(photo.id)" />
        </div>
        <div v-if="progressStore.photos.length === 0" class="col-span-4 text-surface-400 text-sm">
          Nenhuma foto registrada.
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useProgressStore } from '@/stores/progress.store'
import { useAuthStore } from '@/stores/auth.store'
import Button from 'primevue/button'

const progressStore = useProgressStore()
const authStore = useAuthStore()
const fileInput = ref<HTMLInputElement | null>(null)

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
