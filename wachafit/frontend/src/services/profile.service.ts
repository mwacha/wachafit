import api from './api'
import type { TrainerProfile, UpdateTrainerProfileRequest } from '@/types/api'

export default {
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
