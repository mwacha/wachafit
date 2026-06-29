import api from './api'
import type { Goal } from '@/types/api'

export const goalService = {
  list: (studentId: string) =>
    api.get<Goal[]>(`/api/students/${studentId}/goals`).then(r => r.data),
  create: (studentId: string, data: { description: string; metric?: string; targetValue?: number; targetDate?: string }) =>
    api.post<Goal>(`/api/students/${studentId}/goals`, data).then(r => r.data),
  updateStatus: (id: string, status: string) =>
    api.patch<Goal>(`/api/goals/${id}/status`, { status }).then(r => r.data),
}
