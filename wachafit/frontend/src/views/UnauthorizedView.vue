<template>
  <div class="unauth">
    <div class="unauth-box">
      <div class="unauth-code">403</div>
      <h1 class="unauth-title">Acesso negado</h1>
      <p class="unauth-sub">Você não tem permissão para acessar esta página.</p>
      <Button label="Voltar" severity="secondary" @click="$router.back()" />
    </div>
  </div>
</template>

<script setup lang="ts">
import Button from 'primevue/button'
</script>

<style scoped>
.unauth {
  min-height: 100dvh;
  display: flex; align-items: center; justify-content: center;
  background: var(--neutral-50); padding: 24px;
}
.unauth-box {
  text-align: center; display: flex; flex-direction: column;
  align-items: center; gap: 12px; max-width: 360px;
}
.unauth-code {
  font-family: var(--font-display); font-size: 72px; font-weight: 800;
  color: var(--neutral-200); line-height: 1;
}
.unauth-title {
  font-family: var(--font-display); font-size: 24px; font-weight: 700;
  color: var(--neutral-900);
}
.unauth-sub { font-size: 14px; color: var(--neutral-600); line-height: 1.55; }
</style>
