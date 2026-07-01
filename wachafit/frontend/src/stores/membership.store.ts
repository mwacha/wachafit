import { defineStore } from 'pinia'
import membershipService from '@/services/membership.service'
import type { MembershipPlan, MemberSubscription } from '@/types/api'

export const useMembershipStore = defineStore('membership', {
  state: () => ({
    plans: [] as MembershipPlan[],
    subscription: null as MemberSubscription | null,
    loading: false,
    error: null as string | null,
  }),
  actions: {
    async fetchPlans() {
      this.loading = true
      try { this.plans = await membershipService.listPlans() }
      catch (e: any) { this.error = e.message }
      finally { this.loading = false }
    },
    async fetchSubscription(studentId: string) {
      this.loading = true
      try { this.subscription = await membershipService.getSubscription(studentId) }
      catch (e: any) { this.error = e.message }
      finally { this.loading = false }
    },
  },
})
