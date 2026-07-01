import { defineStore } from 'pinia'
import billingService from '@/services/billing.service'
import type { PaymentCharge } from '@/types/api'

export const useBillingStore = defineStore('billing', {
  state: () => ({
    charges: [] as PaymentCharge[],
    loading: false,
    error: null as string | null,
  }),
  actions: {
    async fetchCharges(studentId: string) {
      this.loading = true
      try { this.charges = await billingService.listCharges(studentId) }
      catch (e: any) { this.error = e.message }
      finally { this.loading = false }
    },
  },
})
