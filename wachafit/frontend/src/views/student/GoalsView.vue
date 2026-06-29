<!-- frontend/src/views/student/GoalsView.vue -->
<template>
  <AppLayout>
    <div class="p-6">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold">Minhas Metas</h1>
        <Button label="Nova meta" icon="pi pi-plus" @click="showCreate = true" />
      </div>

      <div v-if="loading" class="text-center py-8">Carregando...</div>
      <div v-else class="grid gap-3">
        <div v-for="g in goals" :key="g.id" class="card p-4 flex items-center justify-between">
          <div>
            <div class="font-medium">{{ g.description }}</div>
            <div class="text-sm text-surface-500">
              {{ g.metric ?? '' }} {{ g.targetValue ? `— Alvo: ${g.targetValue}` : '' }}
              {{ g.targetDate ? ` até ${g.targetDate}` : '' }}
            </div>
          </div>
          <Tag :severity="goalSeverity(g.status)" :value="g.status" />
        </div>
        <div v-if="goals.length === 0" class="text-surface-400 text-sm">Nenhuma meta registrada.</div>
      </div>

      <Dialog v-model:visible="showCreate" header="Nova Meta" :modal="true" style="width: 400px">
        <form @submit.prevent="submitCreate" class="flex flex-col gap-3">
          <InputText v-model="form.description" placeholder="Descrição" required />
          <InputText v-model="form.metric" placeholder="Métrica (ex: weight, body_fat)" />
          <InputNumber v-model="form.targetValue" placeholder="Valor alvo" :minFractionDigits="1" />
          <Button type="submit" label="Salvar" :loading="saving" />
        </form>
      </Dialog>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { goalService } from '@/services/goal.service'
import { useAuthStore } from '@/stores/auth.store'
import type { Goal } from '@/types/api'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Tag from 'primevue/tag'

const authStore = useAuthStore()
const goals = ref<Goal[]>([])
const loading = ref(false)
const showCreate = ref(false)
const saving = ref(false)
const form = ref({ description: '', metric: '', targetValue: null as number | null })

onMounted(async () => {
  loading.value = true
  try { goals.value = await goalService.list(authStore.userId!) }
  finally { loading.value = false }
})

function goalSeverity(status: string) {
  return status === 'ACHIEVED' ? 'success' : status === 'EXPIRED' ? 'danger' : 'info'
}

async function submitCreate() {
  saving.value = true
  try {
    const g = await goalService.create(authStore.userId!, {
      description: form.value.description,
      metric: form.value.metric || undefined,
      targetValue: form.value.targetValue ?? undefined,
    })
    goals.value.unshift(g)
    showCreate.value = false
    form.value = { description: '', metric: '', targetValue: null }
  } finally { saving.value = false }
}
</script>
