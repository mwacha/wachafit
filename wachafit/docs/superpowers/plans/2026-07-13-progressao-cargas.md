# RF-13.4: Gráfico de Progressão de Cargas — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Adicionar seção "Progressão de Cargas" em EvolutionView com dropdown de exercícios e gráfico de linha de carga ao longo do tempo.

**Architecture:** Modificação de um único arquivo Vue (`EvolutionView.vue`). Novos refs `exercises`, `selectedExerciseId`, `progressionPoints`, `loadingProgression` + função `loadProgression()` + computed `progressionChartData` + bloco de template + CSS. Reutiliza `workoutService.progression()` e `exerciseService.search()` já existentes, além do `Chart` PrimeVue já importado na view.

**Tech Stack:** Vue 3 + TypeScript, PrimeVue (Chart, Select), `exerciseService.search()`, `workoutService.progression()`, Vite dev server (`cd frontend && npm run dev`)

## Global Constraints

- PrimeVue components importados individualmente: `import Select from 'primevue/select'`
- `workoutService.progression(studentId, exerciseId)` retorna `Promise<ProgressionPoint[]>` — tipo já em `@/types/api`
- `ProgressionPoint`: `{ performedAt: string; loadKg: number | null; reps: number | null }`
- `Exercise`: `{ id: string; name: string; muscleGroup: string; ... active: boolean }`
- Cor do gráfico de progressão: `#22c55e` (verde — diferente do azul/laranja do gráfico de avaliações)
- `spanGaps: true` no dataset para não quebrar linha quando `loadKg` for `null`
- `onMounted` passa a ser `async`
- Sem novos arquivos, sem novos stores
- Commit em português com prefixo `feat:`

---

### Task 1: Seção de progressão de cargas em EvolutionView

**Files:**
- Modify: `frontend/src/views/student/EvolutionView.vue`

**Interfaces:**
- Consumes: `exerciseService.search(): Promise<Exercise[]>` — `frontend/src/services/exercise.service.ts`
- Consumes: `workoutService.progression(studentId: string, exerciseId: string): Promise<ProgressionPoint[]>` — `frontend/src/services/workout.service.ts` linha 19
- Consumes: `authStore.userId` — já disponível na view
- Produces: nada (view terminal)

**Contexto do arquivo atual:**

```
frontend/src/views/student/EvolutionView.vue
- template linha 1–57 (AppLayout wrapping, page-header, empty-state, 2 section-cards)
- script linha 59–125 (imports, refs: downloadingPdf, reactive: metrics, computed: chartData, const: chartOptions, onMounted, downloadEvolutionPdf)
- style linha 127–158
```

Imports atuais no script (linhas 60–68):
```ts
import { computed, onMounted, reactive, ref } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useAssessmentStore } from '@/stores/assessment.store'
import { useAuthStore } from '@/stores/auth.store'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Chart from 'primevue/chart'
import api from '@/services/api'
```

`onMounted` atual (linha 113):
```ts
onMounted(() => assessmentStore.fetchAssessments(authStore.userId!))
```

---

- [ ] **Step 1: Adicionar imports novos no script**

Após `import api from '@/services/api'` (linha 68), adicionar:

```ts
import { exerciseService } from '@/services/exercise.service'
import { workoutService } from '@/services/workout.service'
import Select from 'primevue/select'
import type { Exercise, ProgressionPoint } from '@/types/api'
```

- [ ] **Step 2: Adicionar refs de progressão**

Após `const downloadingPdf = ref(false)` (linha 72), adicionar:

```ts
const exercises = ref<Exercise[]>([])
const selectedExerciseId = ref<string | null>(null)
const progressionPoints = ref<ProgressionPoint[]>([])
const loadingProgression = ref(false)
```

- [ ] **Step 3: Tornar onMounted async e carregar exercícios**

Substituir `onMounted` (linha 113):
```ts
onMounted(() => assessmentStore.fetchAssessments(authStore.userId!))
```
por:
```ts
onMounted(async () => {
  await assessmentStore.fetchAssessments(authStore.userId!)
  exercises.value = await exerciseService.search()
})
```

