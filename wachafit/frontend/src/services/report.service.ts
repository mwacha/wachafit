import api from './api'
import type { RevenueReport, OverdueStudent, SubscriptionStats, TrainerCommission, CashFlowDay, EnrollmentTrend, AttendanceRank } from '@/types/api'

export default {
  async getRevenue(from: string, to: string): Promise<RevenueReport[]> {
    const res = await api.get('/api/reports/revenue', { params: { from, to } })
    return res.data
  },

  async getOverdue(): Promise<OverdueStudent[]> {
    const res = await api.get('/api/reports/overdue')
    return res.data
  },

  async getSubscriptionStats(): Promise<SubscriptionStats> {
    const res = await api.get('/api/reports/subscriptions')
    return res.data
  },

  async getTrainerCommissions(from: string, to: string): Promise<TrainerCommission[]> {
    const res = await api.get('/api/reports/trainer-commissions', { params: { from, to } })
    return res.data
  },

  async getCashFlow(from: string, to: string): Promise<CashFlowDay[]> {
    const res = await api.get('/api/reports/cash-flow', { params: { from, to } })
    return res.data
  },

  async getEnrollmentTrend(months = 12): Promise<EnrollmentTrend[]> {
    const res = await api.get('/api/reports/enrollment-trend', { params: { months } })
    return res.data
  },

  async getAttendanceRanking(days = 30, limit = 10): Promise<AttendanceRank[]> {
    const res = await api.get('/api/reports/attendance-ranking', { params: { days, limit } })
    return res.data
  },
}
