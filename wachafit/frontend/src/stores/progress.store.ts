import { defineStore } from 'pinia'
import { ref } from 'vue'
import { progressService } from '@/services/progress.service'
import type { Photo } from '@/types/api'

export const useProgressStore = defineStore('progress', () => {
  const photos = ref<Photo[]>([])
  const loading = ref(false)

  async function fetchPhotos(studentId: string) {
    loading.value = true
    try { photos.value = await progressService.list(studentId) }
    finally { loading.value = false }
  }

  async function uploadPhoto(studentId: string, file: File, takenAt?: string, notes?: string) {
    const photo = await progressService.upload(studentId, file, takenAt, notes)
    photos.value.unshift(photo)
  }

  async function deletePhoto(id: string) {
    await progressService.delete(id)
    photos.value = photos.value.filter(p => p.id !== id)
  }

  return { photos, loading, fetchPhotos, uploadPhoto, deletePhoto }
})
