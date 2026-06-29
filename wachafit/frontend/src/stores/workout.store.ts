import { defineStore } from 'pinia'
import { ref } from 'vue'
import { workoutService } from '@/services/workout.service'
import type { WorkoutPlan, PersonalRecord } from '@/types/api'

export const useWorkoutStore = defineStore('workout', () => {
  const activePlan = ref<WorkoutPlan | null>(null)
  const plans = ref<WorkoutPlan[]>([])
  const records = ref<PersonalRecord[]>([])
  const loading = ref(false)

  async function fetchActivePlan(studentId: string) {
    loading.value = true
    try { activePlan.value = await workoutService.getActivePlan(studentId) }
    catch { activePlan.value = null }
    finally { loading.value = false }
  }

  async function fetchRecords(studentId: string) {
    loading.value = true
    try { records.value = await workoutService.listRecords(studentId) }
    finally { loading.value = false }
  }

  return { activePlan, plans, records, loading, fetchActivePlan, fetchRecords }
})
