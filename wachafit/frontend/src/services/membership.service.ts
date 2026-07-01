import api from './api'
import type { MembershipPlan, MemberSubscription, CreateSubscriptionRequest } from '@/types/api'

export default {
  async listPlans(): Promise<MembershipPlan[]> {
    const res = await api.get('/api/membership-plans')
    return res.data
  },

  async createPlan(data: Partial<MembershipPlan>): Promise<MembershipPlan> {
    const res = await api.post('/api/membership-plans', data)
    return res.data
  },

  async updatePlan(id: string, data: Partial<MembershipPlan>): Promise<MembershipPlan> {
    const res = await api.put(`/api/membership-plans/${id}`, data)
    return res.data
  },

  async deactivatePlan(id: string): Promise<void> {
    await api.delete(`/api/membership-plans/${id}`)
  },

  async getSubscription(studentId: string): Promise<MemberSubscription | null> {
    try {
      const res = await api.get(`/api/students/${studentId}/subscription`)
      return res.data
    } catch (e: any) {
      if (e.response?.status === 404) return null
      throw e
    }
  },

  async createSubscription(studentId: string, data: CreateSubscriptionRequest): Promise<MemberSubscription> {
    const res = await api.post(`/api/students/${studentId}/subscription`, data)
    return res.data
  },

  async cancelSubscription(studentId: string, reason: string): Promise<void> {
    await api.delete(`/api/students/${studentId}/subscription`, { data: { cancellationReason: reason } })
  },
}
