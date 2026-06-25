<template>
  <div class="flex flex-col items-center justify-center min-h-screen gap-4">
    <h1 class="text-3xl font-bold">403 — Acesso negado</h1>
    <p class="text-surface-500">Você não tem permissão para acessar esta página.</p>
    <Button label="Voltar" @click="$router.back()" severity="secondary" />
  </div>
</template>

<script setup lang="ts">
import Button from 'primevue/button'
</script>
