import api from './api'
import type { WorkoutPlan, WorkoutLog, PersonalRecord, ProgressionPoint } from '@/types/api'

export const workoutService = {
  listPlans: (studentId: string) =>
    api.get<WorkoutPlan[]>(`/api/students/${studentId}/workout-plans`).then(r => r.data),
  getActivePlan: (studentId: string) =>
    api.get<WorkoutPlan>(`/api/students/${studentId}/workout-plans/active`).then(r => r.data),
  createPlan: (studentId: string, data: unknown) =>
    api.post<WorkoutPlan>(`/api/students/${studentId}/workout-plans`, data).then(r => r.data),
  activatePlan: (planId: string) =>
    api.patch<WorkoutPlan>(`/api/workout-plans/${planId}/activate`).then(r => r.data),
  createLog: (studentId: string, data: { exerciseId: string; performedAt: string; sets?: number; reps?: number; loadKg?: number; notes?: string }) =>
    api.post<WorkoutLog>(`/api/students/${studentId}/workout-logs`, data).then(r => r.data),
  listLogs: (studentId: string) =>
    api.get<WorkoutLog[]>(`/api/students/${studentId}/workout-logs`).then(r => r.data),
  listRecords: (studentId: string) =>
    api.get<PersonalRecord[]>(`/api/students/${studentId}/records`).then(r => r.data),
  progression: (studentId: string, exerciseId: string) =>
    api.get<ProgressionPoint[]>(`/api/students/${studentId}/exercises/${exerciseId}/progression`).then(r => r.data),
}
