<!-- frontend/src/views/student/PhotosView.vue -->
<template>
  <AppLayout>
    <div class="view-wrap">
      <div class="page-header">
        <h1 class="page-title">Fotos de Progresso</h1>
        <Button label="Adicionar foto" icon="pi pi-upload" @click="triggerFileInput" />
      </div>

      <!-- barra de ações de comparação -->
      <div v-if="selectedPhotos.length > 0" class="selection-bar">
        <span class="selection-label">
          {{ selectedPhotos.length === 1 ? '1 foto selecionada' : '2 fotos selecionadas' }}
        </span>
        <Button
          v-if="selectedPhotos.length === 2"
          label="Comparar"
          icon="pi pi-arrows-h"
          size="small"
          @click="showCompare = true"
        />
        <Button label="Cancelar" severity="secondary" text size="small" @click="clearSelection" />
      </div>

      <input ref="fileInput" type="file" accept="image/*" style="display:none" @change="handleFileSelect" />

      <div v-if="progressStore.loading" class="empty-state">Carregando...</div>
      <div v-else-if="progressStore.photos.length === 0" class="empty-state">
        Nenhuma foto registrada.
      </div>
      <div v-else class="photo-grid">
        <div
          v-for="photo in progressStore.photos"
          :key="photo.id"
          class="photo-card"
          :class="{ selected: isSelected(photo) }"
          @click="openZoom(photo)"
        >
          <AuthImg :src="photo.fileUrl" :alt="photo.notes ?? 'Foto'" class="photo-img" />
          <div class="photo-date">{{ formatDate(photo.takenAt) }}</div>
          <!-- botão de seleção para comparação -->
          <button
            class="select-btn"
            :class="{ 'select-btn--active': isSelected(photo) }"
            @click.stop="toggleSelect(photo)"
            :title="isSelected(photo) ? 'Remover da comparação' : 'Selecionar para comparar'"
          >
            <i :class="isSelected(photo) ? 'pi pi-check' : 'pi pi-circle'" />
          </button>
          <Button
            icon="pi pi-trash"
            severity="danger"
            text
            size="small"
            class="delete-btn"
            @click.stop="deletePhoto(photo.id)"
          />
        </div>
      </div>
    </div>

    <!-- Dialog de zoom -->
    <Dialog v-model:visible="showZoom" :modal="true" :header="zoomPhoto ? formatDate(zoomPhoto.takenAt) : ''" style="width: min(700px, 95vw)">
      <div class="zoom-wrap">
        <AuthImg v-if="zoomPhoto" :src="zoomPhoto.fileUrl" :alt="zoomPhoto.notes ?? 'Foto'" class="zoom-img" />
        <p v-if="zoomPhoto?.notes" class="zoom-notes">{{ zoomPhoto.notes }}</p>
      </div>
    </Dialog>

    <!-- Dialog de comparação lado a lado -->
    <Dialog v-model:visible="showCompare" header="Comparação de Fotos" :modal="true" style="width: min(860px, 95vw)">
      <div class="compare-grid">
        <div v-if="comparePhotos[0]" class="compare-col">
          <span class="compare-label">Antes</span>
          <span class="compare-date">{{ formatDate(comparePhotos[0].takenAt) }}</span>
          <AuthImg :src="comparePhotos[0].fileUrl" :alt="comparePhotos[0].notes ?? 'Antes'" class="compare-img" />
          <p v-if="comparePhotos[0].notes" class="compare-notes">{{ comparePhotos[0].notes }}</p>
        </div>
        <div v-if="comparePhotos[1]" class="compare-col">
          <span class="compare-label">Depois</span>
          <span class="compare-date">{{ formatDate(comparePhotos[1].takenAt) }}</span>
          <AuthImg :src="comparePhotos[1].fileUrl" :alt="comparePhotos[1].notes ?? 'Depois'" class="compare-img" />
          <p v-if="comparePhotos[1].notes" class="compare-notes">{{ comparePhotos[1].notes }}</p>
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
import AuthImg from '@/components/AuthImg.vue'
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
const showZoom = ref(false)
const zoomPhoto = ref<Photo | null>(null)

