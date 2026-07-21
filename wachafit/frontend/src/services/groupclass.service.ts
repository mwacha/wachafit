import api from './api'
import type { EnrolledClass, EnrolledStudent, GroupClass } from '@/types/api'

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
  myEnrollments: (): Promise<EnrolledClass[]> =>
    api.get('/api/classes/my-enrollments').then(r => r.data),
  create: (data: ClassPayload & { trainerId: string }) =>
    api.post<GroupClass>('/api/classes', data).then(r => r.data),
  update: (id: string, data: ClassPayload) =>
    api.put<GroupClass>(`/api/classes/${id}`, data).then(r => r.data),
  deactivate: (id: string) => api.delete(`/api/classes/${id}`),
  reactivate: (id: string): Promise<GroupClass> =>
    api.put(`/api/classes/${id}/reactivate`).then(r => r.data),
  listEnrolled: (classId: string): Promise<EnrolledStudent[]> =>
    api.get(`/api/classes/${classId}/enrolled`).then(r => r.data),
  enrollStudent: (classId: string, studentId: string): Promise<void> =>
    api.post(`/api/classes/${classId}/enrolled`, { studentId }),
  unenrollStudent: (classId: string, studentId: string): Promise<void> =>
    api.delete(`/api/classes/${classId}/enrolled/${studentId}`),
}
