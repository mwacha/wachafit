import api from './api'
import type { Photo } from '@/types/api'

export const progressService = {
  list: (studentId: string) =>
    api.get<Photo[]>(`/api/students/${studentId}/photos`).then(r => r.data),
  upload: (studentId: string, file: File, takenAt?: string, notes?: string) => {
    const form = new FormData()
    form.append('file', file)
    if (takenAt) form.append('takenAt', takenAt)
    if (notes) form.append('notes', notes)
    return api.post<Photo>(`/api/students/${studentId}/photos`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(r => r.data)
  },
  delete: (id: string) => api.delete(`/api/photos/${id}`),
}
