<template>
  <div class="flex items-center justify-center min-h-screen bg-surface-50">
    <div class="w-full max-w-md p-8 bg-white rounded-xl shadow-md">
      <h1 class="text-2xl font-bold mb-6 text-center">Criar conta</h1>

      <div class="flex flex-col gap-4">
        <div class="flex flex-col gap-1">
          <label class="text-sm font-medium">Nome completo</label>
          <InputText v-model="name" placeholder="Seu nome" />
        </div>

        <div class="flex flex-col gap-1">
          <label class="text-sm font-medium">E-mail</label>
          <InputText v-model="email" type="email" placeholder="seu@email.com" />
        </div>

        <div class="flex flex-col gap-1">
          <label class="text-sm font-medium">Senha</label>
          <Password v-model="password" toggleMask :feedback="false" placeholder="Mín. 8 caracteres" />
        </div>

        <div class="flex flex-col gap-1">
          <label class="text-sm font-medium">Confirmar senha</label>
          <Password v-model="confirmPassword" toggleMask :feedback="false" :invalid="passwordMismatch" />
          <small v-if="passwordMismatch" class="text-red-500">Senhas não coincidem.</small>
        </div>

        <Message v-if="errorMessage" severity="error" :closable="false">{{ errorMessage }}</Message>

        <Button label="Criar conta" :loading="loading" @click="handleRegister" class="w-full" />

        <p class="text-center text-sm text-surface-500">
          Já tem conta?
          <RouterLink to="/login" class="text-primary-500 hover:underline">Entrar</RouterLink>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import { roleDashboards } from '@/utils/roleRoutes'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Button from 'primevue/button'
import Message from 'primevue/message'

const auth = useAuthStore()
const router = useRouter()
const name = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const errorMessage = ref('')
const loading = ref(false)

const passwordMismatch = computed(() =>
  confirmPassword.value.length > 0 && password.value !== confirmPassword.value
)

async function handleRegister() {
  errorMessage.value = ''
  if (!name.value || !email.value || !password.value) {
    errorMessage.value = 'Preencha todos os campos.'
    return
  }
  if (password.value !== confirmPassword.value) {
    errorMessage.value = 'Senhas não coincidem.'
    return
  }
  if (password.value.length < 8) {
    errorMessage.value = 'Senha deve ter ao menos 8 caracteres.'
    return
  }
  loading.value = true
  try {
    const result = await auth.register(name.value, email.value, password.value)
    router.push(roleDashboards[result.role])
  } catch (err: any) {
    errorMessage.value = err.response?.data?.message ?? 'Erro ao criar conta. Tente novamente.'
  } finally {
    loading.value = false
  }
}
</script>