function openZoom(photo: Photo) { zoomPhoto.value = photo; showZoom.value = true }

function isSelected(photo: Photo) {
  return selectedPhotos.value.some(p => p.id === photo.id)
}

function toggleSelect(photo: Photo) {
  const idx = selectedPhotos.value.findIndex(p => p.id === photo.id)
  if (idx >= 0) selectedPhotos.value.splice(idx, 1)
  else if (selectedPhotos.value.length < 2) selectedPhotos.value.push(photo)
}

const comparePhotos = computed(() =>
  [...selectedPhotos.value].sort((a, b) =>
    new Date(a.takenAt).getTime() - new Date(b.takenAt).getTime()
  )
)

function clearSelection() { selectedPhotos.value = []; showCompare.value = false }

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

function formatDate(d: string) {
  return new Date(d).toLocaleDateString('pt-BR', { timeZone: 'UTC' })
}
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 16px; max-width: 100%; }
.page-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }

.selection-bar {
  display: flex; align-items: center; gap: 10px; flex-wrap: wrap;
  background: var(--blue-50); border-radius: var(--radius-md); padding: 10px 14px;
}
.selection-label { font-size: 13px; font-weight: 600; color: var(--blue-700); }

/* Grade responsiva */
.photo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
}

.photo-card {
  position: relative;
  cursor: pointer;
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: 2px solid transparent;
  transition: border-color .15s;
}
.photo-card.selected { border-color: var(--blue-500); }

.photo-img {
  width: 100%;
  aspect-ratio: 3 / 4;
  object-fit: cover;
  display: block;
}

.photo-date {
  position: absolute; bottom: 0; left: 0; right: 0;
  background: rgba(0,0,0,.5); color: #fff;
  font-size: 11px; padding: 4px 6px;
}

.check-badge {
  position: absolute; top: 6px; left: 6px;
  width: 22px; height: 22px; border-radius: 50%;
  background: var(--blue-500); color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 11px;
}

.select-btn {
  position: absolute; top: 6px; left: 6px;
  width: 24px; height: 24px; border-radius: 50%;
  border: 2px solid #fff; background: rgba(0,0,0,.35);
  color: #fff; font-size: 11px;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; opacity: 0; transition: opacity .15s;
  padding: 0;
}
.select-btn--active {
  background: var(--blue-500); border-color: var(--blue-500);
  opacity: 1 !important;
}
.photo-card:hover .select-btn { opacity: 1; }

.delete-btn {
  position: absolute; top: 4px; right: 4px;
  opacity: 0; transition: opacity .15s;
}
.photo-card:hover .delete-btn { opacity: 1; }

.empty-state { text-align: center; padding: 40px; color: var(--neutral-500); font-size: 14px; }

/* Dialog de zoom */
.zoom-wrap { display: flex; flex-direction: column; align-items: center; gap: 10px; }
.zoom-img { max-width: 100%; max-height: 75vh; object-fit: contain; border-radius: var(--radius-md); }
.zoom-notes { font-size: 13px; color: var(--neutral-500); margin: 0; text-align: center; }

/* Dialog de comparação */
.compare-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.compare-col { display: flex; flex-direction: column; gap: 6px; }
.compare-label { font-size: 11px; font-weight: 700; color: var(--neutral-500); text-transform: uppercase; letter-spacing: .05em; }
.compare-date { font-size: 13px; color: var(--neutral-600); }
.compare-img { width: 100%; max-height: 50vh; object-fit: contain; border-radius: var(--radius-md); background: var(--neutral-100); }
.compare-notes { font-size: 13px; color: var(--neutral-500); margin: 0; }
</style>
