import api from './api'
import type { GroupClass } from '@/types/api'

interface ClassPayload {
  name: string
  description?: string
  capacity: number
  scheduleType: string
  durationMinutes?: number
  startTime?: string | null
  endTime?: string | null
}

export const groupClassService = {
  list: () => api.get<GroupClass[]>('/api/classes').then(r => r.data),
  create: (data: ClassPayload & { trainerId: string }) =>
    api.post<GroupClass>('/api/classes', data).then(r => r.data),
  update: (id: string, data: ClassPayload) =>
    api.put<GroupClass>(`/api/classes/${id}`, data).then(r => r.data),
  deactivate: (id: string) => api.delete(`/api/classes/${id}`),
}
