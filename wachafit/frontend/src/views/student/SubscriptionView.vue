<template>
  <AppLayout>
    <div class="p-6 max-w-2xl">
      <h1 class="text-2xl font-bold mb-6">Meu Plano</h1>

      <div v-if="loading" class="text-center py-8">Carregando...</div>

      <div v-else-if="!subscription" class="card p-6 text-center text-surface-400">
        <i class="pi pi-id-card" style="font-size:2.5rem; margin-bottom:12px; display:block" />
        <p>Você não possui assinatura ativa.</p>
        <p style="font-size:13px; margin-top:4px">Fale com a recepção para se matricular.</p>
      </div>

      <div v-else class="card p-6">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-lg font-semibold">{{ subscription.planName }}</h2>
          <Tag :value="statusLabel(subscription.status)" :severity="statusSeverity(subscription.status)" />
        </div>
        <div style="display:grid; grid-template-columns:1fr 1fr; gap:16px; font-size:14px">
          <div>
            <p style="color:#6b7280; margin-bottom:4px">Início</p>
            <p class="font-medium">{{ formatDate(subscription.startedAt) }}</p>
          </div>
          <div>
            <p style="color:#6b7280; margin-bottom:4px">Validade</p>
            <p class="font-medium">{{ formatDate(subscription.expiresAt) }}</p>
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
