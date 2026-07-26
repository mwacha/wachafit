<!-- frontend/src/views/auth/components/TenantSelectModal.vue -->
<template>
  <Dialog
    :visible="true"
    modal
    :closable="false"
    header="Escolha a academia"
    :style="{ width: '380px' }"
  >
    <p class="modal-hint">Sua conta está associada a mais de uma academia. Escolha qual você quer acessar agora.</p>

    <div class="tenant-list">
      <button
        v-for="m in memberships" :key="m.tenantId"
        type="button"
        class="tenant-option"
        :class="{ selected: selectedTenantId === m.tenantId }"
        @click="selectedTenantId = m.tenantId"
      >
        <span class="tenant-name">{{ m.tenantName }}</span>
        <span class="tenant-role">{{ m.role }}</span>
      </button>
    </div>

    <div v-if="errorMessage" class="error-banner" role="alert">
      <i class="pi pi-exclamation-circle" />
      {{ errorMessage }}
    </div>

    <Button label="Entrar" :loading="loading" class="submit-btn" :disabled="!selectedTenantId" @click="confirm" />
  </Dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import type { TenantMembershipSummary } from '@/types/api'

const props = defineProps<{
  selectTenantToken: string
  memberships: TenantMembershipSummary[]
}>()

const emit = defineEmits<{ selected: [role: string] }>()

const selectedTenantId = ref('')
const errorMessage = ref('')
const loading = ref(false)

import { useAuthStore } from '@/stores/auth.store'
const auth = useAuthStore()

async function confirm() {
  if (!selectedTenantId.value) return
  errorMessage.value = ''
  loading.value = true
  try {
    const result = await auth.selectTenant(props.selectTenantToken, selectedTenantId.value)
    emit('selected', result.role)
  } catch (err: any) {
    errorMessage.value = err.response?.data?.message ?? 'Erro ao selecionar academia. Tente novamente.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.modal-hint {
  font-size: 13px;
  color: var(--neutral-600);
  margin-bottom: 16px;
}
.tenant-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}
.tenant-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  border: 1px solid #ddd;
  border-radius: var(--radius-md);
  background: #fff;
  cursor: pointer;
  text-align: left;
}
.tenant-option.selected {
  border-color: var(--blue-500);
  box-shadow: 0 0 0 2px var(--blue-500) inset;
}
.tenant-name { font-weight: 600; font-size: 14px; }
.tenant-role { font-size: 12px; color: var(--neutral-600); }
.error-banner {
  display: flex; align-items: center; gap: 8px;
  background: var(--error-bg); border: 1px solid #FECACA;
  border-radius: var(--radius-md); color: var(--error-text);
  font-size: 13px; font-weight: 500; padding: 10px 14px; margin-bottom: 12px;
}
.error-banner .pi { font-size: 14px; }
.submit-btn { width: 100% !important; justify-content: center; }
</style>
