<!-- frontend/src/views/admin/ClassesView.vue -->
<template>
  <AppLayout>
    <div class="view-wrap">
      <div class="page-header">
        <h1 class="page-title">Turmas</h1>
        <Button label="Nova turma" icon="pi pi-plus" @click="openCreate" />
      </div>

      <div class="table-scroll">
        <DataTable :value="adminStore.classes" :loading="adminStore.loading" stripedRows>
          <template #empty>Nenhuma turma cadastrada.</template>
          <Column field="name" header="Nome" style="min-width:140px" />
          <Column header="Tipo" style="min-width:130px">
            <template #body="{ data }">
              <Tag :severity="data.scheduleType === 'FIXED' ? 'info' : 'secondary'"
                   :value="data.scheduleType === 'FIXED' ? 'Aula Fixa' : 'Horário Livre'" />
            </template>
          </Column>
          <Column header="Horário / Duração" style="min-width:160px">
            <template #body="{ data }">
              <span v-if="data.scheduleType === 'FIXED' && data.startTime && data.endTime">
                {{ data.startTime.slice(0,5) }} – {{ data.endTime.slice(0,5) }}
              </span>
              <span v-else>{{ data.durationMinutes }} min</span>
            </template>
          </Column>
          <Column header="Vagas" style="min-width:100px">
            <template #body="{ data }">
              <span class="vagas-badge" :class="vagasClass(data)">
                {{ data.capacity - data.enrolledCount }}/{{ data.capacity }}
              </span>
            </template>
          </Column>
          <Column header="Status" style="min-width:90px">
            <template #body="{ data }">
              <Tag :severity="data.active ? 'success' : 'danger'" :value="data.active ? 'Ativa' : 'Inativa'" />
            </template>
          </Column>
          <Column header="Ações" style="min-width:100px">
            <template #body="{ data }">
              <Button icon="pi pi-pencil" text @click="openEdit(data)" />
              <Button v-if="data.active" icon="pi pi-trash" severity="danger" text @click="deactivate(data.id)" />
            </template>
          </Column>
        </DataTable>
      </div>

      <!-- Dialog: Gerenciar Alunos -->
      <Dialog v-model:visible="showEnroll" :header="`Alunos — ${enrollingClass?.name ?? ''}`"
              :modal="true" style="width: min(520px, 95vw)">
        <div class="enroll-dialog">
          <!-- Add student row -->
          <div class="add-student-row">
            <Select v-model="selectedStudentId" :options="studentOptions"
                    optionLabel="label" optionValue="value"
                    placeholder="Buscar aluno..." filter style="flex:1"
                    :loading="studentsLoading" />
            <Button label="Adicionar" icon="pi pi-plus" :loading="enrolling"
                    :disabled="!selectedStudentId" @click="doEnroll" />
          </div>

          <div v-if="enrolledLoading" class="enroll-loading"><i class="pi pi-spin pi-spinner" /></div>
          <div v-else-if="enrolledStudents.length === 0" class="enroll-empty">
            Nenhum aluno inscrito nos próximos horários.
          </div>
          <ul v-else class="enrolled-list">
            <li v-for="s in enrolledStudents" :key="s.studentId" class="enrolled-item">
              <div class="enrolled-info">
                <span class="enrolled-name">{{ s.name }}</span>
                <span class="enrolled-email">{{ s.email }}</span>
              </div>
              <div class="enrolled-meta">
                <span class="enrolled-count">{{ s.upcomingBookings }} aula{{ s.upcomingBookings !== 1 ? 's' : '' }}</span>
                <Button icon="pi pi-times" severity="danger" text size="small"
                        v-tooltip.top="'Remover'" @click="doUnenroll(s.studentId)" />
              </div>
            </li>
          </ul>

          <p v-if="enrollError" class="enroll-error">{{ enrollError }}</p>
          <div class="flex justify-end mt-3">
            <Button label="Fechar" outlined @click="showEnroll = false" />
          </div>
        </div>
      </Dialog>

      <!-- Dialog: Nova Turma -->
      <Dialog v-model:visible="showCreate" header="Nova Turma" :modal="true" style="width: min(480px, 95vw)">
        <form @submit.prevent="submitCreate" class="class-form">
          <div class="form-field">
            <label class="form-label">Nome *</label>
            <InputText v-model="form.name" style="width:100%" required />
          </div>
          <div class="form-field">
            <label class="form-label">Tipo de turma *</label>
            <div class="type-toggle">
              <button type="button"
                :class="['toggle-btn', { active: form.scheduleType === 'FLEX' }]"
                @click="form.scheduleType = 'FLEX'">
                <i class="pi pi-clock" /> Horário Livre
              </button>
              <button type="button"
                :class="['toggle-btn', { active: form.scheduleType === 'FIXED' }]"
                @click="form.scheduleType = 'FIXED'">
                <i class="pi pi-calendar" /> Aula Fixa
              </button>
            </div>
          </div>

          <template v-if="form.scheduleType === 'FIXED'">
            <div class="time-grid">
              <div class="form-field">
                <label class="form-label">Horário início *</label>
                <InputText v-model="form.startTime" type="time" style="width:100%" required />
              </div>
              <div class="form-field">
                <label class="form-label">Horário fim *</label>
                <InputText v-model="form.endTime" type="time" style="width:100%" required />
              </div>
            </div>
            <div class="form-field">
              <label class="form-label">Dias da semana *</label>
              <div class="days-row">
                <button v-for="d in DAY_OPTIONS" :key="d.key" type="button"
                  :class="['day-btn', { active: form.daysOfWeek.includes(d.key) }]"
                  @click="toggleDay(form.daysOfWeek, d.key)">
                  {{ d.label }}
                </button>
              </div>
            </div>
          </template>

          <template v-else>
            <div class="form-field">
              <label class="form-label">Duração (minutos) *</label>
              <InputNumber v-model="form.durationMinutes" :min="1" style="width:100%" required />
            </div>
          </template>

          <div class="form-field">
            <label class="form-label">Capacidade (vagas) *</label>
            <InputNumber v-model="form.capacity" :min="1" style="width:100%" required />
          </div>

          <p v-if="formError" class="form-error">{{ formError }}</p>
          <div class="form-actions">
            <Button type="button" label="Cancelar" outlined @click="showCreate = false" />
            <Button type="submit" label="Criar" :loading="saving" />
          </div>
        </form>
      </Dialog>

      <!-- Dialog: Editar Turma -->
      <Dialog v-model:visible="showEdit" header="Editar Turma" :modal="true" style="width: min(480px, 95vw)">
        <form @submit.prevent="submitEdit" class="class-form">
          <div class="form-field">
            <label class="form-label">Nome *</label>
            <InputText v-model="editForm.name" style="width:100%" required />
          </div>
          <div class="form-field">
            <label class="form-label">Tipo de turma *</label>
            <div class="type-toggle">
              <button type="button"
                :class="['toggle-btn', { active: editForm.scheduleType === 'FLEX' }]"
                @click="editForm.scheduleType = 'FLEX'">
                <i class="pi pi-clock" /> Horário Livre
              </button>
              <button type="button"
                :class="['toggle-btn', { active: editForm.scheduleType === 'FIXED' }]"
                @click="editForm.scheduleType = 'FIXED'">
                <i class="pi pi-calendar" /> Aula Fixa
              </button>
            </div>
          </div>

          <template v-if="editForm.scheduleType === 'FIXED'">
            <div class="time-grid">
              <div class="form-field">
                <label class="form-label">Horário início *</label>
                <InputText v-model="editForm.startTime" type="time" style="width:100%" required />
              </div>
              <div class="form-field">
                <label class="form-label">Horário fim *</label>
                <InputText v-model="editForm.endTime" type="time" style="width:100%" required />
              </div>
            </div>
            <div class="form-field">
              <label class="form-label">Dias da semana *</label>
              <div class="days-row">
                <button v-for="d in DAY_OPTIONS" :key="d.key" type="button"
                  :class="['day-btn', { active: editForm.daysOfWeek.includes(d.key) }]"
                  @click="toggleDay(editForm.daysOfWeek, d.key)">
                  {{ d.label }}
                </button>
              </div>
            </div>
          </template>

          <template v-else>
            <div class="form-field">
              <label class="form-label">Duração (minutos) *</label>
              <InputNumber v-model="editForm.durationMinutes" :min="1" style="width:100%" required />
            </div>
          </template>

          <div class="form-field">
            <label class="form-label">Capacidade (vagas) *</label>
            <InputNumber v-model="editForm.capacity" :min="1" style="width:100%" required />
          </div>

          <p v-if="formError" class="form-error">{{ formError }}</p>
          <div class="form-actions">
            <Button type="button" label="Cancelar" outlined @click="showEdit = false" />
            <Button type="submit" label="Salvar" :loading="saving" />
          </div>
        </form>
      </Dialog>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useAdminStore } from '@/stores/admin.store'
