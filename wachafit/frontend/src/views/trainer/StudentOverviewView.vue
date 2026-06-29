<!-- frontend/src/views/trainer/StudentOverviewView.vue -->
<template>
  <AppLayout>
    <div class="p-6 max-w-4xl">
      <Button icon="pi pi-arrow-left" text label="Voltar" @click="$router.back()" class="mb-4" />
      <h1 class="text-2xl font-bold mb-6">Visão do Aluno</h1>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <!-- Assessments -->
        <div class="card">
          <div class="flex justify-between items-center mb-3">
            <h2 class="text-lg font-semibold">Avaliações</h2>
            <Button icon="pi pi-plus" text size="small" @click="showAssessment = true" />
          </div>
          <div v-if="assessmentStore.assessments.length === 0" class="text-surface-400 text-sm">Nenhuma avaliação.</div>
          <div v-for="a in assessmentStore.assessments" :key="a.id" class="text-sm py-1 border-b">
            {{ a.assessedAt }} — Peso: {{ a.weightKg ?? '—' }} kg, BF: {{ a.bodyFatPct ?? '—' }}%
          </div>
        </div>

        <!-- Goals -->
        <div class="card">
          <div class="flex justify-between items-center mb-3">
            <h2 class="text-lg font-semibold">Metas</h2>
            <Button icon="pi pi-plus" text size="small" @click="showGoal = true" />
          </div>
          <div v-if="goals.length === 0" class="text-surface-400 text-sm">Nenhuma meta.</div>
          <div v-for="g in goals" :key="g.id" class="text-sm py-1 border-b flex justify-between">
            <span>{{ g.description }}</span>
            <Tag :severity="goalSeverity(g.status)" :value="g.status" />
          </div>
        </div>
      </div>

      <!-- New Assessment Dialog -->
      <Dialog v-model:visible="showAssessment" header="Nova Avaliação" :modal="true" style="width: 440px">
        <form @submit.prevent="submitAssessment" class="flex flex-col gap-3">
          <InputNumber v-model="aForm.weightKg" placeholder="Peso (kg)" :minFractionDigits="1" />
          <InputNumber v-model="aForm.heightCm" placeholder="Altura (cm)" :minFractionDigits="1" />
          <InputNumber v-model="aForm.bodyFatPct" placeholder="% gordura" :minFractionDigits="1" />
          <Textarea v-model="aForm.notes" placeholder="Notas" rows="2" />
          <Button type="submit" label="Salvar" :loading="saving" />
        </form>
      </Dialog>

      <!-- New Goal Dialog -->
      <Dialog v-model:visible="showGoal" header="Nova Meta" :modal="true" style="width: 400px">
        <form @submit.prevent="submitGoal" class="flex flex-col gap-3">
          <InputText v-model="gForm.description" placeholder="Descrição" required />
          <InputText v-model="gForm.metric" placeholder="Métrica (ex: weight)" />
          <Button type="submit" label="Salvar" :loading="saving" />
        </form>
      </Dialog>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import AppLayout from '@/components/AppLayout.vue'
import { useAssessmentStore } from '@/stores/assessment.store'
import { assessmentService } from '@/services/assessment.service'
import { goalService } from '@/services/goal.service'
import type { Goal } from '@/types/api'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Textarea from 'primevue/textarea'
import Tag from 'primevue/tag'

const route = useRoute()
const studentId = route.params.id as string
const assessmentStore = useAssessmentStore()
const goals = ref<Goal[]>([])
const showAssessment = ref(false)
const showGoal = ref(false)
const saving = ref(false)
const aForm = ref({ weightKg: null as number | null, heightCm: null as number | null, bodyFatPct: null as number | null, notes: '' })
const gForm = ref({ description: '', metric: '' })

onMounted(async () => {
  await assessmentStore.fetchAssessments(studentId)
  goals.value = await goalService.list(studentId)
})

function goalSeverity(status: string) {
  return status === 'ACHIEVED' ? 'success' : status === 'EXPIRED' ? 'danger' : 'info'
}

async function submitAssessment() {
  saving.value = true
  try {
    await assessmentService.create(studentId, {
      assessedAt: new Date().toISOString().split('T')[0],
      weightKg: aForm.value.weightKg ?? undefined,
      heightCm: aForm.value.heightCm ?? undefined,
      bodyFatPct: aForm.value.bodyFatPct ?? undefined,
      notes: aForm.value.notes || undefined,
      measurements: [],
    } as any)
    showAssessment.value = false
    await assessmentStore.fetchAssessments(studentId)
  } finally { saving.value = false }
}

async function submitGoal() {
  saving.value = true
  try {
    const g = await goalService.create(studentId, { description: gForm.value.description, metric: gForm.value.metric || undefined })
    goals.value.unshift(g)
    showGoal.value = false
  } finally { saving.value = false }
}
</script>
