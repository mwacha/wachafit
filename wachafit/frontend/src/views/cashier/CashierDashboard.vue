<template>
  <AppLayout>
    <div class="dashboard-page">
      <h1 class="page-title">Dashboard — Caixa</h1>
      <p class="page-subtitle">Gerencie cobranças e registre pagamentos.</p>
      <div class="quick-links">
        <RouterLink to="/cashier/charges" class="quick-card">
          <i class="pi pi-money-bill" />
          <span>Cobranças</span>
        </RouterLink>
        <RouterLink to="/cashier/cash-flow" class="quick-card">
          <i class="pi pi-chart-line" />
          <span>Fluxo de Caixa</span>
        </RouterLink>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import AppLayout from '@/components/AppLayout.vue'
</script>

<style scoped>
.dashboard-page { padding: 8px 0; }
.page-title { font-size: 24px; font-weight: 700; color: var(--neutral-900, #111827); margin-bottom: 4px; }
.page-subtitle { color: var(--neutral-500, #6b7280); font-size: 14px; margin-bottom: 24px; }
.quick-links { display: flex; gap: 16px; flex-wrap: wrap; }
.quick-card {
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  background: #fff; border: 1.5px solid #e5e7eb; border-radius: 12px;
  padding: 20px 28px; color: #374151; text-decoration: none;
  font-size: 13px; font-weight: 600; transition: border-color 0.15s, box-shadow 0.15s;
}
.quick-card i { font-size: 22px; color: #3b82f6; }
.quick-card:hover { border-color: #60a5fa; box-shadow: 0 2px 8px rgba(59,130,246,0.12); }
</style>
