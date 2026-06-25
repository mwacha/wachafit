<template>
  <AppLayout>
    <div class="dash">
      <div class="kpi-grid">
        <div class="kpi-card" v-for="kpi in kpis" :key="kpi.label">
          <span class="kpi-label">{{ kpi.label }}</span>
          <span class="kpi-value">{{ kpi.value }}</span>
          <span class="kpi-badge badge-up">+{{ kpi.trend }}%</span>
        </div>
        <div class="kpi-card kpi-highlight">
          <span class="kpi-label kpi-label-white">Aulas hoje</span>
          <span class="kpi-value kpi-value-white">5</span>
          <span class="kpi-badge badge-white">Confirmadas</span>
        </div>
      </div>

      <div class="placeholder-card">
        <i class="pi pi-users placeholder-icon" aria-hidden="true" />
        <p class="placeholder-title">Gestão de alunos e agenda</p>
        <p class="placeholder-sub">Disponível na Etapa 2 — agenda, avaliações e fichas de treino.</p>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import AppLayout from '@/components/AppLayout.vue'

const kpis = [
  { label: 'Alunos ativos',  value: '24', trend: 12 },
  { label: 'Sessões no mês', value: '68', trend: 8 },
  { label: 'Taxa de presença',value: '91%', trend: 3 },
]
</script>

<style scoped>
.dash { display: flex; flex-direction: column; gap: 20px; }

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.kpi-card {
  background: #fff; border: 1px solid var(--neutral-200);
  border-radius: var(--radius-lg); padding: 18px 16px;
  display: flex; flex-direction: column; gap: 6px;
  box-shadow: var(--shadow-card);
  transition: box-shadow 0.2s, transform 0.2s; cursor: default;
}
.kpi-card:hover { box-shadow: 0 4px 20px rgba(0,0,0,0.08); transform: translateY(-2px); }

.kpi-highlight {
  background: linear-gradient(135deg, var(--blue-500), var(--blue-700));
  border-color: transparent; box-shadow: var(--shadow-btn);
}

.kpi-label {
  font-size: 11px; font-weight: 500; color: var(--neutral-500);
  text-transform: uppercase; letter-spacing: 0.06em;
}
.kpi-label-white { color: rgba(255,255,255,0.65); }
.kpi-value {
  font-family: var(--font-display); font-size: 28px; font-weight: 700;
  color: var(--neutral-900); line-height: 1.1;
}
.kpi-value-white { color: #fff; }
.kpi-badge {
  display: inline-flex; align-items: center;
  font-size: 11px; font-weight: 700;
  padding: 3px 8px; border-radius: 6px; align-self: flex-start;
}
.badge-up { background: var(--success-bg); color: var(--success-text); }
.badge-white { background: rgba(255,255,255,0.18); color: #fff; }

.placeholder-card {
  background: #fff; border: 1px solid var(--neutral-200);
  border-radius: var(--radius-lg); padding: 48px 24px;
  display: flex; flex-direction: column; align-items: center;
  gap: 10px; text-align: center; box-shadow: var(--shadow-card);
}
.placeholder-icon { font-size: 32px; color: var(--neutral-300); }
.placeholder-title {
  font-family: var(--font-display); font-size: 16px; font-weight: 600;
  color: var(--neutral-800);
}
.placeholder-sub { font-size: 13px; color: var(--neutral-500); max-width: 320px; }
</style>
