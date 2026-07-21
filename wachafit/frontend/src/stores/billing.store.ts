import { defineStore } from 'pinia'
import billingService from '@/services/billing.service'
import type { PaymentCharge } from '@/types/api'

export const useBillingStore = defineStore('billing', {
  state: () => ({
    charges: [] as PaymentCharge[],
    loading: false,
    error: null as string | null,
    hasOverduePayment: false,
  }),
  actions: {
    async fetchCharges(studentId: string) {
      this.loading = true
      try { this.charges = await billingService.listCharges(studentId) }
      catch (e: any) { this.error = e.message }
      finally { this.loading = false }
    },
    async fetchPaymentStatus() {
      try {
        const res = await billingService.myPaymentStatus()
        this.hasOverduePayment = res.hasOverdue
      } catch {
        this.hasOverduePayment = false
      }
    },
  },
})
