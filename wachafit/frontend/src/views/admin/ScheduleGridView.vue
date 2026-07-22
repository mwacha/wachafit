<template>
  <AppLayout>
    <div class="view-wrap">
      <div class="page-header">
        <h1 class="page-title">Grade de Horários</h1>
        <Button icon="pi pi-refresh" text v-tooltip.top="'Atualizar'" @click="reload" :loading="loading" />
      </div>

      <div v-if="loading" class="loading-state"><i class="pi pi-spin pi-spinner" /></div>

      <template v-else>
        <div v-if="hasNoFixedClasses" class="empty-grid">
          <i class="pi pi-table empty-icon" />
          <p>Nenhuma turma fixa cadastrada.</p>
          <p class="empty-hint">Crie uma turma do tipo "Aula Fixa" com dias da semana em <strong>Turmas</strong>.</p>
        </div>

        <div v-else class="schedule-wrap">
          <!-- Cabeçalho fixo com nomes dos dias -->
          <div class="sched-header">
            <div class="time-gutter-head" />
            <div v-for="day in DAYS" :key="day.key" class="day-head">{{ day.label }}</div>
          </div>
          <!-- Corpo com rolagem: régua de tempo + colunas -->
          <div class="sched-body">
            <div class="time-gutter" :style="{ height: `${GRID_HEIGHT}px` }">
              <div v-for="h in hours" :key="h" class="time-label"
                   :style="{ top: `${(h - START_HOUR) * 60 * PX_PER_MIN}px` }">
                {{ String(h).padStart(2, '0') }}:00
              </div>
            </div>
            <div class="days-area">
              <div v-for="day in DAYS" :key="day.key" class="day-col"
                   :style="{ height: `${GRID_HEIGHT}px` }">
                <div v-for="h in hours" :key="h" class="hour-line"
                     :style="{ top: `${(h - START_HOUR) * 60 * PX_PER_MIN}px` }" />
                <div v-for="cls in classesForDay(day.key)" :key="cls.id"
                     class="class-card" :class="cardStatus(cls)"
                     :style="cardStyle(cls)"
                     @click="openEnroll(cls)">
                  <div class="card-name">{{ cls.name }}</div>
                  <div class="card-time">{{ cls.startTime?.slice(0,5) }} – {{ cls.endTime?.slice(0,5) }}</div>
                  <div v-if="cls.trainerName" class="card-trainer">
                    <i class="pi pi-user" />
                    {{ cls.trainerName }}
                  </div>
                  <div class="card-capacity">
                    <i class="pi pi-users" />
                    {{ cls.enrolledCount }}/{{ cls.capacity }}
                    <span class="vagas-label">vagas</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- Dialog: Associar Alunos -->
      <Dialog v-model:visible="showEnroll"
              :header="enrollingClass ? `${enrollingClass.name} — Alunos` : ''"
              :modal="true" style="width: min(540px, 95vw)">
        <div class="enroll-dialog" v-if="enrollingClass">
          <div class="class-info-bar">
            <span class="info-item"><i class="pi pi-clock" /> {{ enrollingClass.startTime }} – {{ enrollingClass.endTime }}</span>
            <span class="info-item">
              <i class="pi pi-users" />
              {{ enrollingClass.enrolledCount }}/{{ enrollingClass.capacity }} vagas
            </span>
            <span v-if="enrollingClass.trainerName" class="info-item">
              <i class="pi pi-user" />
              {{ enrollingClass.trainerName }}
            </span>
          </div>

          <InputText v-model="searchQuery" placeholder="Buscar aluno pelo nome ou e-mail..."
                     style="width:100%" />

          <div v-if="dialogLoading" class="dialog-loading"><i class="pi pi-spin pi-spinner" /></div>
          <template v-else>
            <!-- Inscritos -->
            <div v-if="filteredEnrolled.length > 0" class="student-section">
              <div class="section-label">Inscritos ({{ enrolledStudents.length }})</div>
              <div v-for="s in filteredEnrolled" :key="s.studentId" class="student-row enrolled-row">
                <div class="student-info">
                  <span class="s-name">{{ s.name }}</span>
                  <span class="s-email">{{ s.email }}</span>
                </div>
                <Button icon="pi pi-times" severity="danger" text size="small"
                        v-tooltip.top="'Remover'" @click="doUnenroll(s.studentId)" />
              </div>
            </div>

            <!-- Disponíveis -->
            <div class="student-section">
              <div class="section-label">
                Disponíveis{{ searchQuery ? ` — "${searchQuery}"` : '' }}
              </div>
              <div v-if="filteredAvailable.length === 0" class="no-results">
                {{ searchQuery ? 'Nenhum aluno encontrado.' : 'Todos os alunos já estão inscritos.' }}
              </div>
              <div v-for="s in filteredAvailable" :key="s.id" class="student-row">
                <div class="student-info">
                  <span class="s-name">{{ s.name }}</span>
                  <span class="s-email">{{ s.email }}</span>
                </div>
                <Button icon="pi pi-plus" size="small" outlined
                        :loading="enrollingId === s.id"
                        @click="doEnroll(s.id)" />
              </div>
            </div>
          </template>

        </div>
      </Dialog>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import AppLayout from '@/components/AppLayout.vue'