- [ ] **Step 4: Adicionar função loadProgression**

Após o bloco `onMounted`, antes de `downloadEvolutionPdf`, adicionar:

```ts
async function loadProgression() {
  if (!selectedExerciseId.value) return
  loadingProgression.value = true
  try {
    progressionPoints.value = await workoutService.progression(
      authStore.userId!,
      selectedExerciseId.value
    )
  } finally {
    loadingProgression.value = false
  }
}
```

- [ ] **Step 5: Adicionar computed progressionChartData e const progressionChartOptions**

Após `const chartOptions = { ... }` (termina na linha 111), adicionar:

```ts
const progressionChartData = computed(() => ({
  labels: progressionPoints.value.map(p => p.performedAt),
  datasets: [{
    label: 'Carga (kg)',
    data: progressionPoints.value.map(p => p.loadKg),
    borderColor: '#22c55e',
    backgroundColor: '#22c55e22',
    fill: false,
    tension: 0.3,
    pointRadius: 5,
    pointHoverRadius: 7,
    spanGaps: true,
  }],
}))

const progressionChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    tooltip: { mode: 'index', intersect: false },
  },
  scales: {
    x: { grid: { color: '#f1f5f9' } },
    y: { grid: { color: '#f1f5f9' }, beginAtZero: true },
  },
}
```

- [ ] **Step 6: Adicionar seção no template**

No template, após o fechamento do `<div class="section-card">` que contém o DataTable de histórico (antes de `</template>` da diretiva `v-else`, linha ~54), inserir nova seção:

O bloco `<template v-else>` atual termina assim (linhas 49–56):
```html
        </div>
      </template>
    </div>
  </AppLayout>
</template>
```

Alterar para:
```html
        </div>

        <!-- Progressão de Cargas -->
        <div class="section-card">
          <h2 class="section-title">Progressão de Cargas</h2>
          <Select
            v-model="selectedExerciseId"
            :options="exercises"
            optionLabel="name"
            optionValue="id"
            placeholder="Selecione um exercício"
            filter
            class="exercise-select"
            @change="loadProgression"
          />
          <div v-if="loadingProgression" class="chart-empty">
            <i class="pi pi-spin pi-spinner" />
          </div>
          <p v-else-if="selectedExerciseId && progressionPoints.length === 0" class="empty-msg">
            Nenhum registro encontrado para este exercício.
          </p>
          <Chart
            v-else-if="progressionPoints.length > 0"
            type="line"
            :data="progressionChartData"
            :options="progressionChartOptions"
            class="progression-chart"
          />
        </div>
      </template>
    </div>
  </AppLayout>
</template>
```

- [ ] **Step 7: Adicionar CSS**

No `<style scoped>`, após `.empty-hint { font-size: 13px; }` (linha 157, última regra antes de `</style>`), adicionar:

```css
.exercise-select { width: 100%; max-width: 320px; margin-bottom: 16px; }
.progression-chart { height: 220px; }
.chart-empty { text-align: center; padding: 40px; color: var(--neutral-400); font-size: 14px; }
.empty-msg { color: var(--p-text-muted-color, #9ca3af); font-size: 0.875rem; text-align: center; padding: 2rem 0; }
```

- [ ] **Step 8: Verificar no browser**

```bash
cd frontend && npm run dev
```

Logar como STUDENT. Navegar em `/student/evolution`. Verificar:
1. Seção "Progressão de Cargas" aparece abaixo da tabela de histórico
2. Dropdown lista os exercícios ativos (com campo de filtro)
3. Selecionar exercício com logs registrados → gráfico de linha verde aparece com datas no eixo X e carga (kg) no eixo Y
4. Selecionar exercício sem logs → mensagem "Nenhum registro encontrado para este exercício."
5. Spinner aparece brevemente durante carregamento
6. O gráfico de avaliações acima continua funcionando normalmente

- [ ] **Step 9: Commit**

```bash
git add frontend/src/views/student/EvolutionView.vue
git commit -m "feat: gráfico de progressão de cargas por exercício em EvolutionView (RF-13.4)"
```
