import api from './api'
import type { Assessment, EvolutionPoint } from '@/types/api'

export const assessmentService = {
  list: (studentId: string) =>
    api.get<Assessment[]>(`/api/students/${studentId}/assessments`).then(r => r.data),
  evolution: (studentId: string) =>
    api.get<EvolutionPoint[]>(`/api/students/${studentId}/assessments/evolution`).then(r => r.data),
  create: (studentId: string, data: Partial<Assessment> & { measurements?: { bodyPart: string; valueCm: number }[] }) =>
    api.post<Assessment>(`/api/students/${studentId}/assessments`, data).then(r => r.data),
  update: (id: string, data: unknown) =>
    api.put<Assessment>(`/api/assessments/${id}`, data).then(r => r.data),
}
