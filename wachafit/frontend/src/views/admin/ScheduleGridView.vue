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

        <div v-else class="week-grid">
          <div v-for="day in DAYS" :key="day.key" class="day-col">
            <div class="day-header">{{ day.label }}</div>
            <div class="day-cards">
              <div v-for="cls in classesForDay(day.key)" :key="cls.id"
                   class="class-card" :class="cardStatus(cls)" @click="openEnroll(cls)">
                <div class="card-name">{{ cls.name }}</div>
                <div class="card-time">{{ cls.startTime }} – {{ cls.endTime }}</div>
                <div class="card-capacity">
                  <i class="pi pi-users" />
                  {{ cls.enrolledCount }}/{{ cls.capacity }}
                  <span class="vagas-label">vagas</span>
                </div>
              </div>
              <div v-if="classesForDay(day.key).length === 0" class="day-empty">—</div>
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
          </div>

          <InputText v-model="searchQuery" placeholder="Buscar aluno pelo nome ou e-mail..."
                     style="width:100%" @input="enrollError = null" />

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
                <div class="s-meta">
                  <span class="s-count">{{ s.upcomingBookings }} aula{{ s.upcomingBookings !== 1 ? 's' : '' }}</span>
                  <Button icon="pi pi-times" severity="danger" text size="small"
                          v-tooltip.top="'Remover'" @click="doUnenroll(s.studentId)" />
                </div>
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

          <p v-if="enrollError" class="enroll-error">{{ enrollError }}</p>
        </div>
      </Dialog>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { groupClassService } from '@/services/groupclass.service'
import { userService } from '@/services/user.service'
import type { EnrolledStudent, GroupClass } from '@/types/api'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'

const DAYS = [
  { key: 'MON', label: 'Segunda' },
  { key: 'TUE', label: 'Terça' },
  { key: 'WED', label: 'Quarta' },
  { key: 'THU', label: 'Quinta' },
  { key: 'FRI', label: 'Sexta' },
  { key: 'SAT', label: 'Sábado' },
  { key: 'SUN', label: 'Domingo' },
]

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
const enrollError = ref<string | null>(null)

async function openEnroll(cls: GroupClass) {
  enrollingClass.value = cls
  searchQuery.value = ''
  enrollError.value = null
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
  enrollError.value = null
  try {
    await groupClassService.enrollStudent(enrollingClass.value.id, studentId)
    enrolledStudents.value = await groupClassService.listEnrolled(enrollingClass.value.id)
    enrollingClass.value = { ...enrollingClass.value, enrolledCount: enrolledStudents.value.length }
    const idx = classes.value.findIndex(c => c.id === enrollingClass.value!.id)
    if (idx >= 0) classes.value[idx] = { ...classes.value[idx], enrolledCount: enrolledStudents.value.length }
  } catch (e: any) {
    enrollError.value = e.response?.data?.message ?? 'Erro ao inscrever aluno.'
  } finally {
    enrollingId.value = null
  }
}

async function doUnenroll(studentId: string) {
  if (!enrollingClass.value) return
  enrollError.value = null
  try {
    await groupClassService.unenrollStudent(enrollingClass.value.id, studentId)
    enrolledStudents.value = await groupClassService.listEnrolled(enrollingClass.value.id)
    enrollingClass.value = { ...enrollingClass.value, enrolledCount: enrolledStudents.value.length }
    const idx = classes.value.findIndex(c => c.id === enrollingClass.value!.id)
    if (idx >= 0) classes.value[idx] = { ...classes.value[idx], enrolledCount: enrolledStudents.value.length }
  } catch (e: any) {
    enrollError.value = e.response?.data?.message ?? 'Erro ao remover aluno.'
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

/* Grid */
.week-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(130px, 1fr));
  gap: 12px;
  overflow-x: auto;
  padding-bottom: 8px;
}
.day-col { display: flex; flex-direction: column; gap: 8px; min-width: 130px; }
.day-header {
  text-align: center; font-size: 12px; font-weight: 700;
  text-transform: uppercase; letter-spacing: .05em;
  color: var(--neutral-500); padding: 4px 0;
  border-bottom: 1px solid var(--neutral-200);
}
.day-cards { display: flex; flex-direction: column; gap: 8px; padding-top: 4px; }
.day-empty { text-align: center; color: var(--neutral-300); font-size: 20px; padding: 16px 0; }

/* Class card */
.class-card {
  background: #fff; border: 1.5px solid var(--neutral-200);
  border-radius: var(--radius-md); padding: 12px 10px;
  cursor: pointer; transition: border-color .15s, box-shadow .15s;
  display: flex; flex-direction: column; gap: 4px;
}
.class-card:hover { border-color: var(--blue-400); box-shadow: 0 2px 8px rgba(59,130,246,.12); }
.card-full  { border-color: var(--red-300)    !important; background: #fff5f5; }
.card-almost{ border-color: var(--orange-300) !important; background: #fffbf0; }

.card-name { font-size: 13px; font-weight: 700; color: var(--neutral-800); line-height: 1.3; }
.card-time { font-size: 11px; color: var(--neutral-500); }
.card-capacity {
  display: flex; align-items: center; gap: 4px;
  font-size: 12px; font-weight: 600; color: var(--neutral-600); margin-top: 2px;
}
.card-full  .card-capacity { color: var(--red-600); }
.card-almost.card-capacity { color: var(--orange-600); }
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
