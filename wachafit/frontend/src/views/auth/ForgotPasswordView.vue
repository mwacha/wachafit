<template>
  <div class="flex items-center justify-center min-h-screen bg-surface-50">
    <div class="w-full max-w-md p-8 bg-white rounded-xl shadow-md">
      <h1 class="text-2xl font-bold mb-2 text-center">Redefinir senha</h1>
      <p class="text-center text-sm text-surface-500 mb-6">
        Informe seu e-mail e enviaremos um link de redefinição.
      </p>

      <div class="flex flex-col gap-4">
        <InputText v-model="email" type="email" placeholder="seu@email.com" />

        <Message v-if="successMessage" severity="success" :closable="false">{{ successMessage }}</Message>
        <Message v-if="errorMessage" severity="error" :closable="false">{{ errorMessage }}</Message>

        <Button label="Enviar link" :loading="loading" @click="handleSubmit" class="w-full" />
        <RouterLink to="/login" class="text-center text-sm text-primary-500 hover:underline">
          Voltar ao login
        </RouterLink>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import api from '@/services/api'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import Message from 'primevue/message'

const email = ref('')
const loading = ref(false)
const successMessage = ref('')
const errorMessage = ref('')

async function handleSubmit() {
  successMessage.value = ''
  errorMessage.value = ''
  loading.value = true
  try {
    await api.post('/api/auth/forgot-password', { email: email.value })
    successMessage.value = 'Se o e-mail estiver cadastrado, você receberá o link em breve.'
  } catch {
    errorMessage.value = 'Erro ao processar solicitação.'
  } finally {
    loading.value = false
  }
}
</script>