import { groupClassService } from '@/services/groupclass.service'
import { userService } from '@/services/user.service'
import type { EnrolledStudent, GroupClass } from '@/types/api'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'

const toast = useToast()

const DAYS = [
  { key: 'MON', label: 'Segunda' },
  { key: 'TUE', label: 'Terça' },
  { key: 'WED', label: 'Quarta' },
  { key: 'THU', label: 'Quinta' },
  { key: 'FRI', label: 'Sexta' },
  { key: 'SAT', label: 'Sábado' },
  { key: 'SUN', label: 'Domingo' },
]

const START_HOUR = 6
const END_HOUR = 23
const PX_PER_MIN = 1.0
const hours = Array.from({ length: END_HOUR - START_HOUR }, (_, i) => START_HOUR + i)
const GRID_HEIGHT = (END_HOUR - START_HOUR) * 60 * PX_PER_MIN

function timeToMinutes(t: string): number {
  const [h, m] = t.split(':').map(Number)
  return h * 60 + m
}

function cardStyle(cls: GroupClass) {
  if (!cls.startTime) return {}
  const top = (timeToMinutes(cls.startTime) - START_HOUR * 60) * PX_PER_MIN
  const height = Math.max(cls.durationMinutes * PX_PER_MIN, 44)
  return { top: `${top}px`, height: `${height}px` }
}

const loading = ref(true)
const classes = ref<GroupClass[]>([])

const hasNoFixedClasses = computed(() =>
  classes.value.filter(c => c.active && c.scheduleType === 'FIXED' && c.daysOfWeek?.length).length === 0
)

async function reload() {
  loading.value = true
  classes.value = await groupClassService.list()
  loading.value = false
}

onMounted(reload)

function classesForDay(dayKey: string): GroupClass[] {
  return classes.value
    .filter(c => c.active && c.scheduleType === 'FIXED' && c.daysOfWeek?.includes(dayKey))
    .sort((a, b) => (a.startTime ?? '').localeCompare(b.startTime ?? ''))
}

function cardStatus(cls: GroupClass) {
  const pct = cls.capacity > 0 ? cls.enrolledCount / cls.capacity : 0
  if (pct >= 1) return 'card-full'
  if (pct >= 0.8) return 'card-almost'
  return ''
}

// Enrollment dialog
const showEnroll = ref(false)
const enrollingClass = ref<GroupClass | null>(null)
const dialogLoading = ref(false)
const enrolledStudents = ref<EnrolledStudent[]>([])
const allStudents = ref<{ id: string; name: string; email: string }[]>([])
const searchQuery = ref('')
const enrollingId = ref<string | null>(null)