import { useAuthStore } from '@/stores/auth.store'
import { groupClassService } from '@/services/groupclass.service'
import type { GroupClass } from '@/types/api'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'

const DAY_OPTIONS = [
  { key: 'MON', label: 'Seg' },
  { key: 'TUE', label: 'Ter' },
  { key: 'WED', label: 'Qua' },
  { key: 'THU', label: 'Qui' },
  { key: 'FRI', label: 'Sex' },
  { key: 'SAT', label: 'Sáb' },
  { key: 'SUN', label: 'Dom' },
]

function toggleDay(arr: string[], key: string) {
  const idx = arr.indexOf(key)
  if (idx >= 0) arr.splice(idx, 1)
  else arr.push(key)
}

function vagasClass(data: GroupClass) {
  const pct = data.capacity > 0 ? data.enrolledCount / data.capacity : 0
  if (pct >= 1) return 'vagas-full'
  if (pct >= 0.8) return 'vagas-almost'
  return 'vagas-ok'
}

const adminStore = useAdminStore()
const authStore = useAuthStore()
const showCreate = ref(false)
const showEdit = ref(false)
const editingId = ref<string | null>(null)
const saving = ref(false)
const formError = ref<string | null>(null)


const defaultForm = () => ({ name: '', capacity: 10, scheduleType: 'FLEX', durationMinutes: 60, startTime: '', endTime: '', daysOfWeek: [] as string[] })
const form = ref(defaultForm())
const editForm = ref(defaultForm())

