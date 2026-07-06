import api from './api'
import type { Schedule } from '@/types/api'

export const scheduleService = {
  list: (params?: { trainerId?: string; date?: string; from?: string; to?: string; type?: string }) =>
    api.get<Schedule[]>('/api/schedules', { params }).then(r => r.data),
  create: (data: { groupClassId?: string; trainerId: string; type: string; startsAt: string; endsAt: string }) =>
    api.post<Schedule>('/api/schedules', data).then(r => r.data),
  cancel: (id: string) => api.delete(`/api/schedules/${id}`),
}