async function openEnroll(cls: GroupClass) {
  enrollingClass.value = cls
  searchQuery.value = ''
  showEnroll.value = true
  dialogLoading.value = true
  try {
    const [enrolled, students] = await Promise.all([
      groupClassService.listEnrolled(cls.id),
      allStudents.value.length ? Promise.resolve(allStudents.value) : userService.list({ role: 'STUDENT' }),
    ])
    enrolledStudents.value = enrolled
    if (!allStudents.value.length) allStudents.value = students as typeof allStudents.value
    enrollingClass.value = { ...cls, enrolledCount: enrolled.length }
  } finally {
    dialogLoading.value = false
  }
}

const enrolledIds = computed(() => new Set(enrolledStudents.value.map(s => s.studentId)))

const filteredEnrolled = computed(() => {
  const q = searchQuery.value.toLowerCase()
  return enrolledStudents.value.filter(s =>
    !q || s.name.toLowerCase().includes(q) || s.email.toLowerCase().includes(q)
  )
})

const filteredAvailable = computed(() => {
  const q = searchQuery.value.toLowerCase()
  return allStudents.value.filter(s =>
    !enrolledIds.value.has(s.id) &&
    (!q || s.name.toLowerCase().includes(q) || s.email.toLowerCase().includes(q))
  )
})

async function doEnroll(studentId: string) {
  if (!enrollingClass.value) return
  enrollingId.value = studentId
  try {
    await groupClassService.enrollStudent(enrollingClass.value.id, studentId)
    enrolledStudents.value = await groupClassService.listEnrolled(enrollingClass.value.id)
    enrollingClass.value = { ...enrollingClass.value, enrolledCount: enrolledStudents.value.length }
    const idx = classes.value.findIndex(c => c.id === enrollingClass.value!.id)
    if (idx >= 0) classes.value[idx] = { ...classes.value[idx], enrolledCount: enrolledStudents.value.length }
  } catch (e: any) {
    toast.add({
      severity: 'error',
      summary: 'Não foi possível inscrever',
      detail: e.response?.data?.message ?? 'Erro ao inscrever aluno.',
      life: 6000,
    })
  } finally {
    enrollingId.value = null
  }
}

async function doUnenroll(studentId: string) {
  if (!enrollingClass.value) return
  try {
    await groupClassService.unenrollStudent(enrollingClass.value.id, studentId)
    enrolledStudents.value = await groupClassService.listEnrolled(enrollingClass.value.id)
    enrollingClass.value = { ...enrollingClass.value, enrolledCount: enrolledStudents.value.length }
    const idx = classes.value.findIndex(c => c.id === enrollingClass.value!.id)
    if (idx >= 0) classes.value[idx] = { ...classes.value[idx], enrolledCount: enrolledStudents.value.length }
  } catch (e: any) {
    toast.add({
      severity: 'error',
      summary: 'Erro',
      detail: e.response?.data?.message ?? 'Erro ao remover aluno.',
      life: 6000,
    })
  }
}
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 20px; }
.page-header { display: flex; align-items: center; justify-content: space-between; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }
.loading-state { text-align: center; padding: 60px; color: var(--neutral-400); font-size: 24px; }

.empty-grid {
  text-align: center; padding: 60px 20px;
  display: flex; flex-direction: column; align-items: center; gap: 10px;
}
.empty-icon { font-size: 40px; color: var(--neutral-300); }
.empty-grid p { color: var(--neutral-500); margin: 0; }
.empty-hint { font-size: 13px; color: var(--neutral-400) !important; }

/* Schedule wrap */
.schedule-wrap {
  display: flex; flex-direction: column;
  border: 1px solid var(--neutral-200);
  border-radius: var(--radius-lg);
  overflow: hidden;
  max-height: 75vh;
}

/* Fixed header */
.sched-header {
  display: flex;
  border-bottom: 2px solid var(--neutral-200);
  background: var(--neutral-50);
  flex-shrink: 0;
}
.time-gutter-head { width: 48px; flex-shrink: 0; }
.day-head {
  flex: 1; min-width: 100px; text-align: center; padding: 7px 4px;
  font-size: 11px; font-weight: 700; text-transform: uppercase;
  letter-spacing: .05em; color: var(--neutral-500);
  border-left: 1px solid var(--neutral-200);
}

