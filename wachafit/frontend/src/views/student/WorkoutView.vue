<!-- frontend/src/views/student/WorkoutView.vue -->
<template>
  <AppLayout>
    <div class="p-6 max-w-3xl">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold">Meu Treino</h1>
        <Button
          v-if="workoutStore.activePlan"
          icon="pi pi-download"
          label="Baixar Ficha"
          class="p-button-outlined"
          :loading="downloadingPdf"
          @click="downloadWorkoutPdf"
        />
      </div>

      <div v-if="workoutStore.loading" class="text-center py-8">Carregando...</div>
      <div v-else-if="!workoutStore.activePlan" class="text-surface-400">Nenhuma ficha ativa.</div>
      <div v-else>
        <h2 class="text-lg font-semibold mb-3">{{ workoutStore.activePlan.name }}</h2>
        <div v-for="item in workoutStore.activePlan.items" :key="item.id"
             class="card p-4 mb-3 flex items-center justify-between">
          <div>
            <div class="font-medium">{{ exerciseNames[item.exerciseId] ?? 'Exercício' }}</div>
            <div class="text-sm text-surface-500">
              {{ item.division ? `Divisão ${item.division} — ` : '' }}
              {{ item.sets }}x{{ item.reps }}
              {{ item.suggestedLoadKg ? ` @ ${item.suggestedLoadKg}kg` : '' }}
            </div>
          </div>
          <Button icon="pi pi-plus" text size="small" label="Registrar" @click="openLog(item)" />
        </div>
      </div>

      <Dialog v-model:visible="showLog" header="Registrar Execução" :modal="true" style="width: min(380px, 95vw)">
        <form @submit.prevent="submitLog" class="flex flex-col gap-3">
          <InputNumber v-model="logForm.sets" placeholder="Séries" :min="1" />
          <InputNumber v-model="logForm.reps" placeholder="Repetições" :min="1" />
          <InputNumber v-model="logForm.loadKg" placeholder="Carga (kg)" :minFractionDigits="1" />
          <InputText v-model="logForm.notes" placeholder="Notas (opcional)" />
          <p v-if="logError" class="text-red-500 text-sm">{{ logError }}</p>
          <Button type="submit" label="Salvar" :loading="saving" />
        </form>
      </Dialog>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useWorkoutStore } from '@/stores/workout.store'
import { useAuthStore } from '@/stores/auth.store'
import { workoutService } from '@/services/workout.service'
import { exerciseService } from '@/services/exercise.service'
import type { WorkoutPlanItem } from '@/types/api'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import api from '@/services/api'

const workoutStore = useWorkoutStore()
const authStore = useAuthStore()
const downloadingPdf = ref(false)
const showLog = ref(false)
const saving = ref(false)
const logError = ref<string | null>(null)
const currentItem = ref<WorkoutPlanItem | null>(null)
const logForm = ref({ sets: null as number | null, reps: null as number | null, loadKg: null as number | null, notes: '' })
const exerciseNames = ref<Record<string, string>>({})

onMounted(async () => {
  await workoutStore.fetchActivePlan(authStore.userId!)
  const exercises = await exerciseService.search()
  exerciseNames.value = Object.fromEntries(exercises.map(e => [e.id, e.name]))
})

async function downloadWorkoutPdf() {
  if (!authStore.userId) return
  downloadingPdf.value = true
  try {
    const res = await api.get(`/api/students/${authStore.userId}/pdf/workout`, { responseType: 'blob' })
    const url = URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }))
    const a = document.createElement('a')
    a.href = url
    a.download = 'ficha-treino.pdf'
    a.click()
    URL.revokeObjectURL(url)
  } finally {
    downloadingPdf.value = false
  }
}

function openLog(item: WorkoutPlanItem) { currentItem.value = item; showLog.value = true }

async function submitLog() {
  if (!currentItem.value) return
  saving.value = true; logError.value = null
  try {
    await workoutService.createLog(authStore.userId!, {
      exerciseId: currentItem.value.exerciseId,
      performedAt: new Date().toISOString().split('T')[0],
      sets: logForm.value.sets ?? undefined,
      reps: logForm.value.reps ?? undefined,
      loadKg: logForm.value.loadKg ?? undefined,
      notes: logForm.value.notes || undefined,
    })
    showLog.value = false
    logForm.value = { sets: null, reps: null, loadKg: null, notes: '' }
  } catch (e: any) {
    logError.value = e.response?.data?.message ?? 'Erro ao registrar'
  } finally { saving.value = false }
}
</script>
