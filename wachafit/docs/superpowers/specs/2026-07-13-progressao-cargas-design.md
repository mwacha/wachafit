# Design — RF-13.4: Gráfico de Progressão de Cargas

**Data:** 2026-07-13
**Status:** Aprovado

---

## Contexto

O PRD exige gráfico de progressão de carga por exercício (RF-13.4). O backend já expõe `GET /api/students/{studentId}/exercises/{exerciseId}/progression` retornando `ProgressionPoint[]` (`{ performedAt: string; loadKg: number | null; reps: number | null }`). O frontend não consume esse endpoint.

---

## Escopo

Modificar apenas `frontend/src/views/student/EvolutionView.vue`. Nenhum arquivo novo.

---

## Design

### Nova seção em EvolutionView

Inserida após a tabela de histórico de avaliações, dentro de um `.section-card` com o mesmo estilo das seções existentes.

**Estrutura do template:**

```html
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
```

**Novos refs e função no script:**

```ts
import { exerciseService } from '@/services/exercise.service'
import { workoutService } from '@/services/workout.service'
import Select from 'primevue/select'
import type { Exercise, ProgressionPoint } from '@/types/api'

const exercises = ref<Exercise[]>([])
const selectedExerciseId = ref<string | null>(null)
const progressionPoints = ref<ProgressionPoint[]>([])
const loadingProgression = ref(false)

// Adicionar em onMounted:
exercises.value = await exerciseService.search()

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

**Novos estilos (scoped):**

```css
.exercise-select { width: 100%; max-width: 320px; margin-bottom: 16px; }
.progression-chart { height: 220px; }
.chart-empty { text-align: center; padding: 40px; color: var(--neutral-400); font-size: 14px; }
.empty-msg { color: var(--p-text-muted-color, #9ca3af); font-size: 0.875rem; text-align: center; padding: 2rem 0; }
```

---

## Detalhes técnicos

- `exerciseService.search()` sem parâmetros retorna todos os exercícios ativos — carregado uma vez no `onMounted`
- `workoutService.progression(studentId, exerciseId)` chama `GET /api/students/{id}/exercises/{exerciseId}/progression`
- O `Select` PrimeVue tem prop `filter` para busca inline — útil quando o catálogo crescer
- Cor do gráfico: `#22c55e` (verde) — diferente do azul/laranja/roxo do gráfico de avaliações
- `spanGaps: true` para não quebrar a linha quando `loadKg` for `null`
- `onMounted` passa a ser `async` para aguardar `exerciseService.search()`

---

## Arquivos a modificar

- `frontend/src/views/student/EvolutionView.vue` — único arquivo

## Arquivos a criar

Nenhum.

---

## Critérios de aceite

- Seção "Progressão de Cargas" aparece abaixo da tabela de histórico
- Dropdown lista todos os exercícios ativos
- Ao selecionar exercício com registros: gráfico de linha verde mostra carga ao longo do tempo
- Ao selecionar exercício sem registros: mensagem "Nenhum registro encontrado para este exercício."
- Loading spinner durante a busca
