<!-- frontend/src/views/student/GoalsView.vue -->
<template>
  <AppLayout>
    <div class="view-wrap">
      <div class="page-header">
        <h1 class="page-title">Minhas Metas</h1>
        <Button label="Nova meta" icon="pi pi-plus" @click="showCreate = true" />
      </div>

      <div v-if="loading" class="empty-state"><i class="pi pi-spin pi-spinner" /></div>
      <div v-else class="goals-list">
        <div v-for="g in goals" :key="g.id" class="goal-card">
          <div class="goal-info">
            <div class="goal-desc">{{ g.description }}</div>
            <div class="goal-meta">
              {{ g.metric ?? '' }}{{ g.targetValue ? ` — Alvo: ${g.targetValue}` : '' }}{{ g.targetDate ? ` até ${g.targetDate}` : '' }}
            </div>
          </div>
          <div class="goal-actions">
            <Tag :severity="goalSeverity(g.status)" :value="goalLabel(g.status)" />
            <Button icon="pi pi-ellipsis-v" text rounded size="small" @click="(e) => toggleMenu(e, g.id)" />
            <Menu :ref="el => menuRefs[g.id] = el as any" :model="menuItems(g)" :popup="true" />
          </div>
        </div>
        <div v-if="goals.length === 0" class="empty-state">Nenhuma meta registrada.</div>
      </div>

      <p v-if="successMsg" class="success-msg">{{ successMsg }}</p>

      <Dialog v-model:visible="showCreate" header="Nova Meta" :modal="true" style="width: min(420px, 95vw)">
        <form @submit.prevent="submitCreate" class="flex flex-col gap-3 pt-2">
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
import type { Goal, GoalStatus } from '@/types/api'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Tag from 'primevue/tag'
import Menu from 'primevue/menu'

const authStore = useAuthStore()
const goals = ref<Goal[]>([])
const loading = ref(false)
const showCreate = ref(false)
const saving = ref(false)
const form = ref({ description: '', metric: '', targetValue: null as number | null })
const menuRefs = ref<Record<string, any>>({})

const successMsg = ref('')
function showSuccess(msg: string) {
  successMsg.value = msg
  setTimeout(() => { successMsg.value = '' }, 3000)
}

onMounted(async () => {
  loading.value = true
  try { goals.value = await goalService.list(authStore.userId!) }
  finally { loading.value = false }
})

function goalSeverity(status: string) {
  return status === 'ACHIEVED' ? 'success' : status === 'EXPIRED' ? 'danger' : 'info'
}
function goalLabel(status: string) {
  return status === 'ACHIEVED' ? 'Atingida' : status === 'EXPIRED' ? 'Expirada' : 'Em andamento'
}

function toggleMenu(event: Event, id: string) {
  menuRefs.value[id]?.toggle(event)
}

function menuItems(goal: Goal) {
  const statuses: { label: string; status: GoalStatus }[] = [
    { label: 'Em andamento', status: 'IN_PROGRESS' },
    { label: 'Atingida', status: 'ACHIEVED' },
    { label: 'Expirada', status: 'EXPIRED' },
  ]
  return statuses
    .filter(s => s.status !== goal.status)
    .map(s => ({
      label: s.label,
      command: () => changeStatus(goal.id, s.status),
    }))
}

async function changeStatus(id: string, status: GoalStatus) {
  const updated = await goalService.updateStatus(id, status)
  const idx = goals.value.findIndex(g => g.id === id)
  if (idx !== -1) goals.value[idx] = updated
  showSuccess('Status atualizado.')
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
    showSuccess('Meta criada.')
  } finally { saving.value = false }
}
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 20px; max-width: 680px; }
.page-header { display: flex; align-items: center; justify-content: space-between; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }
.goals-list { display: flex; flex-direction: column; gap: 10px; }
.goal-card {
  background: #fff; border: 1px solid var(--neutral-200);
  border-radius: var(--radius-lg); padding: 14px 16px;
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  box-shadow: var(--shadow-card);
}
.goal-info { flex: 1; min-width: 0; }
.goal-desc { font-weight: 600; color: var(--neutral-900); font-size: 14px; }
.goal-meta { font-size: 12px; color: var(--neutral-500); margin-top: 2px; }
.goal-actions { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.empty-state { text-align: center; padding: 40px; color: var(--neutral-400); font-size: 14px; }
.success-msg { color: #22c55e; font-size: 0.875rem; margin-top: 0; }
</style>
