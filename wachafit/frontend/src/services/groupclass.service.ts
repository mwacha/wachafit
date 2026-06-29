import api from './api'
import type { GroupClass } from '@/types/api'

export const groupClassService = {
  list: () => api.get<GroupClass[]>('/api/classes').then(r => r.data),
  create: (data: { name: string; description?: string; capacity: number; durationMinutes: number; trainerId: string }) =>
    api.post<GroupClass>('/api/classes', data).then(r => r.data),
  update: (id: string, data: { name: string; description?: string; capacity: number; durationMinutes: number }) =>
    api.put<GroupClass>(`/api/classes/${id}`, data).then(r => r.data),
  deactivate: (id: string) => api.delete(`/api/classes/${id}`),
}
