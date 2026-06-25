<template>
  <div class="flex items-center justify-center min-h-screen bg-surface-50">
    <div class="w-full max-w-md p-8 bg-white rounded-xl shadow-md">
      <h1 class="text-2xl font-bold mb-6 text-center">Nova senha</h1>

      <div class="flex flex-col gap-4">
        <Password v-model="newPassword" toggleMask :feedback="false" placeholder="Nova senha (mín. 8 caracteres)" />
        <Password v-model="confirmPassword" toggleMask :feedback="false"
          placeholder="Confirmar nova senha" :invalid="passwordMismatch" />
        <small v-if="passwordMismatch" class="text-red-500">Senhas não coincidem.</small>

        <Message v-if="successMessage" severity="success" :closable="false">{{ successMessage }}</Message>
        <Message v-if="errorMessage" severity="error" :closable="false">{{ errorMessage }}</Message>

        <Button label="Redefinir senha" :loading="loading" @click="handleReset" class="w-full" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/services/api'
import Password from 'primevue/password'
import Button from 'primevue/button'
import Message from 'primevue/message'

const route = useRoute()
const router = useRouter()
const newPassword = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const successMessage = ref('')
const errorMessage = ref('')

const passwordMismatch = computed(() =>
  confirmPassword.value.length > 0 && newPassword.value !== confirmPassword.value
)

async function handleReset() {
  successMessage.value = ''
  errorMessage.value = ''
  if (newPassword.value !== confirmPassword.value) {
    errorMessage.value = 'Senhas não coincidem.'
    return
  }
  loading.value = true
  try {
    await api.post('/api/auth/reset-password', {
      token: route.query.token as string,
      newPassword: newPassword.value,
    })
    successMessage.value = 'Senha redefinida com sucesso! Redirecionando...'
    setTimeout(() => router.push('/login'), 2000)
  } catch (err: any) {
    errorMessage.value = err.response?.data?.message ?? 'Erro ao redefinir senha.'
  } finally {
    loading.value = false
  }
}
</script>
