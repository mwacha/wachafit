import api from './api'
import type { Exercise } from '@/types/api'

export const exerciseService = {
  search: (params?: { q?: string; muscleGroup?: string }) =>
    api.get<Exercise[]>('/api/exercises', { params }).then(r => r.data),
  create: (data: { name: string; muscleGroup: string; description?: string; videoUrl?: string }) =>
    api.post<Exercise>('/api/exercises', data).then(r => r.data),
  update: (id: string, data: { name: string; muscleGroup: string; description?: string; videoUrl?: string }) =>
    api.put<Exercise>(`/api/exercises/${id}`, data).then(r => r.data),
  deactivate: (id: string) => api.patch(`/api/exercises/${id}/deactivate`),
}
