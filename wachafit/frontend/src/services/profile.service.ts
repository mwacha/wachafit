import api from './api'
import type { StudentProfile, StudentHealth, TrainerProfile, UpdateTrainerProfileRequest } from '@/types/api'

export default {
  // ---- Student profile ----
  async getStudentProfile(studentId: string): Promise<StudentProfile | null> {
    try {
      const res = await api.get(`/api/students/${studentId}/profile`)
      return res.data
    } catch (e: any) {
      if (e.response?.status === 404) return null
      throw e
    }
  },

  async createStudentProfile(studentId: string, data: Partial<StudentProfile>): Promise<StudentProfile> {
    const res = await api.post(`/api/students/${studentId}/profile`, data)
    return res.data
  },

  async updateStudentProfile(studentId: string, data: Partial<StudentProfile>): Promise<StudentProfile> {
    const res = await api.put(`/api/students/${studentId}/profile`, data)
    return res.data
  },

  // ---- Student health ----
  async getStudentHealth(studentId: string): Promise<StudentHealth | null> {
    try {
      const res = await api.get(`/api/students/${studentId}/health`)
      return res.data
    } catch (e: any) {
      if (e.response?.status === 404) return null
      throw e
    }
  },

  async upsertStudentHealth(studentId: string, data: Partial<StudentHealth>): Promise<StudentHealth> {
    const res = await api.post(`/api/students/${studentId}/health`, data)
    return res.data
  },

  // ---- Trainer profile ----
  async getTrainerProfile(userId: string): Promise<TrainerProfile | null> {
    try {
      const res = await api.get(`/api/trainers/${userId}/profile`)
      return res.data
    } catch (e: any) {
      if (e.response?.status === 404) return null
      throw e
    }
  },

  async updateTrainerProfile(userId: string, data: UpdateTrainerProfileRequest): Promise<TrainerProfile> {
    const res = await api.put(`/api/trainers/${userId}/profile`, data)
    return res.data
  },
}
