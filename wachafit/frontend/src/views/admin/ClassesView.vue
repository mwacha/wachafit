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
          <Column field="capacity" header="Vagas" style="min-width:80px" />
          <Column header="Status" style="min-width:90px">
            <template #body="{ data }">
              <Tag :severity="data.active ? 'success' : 'danger'" :value="data.active ? 'Ativa' : 'Inativa'" />
            </template>
          </Column>
          <Column header="Ações" style="min-width:150px">
            <template #body="{ data }">
              <Button icon="pi pi-users" text v-tooltip.top="'Gerenciar Alunos'" @click="openEnroll(data)" />
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
import { ref, onMounted, computed } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useAdminStore } from '@/stores/admin.store'
import { useAuthStore } from '@/stores/auth.store'
import { groupClassService } from '@/services/groupclass.service'
import { userService } from '@/services/user.service'
import type { EnrolledStudent, GroupClass } from '@/types/api'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Select from 'primevue/select'

const adminStore = useAdminStore()
const authStore = useAuthStore()
const showCreate = ref(false)
const showEdit = ref(false)
const editingId = ref<string | null>(null)
const saving = ref(false)
const formError = ref<string | null>(null)

// --- Gerenciar Alunos ---
const showEnroll = ref(false)
const enrollingClass = ref<GroupClass | null>(null)
const enrolledStudents = ref<EnrolledStudent[]>([])
const enrolledLoading = ref(false)
const studentsLoading = ref(false)
const enrolling = ref(false)
const enrollError = ref<string | null>(null)
const selectedStudentId = ref<string | null>(null)
const allStudents = ref<{ id: string; name: string; email: string }[]>([])

const studentOptions = computed(() =>
  allStudents.value.map(s => ({ label: `${s.name} (${s.email})`, value: s.id }))
)

async function openEnroll(cls: GroupClass) {
  enrollingClass.value = cls
  enrolledStudents.value = []
  selectedStudentId.value = null
  enrollError.value = null
  showEnroll.value = true

  enrolledLoading.value = true
  studentsLoading.value = true
  try {
    const [enrolled, students] = await Promise.all([
      groupClassService.listEnrolled(cls.id),
      userService.list({ role: 'STUDENT' }),
    ])
    enrolledStudents.value = enrolled
    allStudents.value = students
  } finally {
    enrolledLoading.value = false
    studentsLoading.value = false
  }
}

async function doEnroll() {
  if (!enrollingClass.value || !selectedStudentId.value) return
  enrolling.value = true
  enrollError.value = null
  try {
    await groupClassService.enrollStudent(enrollingClass.value.id, selectedStudentId.value)
    selectedStudentId.value = null
    enrolledStudents.value = await groupClassService.listEnrolled(enrollingClass.value.id)
  } catch (e: any) {
    enrollError.value = e.response?.data?.message ?? 'Erro ao adicionar aluno.'
  } finally { enrolling.value = false }
}

async function doUnenroll(studentId: string) {
  if (!enrollingClass.value) return
  enrollError.value = null
  try {
    await groupClassService.unenrollStudent(enrollingClass.value.id, studentId)
    enrolledStudents.value = await groupClassService.listEnrolled(enrollingClass.value.id)
  } catch (e: any) {
    enrollError.value = e.response?.data?.message ?? 'Erro ao remover aluno.'
  }
}

const defaultForm = () => ({ name: '', capacity: 10, scheduleType: 'FLEX', durationMinutes: 60, startTime: '', endTime: '' })
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
  }
  formError.value = null
  showEdit.value = true
}

function buildPayload(f: typeof form.value) {
  if (f.scheduleType === 'FIXED') {
    return { name: f.name, capacity: f.capacity, scheduleType: 'FIXED', startTime: f.startTime, endTime: f.endTime }
  }
  return { name: f.name, capacity: f.capacity, scheduleType: 'FLEX', durationMinutes: f.durationMinutes }
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

/* Gerenciar Alunos dialog */
.enroll-dialog { display: flex; flex-direction: column; gap: 16px; padding-top: 4px; }
.add-student-row { display: flex; gap: 10px; align-items: center; }
.enroll-loading { text-align: center; padding: 24px; color: var(--neutral-400); }
.enroll-empty { text-align: center; padding: 24px; color: var(--neutral-400); font-size: 14px; }
.enrolled-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; max-height: 340px; overflow-y: auto; }
.enrolled-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 12px; border: 1px solid var(--neutral-200);
  border-radius: var(--radius-md); background: #fafafa;
}
.enrolled-info { display: flex; flex-direction: column; gap: 2px; }
.enrolled-name { font-size: 14px; font-weight: 600; color: var(--neutral-800); }
.enrolled-email { font-size: 12px; color: var(--neutral-500); }
.enrolled-meta { display: flex; align-items: center; gap: 6px; }
.enrolled-count { font-size: 12px; color: var(--neutral-500); white-space: nowrap; }
.enroll-error { color: var(--red-600); font-size: 13px; margin: 0; }
</style>
