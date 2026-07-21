<template>
  <AppLayout>
    <div class="view-wrap">
      <h1 class="page-title">Meu Plano</h1>

      <div v-if="loading" class="empty-state">Carregando...</div>

      <div v-else-if="!subscription" class="sub-empty">
        <i class="pi pi-id-card sub-empty-icon" />
        <p>Você não possui assinatura ativa.</p>
        <p class="sub-empty-hint">Fale com a recepção para se matricular.</p>
      </div>

      <div v-else class="sub-card">
        <div class="sub-header">
          <h2 class="sub-plan-name">{{ subscription.planName }}</h2>
          <Tag :value="statusLabel(subscription.status)" :severity="statusSeverity(subscription.status)" />
        </div>
        <div class="sub-details">
          <div class="sub-detail">
            <p class="sub-detail-label">Início</p>
            <p class="sub-detail-value">{{ formatDate(subscription.startedAt) }}</p>
          </div>
          <div class="sub-detail">
            <p class="sub-detail-label">Validade</p>
            <p class="sub-detail-value">{{ formatDate(subscription.expiresAt) }}</p>
          </div>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import Tag from 'primevue/tag'
import { useAuthStore } from '@/stores/auth.store'
import membershipService from '@/services/membership.service'
import type { MemberSubscription } from '@/types/api'

const auth = useAuthStore()
const subscription = ref<MemberSubscription | null>(null)
const loading = ref(true)

onMounted(async () => {
  if (auth.userId) subscription.value = await membershipService.getSubscription(auth.userId)
  loading.value = false
})

function statusLabel(s: string) {
  return { ACTIVE: 'Ativo', SUSPENDED: 'Suspenso', CANCELLED: 'Cancelado' }[s] ?? s
}
function statusSeverity(s: string) {
  return { ACTIVE: 'success', SUSPENDED: 'warn', CANCELLED: 'danger' }[s] ?? 'secondary'
}
function formatDate(d: string) { return new Date(d).toLocaleDateString('pt-BR') }
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 20px; max-width: 600px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }
.empty-state { text-align: center; padding: 40px; color: var(--neutral-500); font-size: 14px; }
.sub-empty {
  background: #fff; border: 1px solid var(--neutral-200); border-radius: var(--radius-lg);
  padding: 48px 24px; display: flex; flex-direction: column; align-items: center;
  gap: 8px; text-align: center; color: var(--neutral-500);
}
.sub-empty-icon { font-size: 2.5rem; color: var(--neutral-300); }
.sub-empty-hint { font-size: 13px; }
.sub-card {
  background: #fff; border: 1px solid var(--neutral-200); border-radius: var(--radius-lg);
  padding: 24px; box-shadow: var(--shadow-card);
}
.sub-header {
  display: flex; align-items: center; justify-content: space-between;
  flex-wrap: wrap; gap: 10px; margin-bottom: 20px;
}
.sub-plan-name { font-size: 18px; font-weight: 600; color: var(--neutral-900); }
.sub-details { display: grid; grid-template-columns: repeat(auto-fit, minmax(120px, 1fr)); gap: 16px; }
.sub-detail-label { font-size: 13px; color: var(--neutral-500); margin-bottom: 4px; }
.sub-detail-value { font-size: 15px; font-weight: 600; color: var(--neutral-900); }
</style>
