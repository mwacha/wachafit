import api from './api'
import type { AdminUser } from '@/types/api'

export const userService = {
  list: (params?: { role?: string; active?: boolean }) =>
    api.get<AdminUser[]>('/api/admin/users', { params }).then(r => r.data),
  create: (data: { name: string; email: string; password: string; role: string }) =>
    api.post<AdminUser>('/api/admin/users', data).then(r => r.data),
  update: (id: string, data: { name: string; role: string }) =>
    api.patch<AdminUser>(`/api/admin/users/${id}`, data).then(r => r.data),
  deactivate: (id: string) => api.delete(`/api/admin/users/${id}`),
  activate: (id: string) => api.patch(`/api/admin/users/${id}/activate`),
}
