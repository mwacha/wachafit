import { defineStore } from 'pinia'
import { ref } from 'vue'
import { scheduleService } from '@/services/schedule.service'
import type { Schedule } from '@/types/api'

export const useScheduleStore = defineStore('schedule', () => {
  const schedules = ref<Schedule[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchSchedules(params?: { date?: string; from?: string; to?: string; type?: string; trainerId?: string }) {
    loading.value = true; error.value = null
    try { schedules.value = await scheduleService.list(params) }
    catch (e: any) { error.value = e.response?.data?.message ?? 'Erro ao carregar horários' }
    finally { loading.value = false }
  }

  async function createSchedule(data: Parameters<typeof scheduleService.create>[0]) {
    const s = await scheduleService.create(data)
    schedules.value.unshift(s)
    return s
  }

  async function cancelSchedule(id: string) {
    await scheduleService.cancel(id)
    schedules.value = schedules.value.filter(s => s.id !== id)
  }

  return { schedules, loading, error, fetchSchedules, createSchedule, cancelSchedule }
})
