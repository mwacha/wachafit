import { defineStore } from 'pinia'
import { ref } from 'vue'
import { assessmentService } from '@/services/assessment.service'
import type { Assessment, EvolutionPoint } from '@/types/api'

export const useAssessmentStore = defineStore('assessment', () => {
  const assessments = ref<Assessment[]>([])
  const evolution = ref<EvolutionPoint[]>([])
  const loading = ref(false)

  async function fetchAssessments(studentId: string) {
    loading.value = true
    try {
      assessments.value = await assessmentService.list(studentId)
      evolution.value = await assessmentService.evolution(studentId)
    } finally { loading.value = false }
  }

  return { assessments, evolution, loading, fetchAssessments }
})