/* Scrollable body */
.sched-body {
  display: flex;
  overflow-y: auto;
  overflow-x: auto;
  flex: 1;
}

/* Time ruler */
.time-gutter {
  position: relative; width: 48px; flex-shrink: 0;
  border-right: 1px solid var(--neutral-200);
  background: var(--neutral-50);
}
.time-label {
  position: absolute; right: 6px;
  font-size: 10px; color: var(--neutral-400);
  white-space: nowrap; line-height: 1; user-select: none;
  padding-top: 2px;
}

/* Days area */
.days-area { display: flex; flex: 1; }
.day-col {
  flex: 1; min-width: 100px; position: relative;
  border-left: 1px solid var(--neutral-200);
}
.hour-line {
  position: absolute; left: 0; right: 0; height: 1px;
  background: var(--neutral-100); pointer-events: none;
}

/* Class card — absolute positioned */
.class-card {
  position: absolute; left: 4px; right: 4px;
  background: var(--blue-50); border: 1.5px solid var(--blue-200);
  border-radius: var(--radius-md); padding: 5px 7px;
  cursor: pointer; transition: border-color .15s, box-shadow .15s;
  overflow: hidden; display: flex; flex-direction: column; gap: 2px;
  box-sizing: border-box;
}
.class-card:hover { border-color: var(--blue-400); box-shadow: 0 2px 8px rgba(59,130,246,.18); }
.card-full   { border-color: var(--red-300)    !important; background: #fff5f5; }
.card-almost { border-color: var(--orange-300) !important; background: #fffbf0; }

.card-name { font-size: 12px; font-weight: 700; color: var(--neutral-800); line-height: 1.2; }
.card-time { font-size: 10px; color: var(--neutral-500); }
.card-trainer {
  display: flex; align-items: center; gap: 3px;
  font-size: 10px; color: var(--blue-600); font-weight: 500;
}
.card-capacity {
  display: flex; align-items: center; gap: 3px;
  font-size: 11px; font-weight: 600; color: var(--neutral-600);
}
.card-full  .card-capacity { color: var(--red-600); }
.vagas-label { font-weight: 400; color: var(--neutral-400); }

/* Enrollment dialog */
.enroll-dialog { display: flex; flex-direction: column; gap: 14px; padding-top: 4px; }
.class-info-bar {
  display: flex; gap: 16px; flex-wrap: wrap;
  background: var(--neutral-50); border: 1px solid var(--neutral-200);
  border-radius: var(--radius-md); padding: 10px 14px;
}
.info-item { display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--neutral-600); }
.info-item i { color: var(--blue-500); }

.dialog-loading { text-align: center; padding: 32px; color: var(--neutral-400); font-size: 22px; }
.student-section { display: flex; flex-direction: column; gap: 6px; max-height: 260px; overflow-y: auto; }
.section-label { font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: .05em; color: var(--neutral-400); position: sticky; top: 0; background: #fff; padding: 2px 0; }

.student-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 10px; border-radius: var(--radius-sm);
  border: 1px solid var(--neutral-200);
}
.enrolled-row { background: var(--green-50, #f0fdf4); border-color: var(--green-200, #bbf7d0); }

.student-info { display: flex; flex-direction: column; gap: 1px; min-width: 0; }
.s-name  { font-size: 13px; font-weight: 600; color: var(--neutral-800); }
.s-email { font-size: 11px; color: var(--neutral-500); }
.s-meta  { display: flex; align-items: center; gap: 4px; flex-shrink: 0; }
.s-count { font-size: 11px; color: var(--neutral-400); white-space: nowrap; }

.no-results  { font-size: 13px; color: var(--neutral-400); padding: 8px 4px; }
.enroll-error{ color: var(--red-600); font-size: 13px; margin: 0; }
</style>