onMounted(() => adminStore.fetchClasses())

function openCreate() {
  form.value = defaultForm()
  formError.value = null
  showCreate.value = true
}

function openEdit(cls: GroupClass) {
  editingId.value = cls.id
  editForm.value = {
    name: cls.name,
    capacity: cls.capacity,
    scheduleType: cls.scheduleType ?? 'FLEX',
    durationMinutes: cls.durationMinutes,
    startTime: cls.startTime?.slice(0, 5) ?? '',
    endTime: cls.endTime?.slice(0, 5) ?? '',
    daysOfWeek: cls.daysOfWeek ? [...cls.daysOfWeek] : [],
  }
  formError.value = null
  showEdit.value = true
}

function buildPayload(f: typeof form.value) {
  if (f.scheduleType === 'FIXED') {
    return { name: f.name, capacity: f.capacity, scheduleType: 'FIXED', startTime: f.startTime, endTime: f.endTime, daysOfWeek: f.daysOfWeek }
  }
  return { name: f.name, capacity: f.capacity, scheduleType: 'FLEX', durationMinutes: f.durationMinutes, daysOfWeek: null }
}

async function submitCreate() {
  saving.value = true; formError.value = null
  try {
    await groupClassService.create({ ...buildPayload(form.value), trainerId: authStore.userId! })
    showCreate.value = false
    await adminStore.fetchClasses()
  } catch (e: any) {
    formError.value = e.response?.data?.message ?? 'Erro ao criar turma'
  } finally { saving.value = false }
}

async function submitEdit() {
  if (!editingId.value) return
  saving.value = true; formError.value = null
  try {
    await groupClassService.update(editingId.value, buildPayload(editForm.value))
    showEdit.value = false
    await adminStore.fetchClasses()
  } catch (e: any) {
    formError.value = e.response?.data?.message ?? 'Erro ao salvar'
  } finally { saving.value = false }
}

async function deactivate(id: string) {
  await groupClassService.deactivate(id)
  await adminStore.fetchClasses()
}
</script>

<style scoped>
.view-wrap { display: flex; flex-direction: column; gap: 20px; }
.page-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; }
.page-title { font-family: var(--font-display); font-size: 22px; font-weight: 700; color: var(--neutral-900); }
.table-scroll { overflow-x: auto; border-radius: var(--radius-lg); }
.class-form { display: flex; flex-direction: column; gap: 20px; padding-top: 4px; }
.form-field { display: flex; flex-direction: column; gap: 6px; }
.form-label { font-size: 13px; font-weight: 600; color: var(--neutral-700); }
.form-actions { display: flex; justify-content: flex-end; gap: 8px; padding-top: 4px; }
.form-error { color: var(--red-600); font-size: 13px; margin: 0; }
.time-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }

.type-toggle { display: flex; gap: 8px; }
.toggle-btn {
  flex: 1; display: flex; align-items: center; justify-content: center; gap: 6px;
  padding: 9px 12px; border-radius: var(--radius-md);
  border: 1.5px solid var(--neutral-200); background: #fff;
  font-size: 13px; font-weight: 500; color: var(--neutral-600);
  cursor: pointer; transition: all .15s;
}
.toggle-btn:hover { border-color: var(--blue-400); color: var(--blue-600); }
.toggle-btn.active {
  border-color: var(--blue-500); background: var(--blue-50);
  color: var(--blue-700); font-weight: 600;
}

/* Vagas badge */
.vagas-badge { font-size: 12px; font-weight: 700; padding: 2px 8px; border-radius: 20px; }
.vagas-ok    { background: var(--green-50, #f0fdf4); color: var(--green-700, #15803d); }
.vagas-almost{ background: #fffbf0; color: var(--orange-600, #ea580c); }
.vagas-full  { background: #fff5f5; color: var(--red-600, #dc2626); }

/* Days selector */
.days-row { display: flex; gap: 6px; flex-wrap: wrap; }
.day-btn {
  padding: 5px 10px; border-radius: var(--radius-sm);
  border: 1.5px solid var(--neutral-200); background: #fff;
  font-size: 12px; font-weight: 600; color: var(--neutral-600);
  cursor: pointer; transition: all .12s;
}
.day-btn:hover { border-color: var(--blue-400); color: var(--blue-600); }
.day-btn.active { border-color: var(--blue-500); background: var(--blue-50); color: var(--blue-700); }
</style>
