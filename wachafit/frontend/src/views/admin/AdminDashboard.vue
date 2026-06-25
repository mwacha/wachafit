<template>
  <AppLayout>
    <div class="dash">
      <div class="kpi-grid">
        <div class="kpi-card" v-for="kpi in kpis" :key="kpi.label">
          <span class="kpi-label">{{ kpi.label }}</span>
          <span class="kpi-value">{{ kpi.value }}</span>
          <span class="kpi-badge" :class="kpi.up ? 'badge-up' : 'badge-neutral'">
            {{ kpi.up ? '+' : '' }}{{ kpi.delta }}
          </span>
        </div>
        <div class="kpi-card kpi-highlight">
          <span class="kpi-label kpi-label-white">Ocupação</span>
          <span class="kpi-value kpi-value-white">78%</span>
          <span class="kpi-badge badge-white">Esta semana</span>
        </div>
      </div>

      <div class="placeholder-card">
        <i class="pi pi-chart-bar placeholder-icon" aria-hidden="true" />
        <p class="placeholder-title">Painel administrativo</p>
        <p class="placeholder-sub">Disponível na Etapa 2 — gestão de turmas, profissionais e relatórios.</p>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import AppLayout from '@/components/AppLayout.vue'

const kpis = [
  { label: 'Alunos matriculados', value: '142', up: true,  delta: '12 este mês' },
  { label: 'Profissionais',       value: '8',   up: false, delta: 'estável' },
  { label: 'Aulas hoje',          value: '22',  up: true,  delta: '+3 vs ontem' },
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
  display: inline-flex; font-size: 11px; font-weight: 700;
  padding: 3px 8px; border-radius: 6px; align-self: flex-start;
}
.badge-up { background: var(--success-bg); color: var(--success-text); }
.badge-neutral { background: var(--neutral-100); color: var(--neutral-600); }
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
