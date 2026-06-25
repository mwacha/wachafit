<template>
  <div class="flex items-center justify-center min-h-screen bg-surface-50">
    <div class="w-full max-w-md p-8 bg-white rounded-xl shadow-md">
      <h1 class="text-2xl font-bold mb-6 text-center">Entrar</h1>

      <div class="flex flex-col gap-4">
        <div class="flex flex-col gap-1">
          <label class="text-sm font-medium">E-mail</label>
          <InputText v-model="email" type="email" placeholder="seu@email.com" :invalid="!!errorMessage" />
        </div>

        <div class="flex flex-col gap-1">
          <label class="text-sm font-medium">Senha</label>
          <Password v-model="password" :feedback="false" toggleMask :invalid="!!errorMessage" />
        </div>

        <Message v-if="errorMessage" severity="error" :closable="false">{{ errorMessage }}</Message>

        <Button label="Entrar" :loading="loading" @click="handleLogin" class="w-full" />

        <div class="text-center text-sm text-surface-500 flex flex-col gap-1">
          <RouterLink to="/forgot-password" class="text-primary-500 hover:underline">
            Esqueci minha senha
          </RouterLink>
          <span>Não tem conta?
            <RouterLink to="/register" class="text-primary-500 hover:underline">Cadastre-se</RouterLink>
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import type { Role } from '@/types/api'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Button from 'primevue/button'
import Message from 'primevue/message'

const auth = useAuthStore()
const router = useRouter()
const email = ref('')
const password = ref('')
const errorMessage = ref('')
const loading = ref(false)

const dashboards: Record<Role, string> = { ADMIN: '/admin', TRAINER: '/trainer', STUDENT: '/student' }

async function handleLogin() {
  errorMessage.value = ''
  if (!email.value || !password.value) {
    errorMessage.value = 'Preencha todos os campos.'
    return
  }
  loading.value = true
  try {
    const result = await auth.login(email.value, password.value)
    router.push(dashboards[result.role])
  } catch (err: any) {
    errorMessage.value = err.response?.data?.message ?? 'Erro ao fazer login. Tente novamente.'
  } finally {
    loading.value = false
  }
}
</script>
