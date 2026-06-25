<template>
  <div class="min-h-screen flex flex-col">
    <nav class="p-4 bg-surface-100 border-b border-surface-200 flex items-center justify-between">
      <span class="font-bold text-xl text-primary-600">Wachafit</span>
      <div class="flex items-center gap-3">
        <span class="text-sm text-surface-600 capitalize">{{ auth.role?.toLowerCase() }}</span>
        <Button label="Sair" severity="secondary" size="small" @click="handleLogout" />
      </div>
    </nav>
    <main class="flex-1 p-6">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import Button from 'primevue/button'

const auth = useAuthStore()
const router = useRouter()

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>
