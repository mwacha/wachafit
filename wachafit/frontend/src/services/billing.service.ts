import api from './api'
import type { PaymentCharge, CreateChargeRequest, ManualPaymentRequest } from '@/types/api'

export default {
  async listCharges(studentId: string): Promise<PaymentCharge[]> {
    const res = await api.get(`/api/students/${studentId}/charges`)
    return res.data
  },

  async createCharge(studentId: string, data: CreateChargeRequest): Promise<PaymentCharge> {
    const res = await api.post(`/api/students/${studentId}/charges`, data)
    return res.data
  },

  async payCharge(chargeId: string, data: ManualPaymentRequest): Promise<PaymentCharge> {
    const res = await api.patch(`/api/charges/${chargeId}/pay`, data)
    return res.data
  },

  async cancelCharge(chargeId: string): Promise<void> {
    await api.patch(`/api/charges/${chargeId}/cancel`)
  },
}
