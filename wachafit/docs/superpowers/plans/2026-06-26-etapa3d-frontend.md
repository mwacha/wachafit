# WachaFit Etapa 3D — Frontend Completo Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the complete frontend — Etapa 2 core (T-13: calendar, booking, admin panel, class management) and Etapa 3 tracking screens (T-18: evolution, workout plans, goals, progress photos).

**Architecture:** Vue 3 Composition API (`<script setup lang="ts">`). New Pinia stores per domain. New API service modules. New views added to the existing router. Sidebar in `AppLayout.vue` updated with links per role. PrimeVue components throughout; no FullCalendar dependency.

**Tech Stack:** Vue 3, TypeScript, Pinia, Vue Router 4, Axios (via existing `api.ts`), PrimeVue 4, Vite

## Global Constraints

- Worktree: `/Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit/`
- Frontend root: `wachafit/frontend/`
- Follow existing patterns in `auth.store.ts`, `AppLayout.vue`, `router/index.ts`
- All API calls via `import api from '@/services/api'` (existing axios instance with JWT interceptor)
- Current user via `authStore.userId`, `authStore.role` (Pinia store)
- Route guards already protect `/admin`, `/trainer`, `/student` by role
- No new npm packages unless strictly necessary
- Git: `git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit`
- Build verification: `cd frontend && npm run build 2>&1 | tail -10` — must succeed with zero TypeScript errors

---

## Task 1: API services + Pinia stores (T-13 + T-18 foundation)

**Files:**
- Create: `frontend/src/services/schedule.service.ts`
- Create: `frontend/src/services/booking.service.ts`
- Create: `frontend/src/services/user.service.ts`
- Create: `frontend/src/services/groupclass.service.ts`
- Create: `frontend/src/services/assessment.service.ts`
- Create: `frontend/src/services/goal.service.ts`
- Create: `frontend/src/services/exercise.service.ts`
- Create: `frontend/src/services/workout.service.ts`
- Create: `frontend/src/services/progress.service.ts`
- Create: `frontend/src/stores/schedule.store.ts`
- Create: `frontend/src/stores/booking.store.ts`
- Create: `frontend/src/stores/admin.store.ts`
- Create: `frontend/src/stores/assessment.store.ts`
- Create: `frontend/src/stores/workout.store.ts`
- Create: `frontend/src/stores/progress.store.ts`
- Modify: `frontend/src/types/api.ts` — add all new types

- [ ] **Step 1: Add new types to `frontend/src/types/api.ts`**

Append to the existing file:

```typescript
// --- Schedule / Booking ---
export type ScheduleType = 'CLASS' | 'PERSONAL'
export type ScheduleStatus = 'OPEN' | 'FULL' | 'CANCELLED'

export interface Schedule {
  id: string
  groupClassId: string | null
  trainerId: string
  type: ScheduleType
  startsAt: string
  endsAt: string
  status: ScheduleStatus
  createdAt: string
}

export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED'

export interface Booking {
  id: string
  scheduleId: string
  studentId: string
  status: BookingStatus
  bookedAt: string
}

// --- Admin ---
export interface AdminUser {
  id: string
  name: string
  email: string
  role: Role
  active: boolean
  createdAt: string
}

export interface GroupClass {
  id: string
  name: string
  description: string | null
  capacity: number
  durationMinutes: number
  trainerId: string
  active: boolean
  createdAt: string
}

// --- Assessment ---
export interface Measurement { bodyPart: string; valueCm: number }
export interface Assessment {
  id: string
  studentId: string
  assessedBy: string
  assessedAt: string
  weightKg: number | null
  heightCm: number | null
  bodyFatPct: number | null
  bmi: number | null
  notes: string | null
  measurements: Measurement[]
  createdAt: string
}
export interface EvolutionPoint { assessedAt: string; weightKg: number | null; bodyFatPct: number | null; bmi: number | null }

// --- Goal ---
export type GoalStatus = 'IN_PROGRESS' | 'ACHIEVED' | 'EXPIRED'
export interface Goal {
  id: string
  studentId: string
  createdById: string
  description: string
  metric: string | null
  targetValue: number | null
  targetDate: string | null
  status: GoalStatus
  createdAt: string
}

// --- Exercise ---
export interface Exercise {
  id: string
  name: string
  muscleGroup: string
  description: string | null
  videoUrl: string | null
  active: boolean
}

// --- Workout ---
export interface WorkoutPlanItem {
  id: string
  exerciseId: string
  division: string | null
  sets: number
  reps: string
  suggestedLoadKg: number | null
  restSeconds: number | null
  orderIndex: number
  notes: string | null
}
export interface WorkoutPlan {
  id: string
  studentId: string
  trainerId: string
  name: string
  description: string | null
  active: boolean
  createdAt: string
  items: WorkoutPlanItem[]
}
export interface WorkoutLog {
  id: string
  studentId: string
  exerciseId: string
  performedAt: string
  sets: number | null
  reps: number | null
  loadKg: number | null
  notes: string | null
  createdAt: string
}
export interface PersonalRecord {
  id: string
  exerciseId: string
  recordLoadKg: number
  achievedAt: string
}
export interface ProgressionPoint { performedAt: string; loadKg: number | null; reps: number | null }

// --- Progress Photos ---
export interface Photo {
  id: string
  studentId: string
  uploadedBy: string
  takenAt: string
  notes: string | null
  fileUrl: string
  createdAt: string
}
```

- [ ] **Step 2: Create API service files**

```typescript
// frontend/src/services/schedule.service.ts
import api from './api'
import type { Schedule } from '@/types/api'

export const scheduleService = {
  list: (params?: { trainerId?: string; date?: string; type?: string }) =>
    api.get<Schedule[]>('/api/schedules', { params }).then(r => r.data),
  create: (data: { groupClassId?: string; trainerId: string; type: string; startsAt: string; endsAt: string }) =>
    api.post<Schedule>('/api/schedules', data).then(r => r.data),
  cancel: (id: string) => api.delete(`/api/schedules/${id}`),
}

// frontend/src/services/booking.service.ts
import api from './api'
import type { Booking } from '@/types/api'

export const bookingService = {
  myBookings: () => api.get<Booking[]>('/api/bookings/my').then(r => r.data),
  create: (scheduleId: string) => api.post<Booking>('/api/bookings', { scheduleId }).then(r => r.data),
  cancel: (id: string) => api.delete(`/api/bookings/${id}`),
}

// frontend/src/services/user.service.ts
import api from './api'
import type { AdminUser } from '@/types/api'

export const userService = {
  list: (params?: { role?: string; active?: boolean }) =>
    api.get<AdminUser[]>('/api/admin/users', { params }).then(r => r.data),
  create: (data: { name: string; email: string; password: string; role: string }) =>
    api.post<AdminUser>('/api/admin/users', data).then(r => r.data),
  update: (id: string, data: { name: string; role: string }) =>
    api.patch<AdminUser>(`/api/admin/users/${id}`, data).then(r => r.data),
  deactivate: (id: string) => api.delete(`/api/admin/users/${id}`),
  activate: (id: string) => api.patch(`/api/admin/users/${id}/activate`),
}

// frontend/src/services/groupclass.service.ts
import api from './api'
import type { GroupClass } from '@/types/api'

export const groupClassService = {
  list: () => api.get<GroupClass[]>('/api/classes').then(r => r.data),
  create: (data: { name: string; description?: string; capacity: number; durationMinutes: number; trainerId: string }) =>
    api.post<GroupClass>('/api/classes', data).then(r => r.data),
  update: (id: string, data: { name: string; description?: string; capacity: number; durationMinutes: number }) =>
    api.put<GroupClass>(`/api/classes/${id}`, data).then(r => r.data),
  deactivate: (id: string) => api.delete(`/api/classes/${id}`),
}

// frontend/src/services/assessment.service.ts
import api from './api'
import type { Assessment, EvolutionPoint } from '@/types/api'

export const assessmentService = {
  list: (studentId: string) =>
    api.get<Assessment[]>(`/api/students/${studentId}/assessments`).then(r => r.data),
  evolution: (studentId: string) =>
    api.get<EvolutionPoint[]>(`/api/students/${studentId}/assessments/evolution`).then(r => r.data),
  create: (studentId: string, data: Partial<Assessment> & { measurements?: { bodyPart: string; valueCm: number }[] }) =>
    api.post<Assessment>(`/api/students/${studentId}/assessments`, data).then(r => r.data),
  update: (id: string, data: unknown) =>
    api.put<Assessment>(`/api/assessments/${id}`, data).then(r => r.data),
}

// frontend/src/services/goal.service.ts
import api from './api'
import type { Goal } from '@/types/api'

export const goalService = {
  list: (studentId: string) =>
    api.get<Goal[]>(`/api/students/${studentId}/goals`).then(r => r.data),
  create: (studentId: string, data: { description: string; metric?: string; targetValue?: number; targetDate?: string }) =>
    api.post<Goal>(`/api/students/${studentId}/goals`, data).then(r => r.data),
  updateStatus: (id: string, status: string) =>
    api.patch<Goal>(`/api/goals/${id}/status`, { status }).then(r => r.data),
}

// frontend/src/services/exercise.service.ts
import api from './api'
import type { Exercise } from '@/types/api'

export const exerciseService = {
  search: (params?: { q?: string; muscleGroup?: string }) =>
    api.get<Exercise[]>('/api/exercises', { params }).then(r => r.data),
  create: (data: { name: string; muscleGroup: string; description?: string; videoUrl?: string }) =>
    api.post<Exercise>('/api/exercises', data).then(r => r.data),
  update: (id: string, data: { name: string; muscleGroup: string; description?: string; videoUrl?: string }) =>
    api.put<Exercise>(`/api/exercises/${id}`, data).then(r => r.data),
  deactivate: (id: string) => api.delete(`/api/exercises/${id}`),
}

// frontend/src/services/workout.service.ts
import api from './api'
import type { WorkoutPlan, WorkoutLog, PersonalRecord, ProgressionPoint } from '@/types/api'

export const workoutService = {
  listPlans: (studentId: string) =>
    api.get<WorkoutPlan[]>(`/api/students/${studentId}/workout-plans`).then(r => r.data),
  getActivePlan: (studentId: string) =>
    api.get<WorkoutPlan>(`/api/students/${studentId}/workout-plans/active`).then(r => r.data),
  createPlan: (studentId: string, data: unknown) =>
    api.post<WorkoutPlan>(`/api/students/${studentId}/workout-plans`, data).then(r => r.data),
  activatePlan: (planId: string) =>
    api.patch<WorkoutPlan>(`/api/workout-plans/${planId}/activate`).then(r => r.data),
  createLog: (studentId: string, data: { exerciseId: string; performedAt: string; sets?: number; reps?: number; loadKg?: number; notes?: string }) =>
    api.post<WorkoutLog>(`/api/students/${studentId}/workout-logs`, data).then(r => r.data),
  listLogs: (studentId: string) =>
    api.get<WorkoutLog[]>(`/api/students/${studentId}/workout-logs`).then(r => r.data),
  listRecords: (studentId: string) =>
    api.get<PersonalRecord[]>(`/api/students/${studentId}/records`).then(r => r.data),
  progression: (studentId: string, exerciseId: string) =>
    api.get<ProgressionPoint[]>(`/api/students/${studentId}/exercises/${exerciseId}/progression`).then(r => r.data),
}

// frontend/src/services/progress.service.ts
import api from './api'
import type { Photo } from '@/types/api'

export const progressService = {
  list: (studentId: string) =>
    api.get<Photo[]>(`/api/students/${studentId}/photos`).then(r => r.data),
  upload: (studentId: string, file: File, takenAt?: string, notes?: string) => {
    const form = new FormData()
    form.append('file', file)
    if (takenAt) form.append('takenAt', takenAt)
    if (notes) form.append('notes', notes)
    return api.post<Photo>(`/api/students/${studentId}/photos`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(r => r.data)
  },
  delete: (id: string) => api.delete(`/api/photos/${id}`),
}
```

- [ ] **Step 3: Create Pinia stores**

```typescript
// frontend/src/stores/schedule.store.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { scheduleService } from '@/services/schedule.service'
import type { Schedule } from '@/types/api'

export const useScheduleStore = defineStore('schedule', () => {
  const schedules = ref<Schedule[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchSchedules(params?: { date?: string; type?: string; trainerId?: string }) {
    loading.value = true; error.value = null
    try { schedules.value = await scheduleService.list(params) }
    catch (e: any) { error.value = e.response?.data?.message ?? 'Erro ao carregar horários' }
    finally { loading.value = false }
  }

  async function createSchedule(data: Parameters<typeof scheduleService.create>[0]) {
    const s = await scheduleService.create(data)
    schedules.value.unshift(s)
    return s
  }

  async function cancelSchedule(id: string) {
    await scheduleService.cancel(id)
    schedules.value = schedules.value.filter(s => s.id !== id)
  }

  return { schedules, loading, error, fetchSchedules, createSchedule, cancelSchedule }
})

// frontend/src/stores/booking.store.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { bookingService } from '@/services/booking.service'
import type { Booking } from '@/types/api'

export const useBookingStore = defineStore('booking', () => {
  const bookings = ref<Booking[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchMyBookings() {
    loading.value = true; error.value = null
    try { bookings.value = await bookingService.myBookings() }
    catch (e: any) { error.value = e.response?.data?.message ?? 'Erro ao carregar reservas' }
    finally { loading.value = false }
  }

  async function createBooking(scheduleId: string) {
    const b = await bookingService.create(scheduleId)
    bookings.value.unshift(b)
    return b
  }

  async function cancelBooking(id: string) {
    await bookingService.cancel(id)
    bookings.value = bookings.value.filter(b => b.id !== id)
  }

  return { bookings, loading, error, fetchMyBookings, createBooking, cancelBooking }
})

// frontend/src/stores/admin.store.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { userService } from '@/services/user.service'
import { groupClassService } from '@/services/groupclass.service'
import type { AdminUser, GroupClass } from '@/types/api'

export const useAdminStore = defineStore('admin', () => {
  const users = ref<AdminUser[]>([])
  const classes = ref<GroupClass[]>([])
  const loading = ref(false)

  async function fetchUsers() {
    loading.value = true
    try { users.value = await userService.list() } finally { loading.value = false }
  }

  async function fetchClasses() {
    loading.value = true
    try { classes.value = await groupClassService.list() } finally { loading.value = false }
  }

  return { users, classes, loading, fetchUsers, fetchClasses }
})

// frontend/src/stores/assessment.store.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { assessmentService } from '@/services/assessment.service'
import type { Assessment, EvolutionPoint } from '@/types/api'

export const useAssessmentStore = defineStore('assessment', () => {
  const assessments = ref<Assessment[]>([])
  const evolution = ref<EvolutionPoint[]>([])
  const loading = ref(false)

  async function fetchAssessments(studentId: string) {
    loading.value = true
    try {
      assessments.value = await assessmentService.list(studentId)
      evolution.value = await assessmentService.evolution(studentId)
    } finally { loading.value = false }
  }

  return { assessments, evolution, loading, fetchAssessments }
})

// frontend/src/stores/workout.store.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { workoutService } from '@/services/workout.service'
import type { WorkoutPlan, PersonalRecord } from '@/types/api'

export const useWorkoutStore = defineStore('workout', () => {
  const activePlan = ref<WorkoutPlan | null>(null)
  const plans = ref<WorkoutPlan[]>([])
  const records = ref<PersonalRecord[]>([])
  const loading = ref(false)

  async function fetchActivePlan(studentId: string) {
    loading.value = true
    try { activePlan.value = await workoutService.getActivePlan(studentId) }
    catch { activePlan.value = null }
    finally { loading.value = false }
  }

  async function fetchRecords(studentId: string) {
    loading.value = true
    try { records.value = await workoutService.listRecords(studentId) }
    finally { loading.value = false }
  }

  return { activePlan, plans, records, loading, fetchActivePlan, fetchRecords }
})

// frontend/src/stores/progress.store.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { progressService } from '@/services/progress.service'
import type { Photo } from '@/types/api'

export const useProgressStore = defineStore('progress', () => {
  const photos = ref<Photo[]>([])
  const loading = ref(false)

  async function fetchPhotos(studentId: string) {
    loading.value = true
    try { photos.value = await progressService.list(studentId) }
    finally { loading.value = false }
  }

  async function uploadPhoto(studentId: string, file: File, takenAt?: string, notes?: string) {
    const photo = await progressService.upload(studentId, file, takenAt, notes)
    photos.value.unshift(photo)
  }

  async function deletePhoto(id: string) {
    await progressService.delete(id)
    photos.value = photos.value.filter(p => p.id !== id)
  }

  return { photos, loading, fetchPhotos, uploadPhoto, deletePhoto }
})
```

- [ ] **Step 4: Verify build passes**

```bash
cd /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit/frontend
npm run build 2>&1 | tail -10
```

Expected: `built in Xs` with zero TypeScript errors.

- [ ] **Step 5: Commit**

```bash
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit add frontend/src/
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit commit -m "feat: frontend API services and Pinia stores for Etapa 2+3"
```

---

## Task 2: Router + AppLayout update (T-13 + T-18)

**Files:**
- Modify: `frontend/src/router/index.ts` — add all new routes
- Modify: `frontend/src/components/AppLayout.vue` — add sidebar links per role

- [ ] **Step 1: Update router with all new routes**

Read `frontend/src/router/index.ts` first to understand existing structure. Then add these route groups:

```typescript
// Add inside the routes array, after existing routes:

// --- Admin routes ---
{
  path: '/admin/users',
  component: () => import('@/views/admin/UsersView.vue'),
  meta: { requiresAuth: true, role: 'ADMIN' }
},
{
  path: '/admin/classes',
  component: () => import('@/views/admin/ClassesView.vue'),
  meta: { requiresAuth: true, role: 'ADMIN' }
},
{
  path: '/admin/schedules',
  component: () => import('@/views/admin/SchedulesView.vue'),
  meta: { requiresAuth: true, role: 'ADMIN' }
},

// --- Trainer routes ---
{
  path: '/trainer/schedule',
  component: () => import('@/views/trainer/ScheduleView.vue'),
  meta: { requiresAuth: true, role: 'TRAINER' }
},
{
  path: '/trainer/students',
  component: () => import('@/views/trainer/StudentsView.vue'),
  meta: { requiresAuth: true, role: 'TRAINER' }
},
{
  path: '/trainer/students/:id/overview',
  component: () => import('@/views/trainer/StudentOverviewView.vue'),
  meta: { requiresAuth: true, role: 'TRAINER' }
},

// --- Student routes ---
{
  path: '/student/schedule',
  component: () => import('@/views/student/ScheduleView.vue'),
  meta: { requiresAuth: true, role: 'STUDENT' }
},
{
  path: '/student/bookings',
  component: () => import('@/views/student/BookingsView.vue'),
  meta: { requiresAuth: true, role: 'STUDENT' }
},
{
  path: '/student/workout',
  component: () => import('@/views/student/WorkoutView.vue'),
  meta: { requiresAuth: true, role: 'STUDENT' }
},
{
  path: '/student/records',
  component: () => import('@/views/student/RecordsView.vue'),
  meta: { requiresAuth: true, role: 'STUDENT' }
},
{
  path: '/student/evolution',
  component: () => import('@/views/student/EvolutionView.vue'),
  meta: { requiresAuth: true, role: 'STUDENT' }
},
{
  path: '/student/goals',
  component: () => import('@/views/student/GoalsView.vue'),
  meta: { requiresAuth: true, role: 'STUDENT' }
},
{
  path: '/student/photos',
  component: () => import('@/views/student/PhotosView.vue'),
  meta: { requiresAuth: true, role: 'STUDENT' }
},
```

- [ ] **Step 2: Update AppLayout.vue sidebar**

Read `frontend/src/components/AppLayout.vue`. Add navigation items for each role. The sidebar should show different links based on `authStore.role`. Example sidebar structure to add:

```vue
<!-- Inside the sidebar section, replace placeholder nav with: -->
<nav class="flex flex-col gap-1 p-2">
  <!-- Admin links -->
  <template v-if="authStore.role === 'ADMIN'">
    <RouterLink to="/admin" class="sidebar-link">Dashboard</RouterLink>
    <RouterLink to="/admin/users" class="sidebar-link">Usuários</RouterLink>
    <RouterLink to="/admin/classes" class="sidebar-link">Turmas</RouterLink>
    <RouterLink to="/admin/schedules" class="sidebar-link">Agenda</RouterLink>
  </template>
  <!-- Trainer links -->
  <template v-else-if="authStore.role === 'TRAINER'">
    <RouterLink to="/trainer" class="sidebar-link">Dashboard</RouterLink>
    <RouterLink to="/trainer/schedule" class="sidebar-link">Minha Agenda</RouterLink>
    <RouterLink to="/trainer/students" class="sidebar-link">Alunos</RouterLink>
  </template>
  <!-- Student links -->
  <template v-else-if="authStore.role === 'STUDENT'">
    <RouterLink to="/student" class="sidebar-link">Dashboard</RouterLink>
    <RouterLink to="/student/schedule" class="sidebar-link">Aulas Disponíveis</RouterLink>
    <RouterLink to="/student/bookings" class="sidebar-link">Minhas Reservas</RouterLink>
    <RouterLink to="/student/workout" class="sidebar-link">Treino</RouterLink>
    <RouterLink to="/student/records" class="sidebar-link">Recordes</RouterLink>
    <RouterLink to="/student/evolution" class="sidebar-link">Evolução</RouterLink>
    <RouterLink to="/student/goals" class="sidebar-link">Metas</RouterLink>
    <RouterLink to="/student/photos" class="sidebar-link">Fotos</RouterLink>
  </template>
</nav>
```

Add `sidebar-link` CSS class (or use PrimeVue styles consistent with existing design):
```css
.sidebar-link {
  @apply flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium
         text-surface-700 hover:bg-surface-100 hover:text-primary-600
         router-link-active:bg-primary-50 router-link-active:text-primary-700;
}
```

- [ ] **Step 3: Verify build**

```bash
cd /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit/frontend
npm run build 2>&1 | tail -10
```

Expected: `built in Xs` — zero errors. (Views don't exist yet, but lazy imports won't fail at build time.)

- [ ] **Step 4: Commit**

```bash
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit add frontend/src/router/ frontend/src/components/AppLayout.vue
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit commit -m "feat: router and sidebar updated for Etapa 2+3 routes"
```

---

## Task 3: Admin + Trainer views (T-13)

**Files:**
- Create: `frontend/src/views/admin/UsersView.vue`
- Create: `frontend/src/views/admin/ClassesView.vue`
- Create: `frontend/src/views/admin/SchedulesView.vue`
- Create: `frontend/src/views/trainer/ScheduleView.vue`
- Create: `frontend/src/views/trainer/StudentsView.vue`
- Create: `frontend/src/views/trainer/StudentOverviewView.vue`

- [ ] **Step 1: Create `UsersView.vue`**

```vue
<!-- frontend/src/views/admin/UsersView.vue -->
<template>
  <AppLayout>
    <div class="p-6">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold">Usuários</h1>
        <Button label="Novo usuário" icon="pi pi-plus" @click="showCreate = true" />
      </div>

      <DataTable :value="adminStore.users" :loading="adminStore.loading" stripedRows>
        <Column field="name" header="Nome" />
        <Column field="email" header="Email" />
        <Column field="role" header="Perfil" />
        <Column header="Status">
          <template #body="{ data }">
            <Tag :severity="data.active ? 'success' : 'danger'" :value="data.active ? 'Ativo' : 'Inativo'" />
          </template>
        </Column>
        <Column header="Ações">
          <template #body="{ data }">
            <Button v-if="data.active" icon="pi pi-ban" severity="danger" text @click="deactivate(data.id)" />
            <Button v-else icon="pi pi-check" severity="success" text @click="activate(data.id)" />
          </template>
        </Column>
      </DataTable>

      <Dialog v-model:visible="showCreate" header="Novo Usuário" :modal="true" style="width: 420px">
        <form @submit.prevent="submitCreate" class="flex flex-col gap-3">
          <InputText v-model="form.name" placeholder="Nome" required />
          <InputText v-model="form.email" type="email" placeholder="Email" required />
          <Password v-model="form.password" placeholder="Senha" :feedback="false" required />
          <Select v-model="form.role" :options="['ADMIN','TRAINER','STUDENT']" placeholder="Perfil" required />
          <p v-if="formError" class="text-red-500 text-sm">{{ formError }}</p>
          <Button type="submit" label="Criar" :loading="saving" />
        </form>
      </Dialog>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useAdminStore } from '@/stores/admin.store'
import { userService } from '@/services/user.service'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Select from 'primevue/select'

const adminStore = useAdminStore()
const showCreate = ref(false)
const saving = ref(false)
const formError = ref<string | null>(null)
const form = ref({ name: '', email: '', password: '', role: '' })

onMounted(() => adminStore.fetchUsers())

async function deactivate(id: string) {
  await userService.deactivate(id)
  await adminStore.fetchUsers()
}

async function activate(id: string) {
  await userService.activate(id)
  await adminStore.fetchUsers()
}

async function submitCreate() {
  saving.value = true; formError.value = null
  try {
    await userService.create(form.value as any)
    showCreate.value = false
    form.value = { name: '', email: '', password: '', role: '' }
    await adminStore.fetchUsers()
  } catch (e: any) {
    formError.value = e.response?.data?.message ?? 'Erro ao criar usuário'
  } finally { saving.value = false }
}
</script>
```

- [ ] **Step 2: Create `ClassesView.vue`**

```vue
<!-- frontend/src/views/admin/ClassesView.vue -->
<template>
  <AppLayout>
    <div class="p-6">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold">Turmas</h1>
        <Button label="Nova turma" icon="pi pi-plus" @click="showCreate = true" />
      </div>

      <DataTable :value="adminStore.classes" :loading="adminStore.loading" stripedRows>
        <Column field="name" header="Nome" />
        <Column field="capacity" header="Capacidade" />
        <Column field="durationMinutes" header="Duração (min)" />
        <Column header="Status">
          <template #body="{ data }">
            <Tag :severity="data.active ? 'success' : 'danger'" :value="data.active ? 'Ativa' : 'Inativa'" />
          </template>
        </Column>
        <Column header="Ações">
          <template #body="{ data }">
            <Button v-if="data.active" icon="pi pi-trash" severity="danger" text @click="deactivate(data.id)" />
          </template>
        </Column>
      </DataTable>

      <Dialog v-model:visible="showCreate" header="Nova Turma" :modal="true" style="width: 420px">
        <form @submit.prevent="submitCreate" class="flex flex-col gap-3">
          <InputText v-model="form.name" placeholder="Nome" required />
          <InputNumber v-model="form.capacity" placeholder="Capacidade" :min="1" required />
          <InputNumber v-model="form.durationMinutes" placeholder="Duração (minutos)" :min="1" required />
          <p v-if="formError" class="text-red-500 text-sm">{{ formError }}</p>
          <Button type="submit" label="Criar" :loading="saving" />
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
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'

const adminStore = useAdminStore()
const authStore = useAuthStore()
const showCreate = ref(false)
const saving = ref(false)
const formError = ref<string | null>(null)
const form = ref({ name: '', capacity: 10, durationMinutes: 60 })

onMounted(() => adminStore.fetchClasses())

async function deactivate(id: string) {
  await groupClassService.deactivate(id)
  await adminStore.fetchClasses()
}

async function submitCreate() {
  saving.value = true; formError.value = null
  try {
    await groupClassService.create({ ...form.value, trainerId: authStore.userId! })
    showCreate.value = false
    await adminStore.fetchClasses()
  } catch (e: any) {
    formError.value = e.response?.data?.message ?? 'Erro ao criar turma'
  } finally { saving.value = false }
}
</script>
```

- [ ] **Step 3: Create `SchedulesView.vue` (Admin)**

```vue
<!-- frontend/src/views/admin/SchedulesView.vue -->
<template>
  <AppLayout>
    <div class="p-6">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold">Agenda</h1>
        <Button label="Novo horário" icon="pi pi-plus" @click="showCreate = true" />
      </div>

      <div class="flex gap-3 mb-4">
        <DatePicker v-model="filterDate" placeholder="Filtrar por data" dateFormat="yy-mm-dd"
          @update:modelValue="loadSchedules" showButtonBar />
        <Select v-model="filterType" :options="['CLASS','PERSONAL']" placeholder="Tipo" showClear
          @update:modelValue="loadSchedules" />
      </div>

      <DataTable :value="scheduleStore.schedules" :loading="scheduleStore.loading" stripedRows>
        <Column field="type" header="Tipo" />
        <Column header="Início">
          <template #body="{ data }">{{ formatDate(data.startsAt) }}</template>
        </Column>
        <Column header="Fim">
          <template #body="{ data }">{{ formatDate(data.endsAt) }}</template>
        </Column>
        <Column field="status" header="Status" />
        <Column header="Ações">
          <template #body="{ data }">
            <Button v-if="data.status !== 'CANCELLED'" icon="pi pi-times" severity="danger" text
              @click="cancelSchedule(data.id)" />
          </template>
        </Column>
      </DataTable>

      <Dialog v-model:visible="showCreate" header="Novo Horário" :modal="true" style="width: 460px">
        <form @submit.prevent="submitCreate" class="flex flex-col gap-3">
          <Select v-model="form.type" :options="['CLASS','PERSONAL']" placeholder="Tipo" required />
          <label class="text-sm font-medium">Início</label>
          <DatePicker v-model="form.startsAt" showTime hourFormat="24" dateFormat="yy-mm-dd" required />
          <label class="text-sm font-medium">Fim</label>
          <DatePicker v-model="form.endsAt" showTime hourFormat="24" dateFormat="yy-mm-dd" required />
          <p v-if="formError" class="text-red-500 text-sm">{{ formError }}</p>
          <Button type="submit" label="Criar" :loading="saving" />
        </form>
      </Dialog>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useScheduleStore } from '@/stores/schedule.store'
import { useAuthStore } from '@/stores/auth.store'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Dialog from 'primevue/dialog'
import DatePicker from 'primevue/datepicker'
import Select from 'primevue/select'

const scheduleStore = useScheduleStore()
const authStore = useAuthStore()
const showCreate = ref(false)
const saving = ref(false)
const formError = ref<string | null>(null)
const filterDate = ref<Date | null>(null)
const filterType = ref<string | null>(null)
const form = ref({ type: '', startsAt: null as Date | null, endsAt: null as Date | null })

onMounted(() => loadSchedules())

function loadSchedules() {
  scheduleStore.fetchSchedules({
    date: filterDate.value ? filterDate.value.toISOString().split('T')[0] : undefined,
    type: filterType.value ?? undefined,
  })
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleString('pt-BR')
}

async function cancelSchedule(id: string) {
  await scheduleStore.cancelSchedule(id)
}

async function submitCreate() {
  saving.value = true; formError.value = null
  try {
    await scheduleStore.createSchedule({
      trainerId: authStore.userId!,
      type: form.value.type,
      startsAt: form.value.startsAt!.toISOString(),
      endsAt: form.value.endsAt!.toISOString(),
    })
    showCreate.value = false
  } catch (e: any) {
    formError.value = e.response?.data?.message ?? 'Erro ao criar horário'
  } finally { saving.value = false }
}
</script>
```

- [ ] **Step 4: Create `trainer/ScheduleView.vue`**

```vue
<!-- frontend/src/views/trainer/ScheduleView.vue -->
<template>
  <AppLayout>
    <div class="p-6">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold">Minha Agenda</h1>
        <Button label="Novo horário" icon="pi pi-plus" @click="showCreate = true" />
      </div>

      <DatePicker v-model="filterDate" placeholder="Selecione uma data" dateFormat="yy-mm-dd"
        showButtonBar @update:modelValue="loadSchedules" class="mb-4" />

      <DataTable :value="scheduleStore.schedules" :loading="scheduleStore.loading" stripedRows>
        <Column field="type" header="Tipo" />
        <Column header="Início">
          <template #body="{ data }">{{ new Date(data.startsAt).toLocaleString('pt-BR') }}</template>
        </Column>
        <Column header="Fim">
          <template #body="{ data }">{{ new Date(data.endsAt).toLocaleString('pt-BR') }}</template>
        </Column>
        <Column field="status" header="Status" />
        <Column header="Ações">
          <template #body="{ data }">
            <Button v-if="data.status !== 'CANCELLED'" icon="pi pi-times" severity="danger" text
              @click="scheduleStore.cancelSchedule(data.id)" />
          </template>
        </Column>
      </DataTable>

      <Dialog v-model:visible="showCreate" header="Novo Horário" :modal="true" style="width: 440px">
        <form @submit.prevent="submitCreate" class="flex flex-col gap-3">
          <Select v-model="form.type" :options="['CLASS','PERSONAL']" placeholder="Tipo" required />
          <DatePicker v-model="form.startsAt" showTime hourFormat="24" dateFormat="yy-mm-dd" placeholder="Início" required />
          <DatePicker v-model="form.endsAt" showTime hourFormat="24" dateFormat="yy-mm-dd" placeholder="Fim" required />
          <p v-if="error" class="text-red-500 text-sm">{{ error }}</p>
          <Button type="submit" label="Criar" :loading="saving" />
        </form>
      </Dialog>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useScheduleStore } from '@/stores/schedule.store'
import { useAuthStore } from '@/stores/auth.store'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Dialog from 'primevue/dialog'
import DatePicker from 'primevue/datepicker'
import Select from 'primevue/select'

const scheduleStore = useScheduleStore()
const authStore = useAuthStore()
const showCreate = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)
const filterDate = ref<Date | null>(null)
const form = ref({ type: '', startsAt: null as Date | null, endsAt: null as Date | null })

onMounted(() => loadSchedules())

function loadSchedules() {
  scheduleStore.fetchSchedules({
    trainerId: authStore.userId ?? undefined,
    date: filterDate.value ? filterDate.value.toISOString().split('T')[0] : undefined,
  })
}

async function submitCreate() {
  saving.value = true; error.value = null
  try {
    await scheduleStore.createSchedule({
      trainerId: authStore.userId!,
      type: form.value.type,
      startsAt: form.value.startsAt!.toISOString(),
      endsAt: form.value.endsAt!.toISOString(),
    })
    showCreate.value = false
  } catch (e: any) {
    error.value = e.response?.data?.message ?? 'Erro ao criar horário'
  } finally { saving.value = false }
}
</script>
```

- [ ] **Step 5: Create `trainer/StudentsView.vue`**

```vue
<!-- frontend/src/views/trainer/StudentsView.vue -->
<template>
  <AppLayout>
    <div class="p-6">
      <h1 class="text-2xl font-bold mb-6">Alunos</h1>
      <InputText v-model="search" placeholder="Buscar por nome..." class="mb-4 w-full" />
      <DataTable :value="filteredStudents" :loading="adminStore.loading" stripedRows>
        <Column field="name" header="Nome" />
        <Column field="email" header="Email" />
        <Column header="Ações">
          <template #body="{ data }">
            <RouterLink :to="`/trainer/students/${data.id}/overview`">
              <Button icon="pi pi-eye" text label="Ver" />
            </RouterLink>
          </template>
        </Column>
      </DataTable>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useAdminStore } from '@/stores/admin.store'
import { RouterLink } from 'vue-router'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import InputText from 'primevue/inputtext'

const adminStore = useAdminStore()
const search = ref('')

onMounted(() => adminStore.fetchUsers())

const filteredStudents = computed(() =>
  adminStore.users
    .filter(u => u.role === 'STUDENT' && u.active)
    .filter(u => u.name.toLowerCase().includes(search.value.toLowerCase()))
)
</script>
```

- [ ] **Step 6: Create `trainer/StudentOverviewView.vue`**

```vue
<!-- frontend/src/views/trainer/StudentOverviewView.vue -->
<template>
  <AppLayout>
    <div class="p-6 max-w-4xl">
      <Button icon="pi pi-arrow-left" text label="Voltar" @click="$router.back()" class="mb-4" />
      <h1 class="text-2xl font-bold mb-6">Visão do Aluno</h1>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <!-- Assessments -->
        <div class="card">
          <div class="flex justify-between items-center mb-3">
            <h2 class="text-lg font-semibold">Avaliações</h2>
            <Button icon="pi pi-plus" text size="small" @click="showAssessment = true" />
          </div>
          <div v-if="assessmentStore.assessments.length === 0" class="text-surface-400 text-sm">Nenhuma avaliação.</div>
          <div v-for="a in assessmentStore.assessments" :key="a.id" class="text-sm py-1 border-b">
            {{ a.assessedAt }} — Peso: {{ a.weightKg ?? '—' }} kg, BF: {{ a.bodyFatPct ?? '—' }}%
          </div>
        </div>

        <!-- Goals -->
        <div class="card">
          <div class="flex justify-between items-center mb-3">
            <h2 class="text-lg font-semibold">Metas</h2>
            <Button icon="pi pi-plus" text size="small" @click="showGoal = true" />
          </div>
          <div v-if="goals.length === 0" class="text-surface-400 text-sm">Nenhuma meta.</div>
          <div v-for="g in goals" :key="g.id" class="text-sm py-1 border-b flex justify-between">
            <span>{{ g.description }}</span>
            <Tag :severity="goalSeverity(g.status)" :value="g.status" />
          </div>
        </div>
      </div>

      <!-- New Assessment Dialog -->
      <Dialog v-model:visible="showAssessment" header="Nova Avaliação" :modal="true" style="width: 440px">
        <form @submit.prevent="submitAssessment" class="flex flex-col gap-3">
          <InputNumber v-model="aForm.weightKg" placeholder="Peso (kg)" :minFractionDigits="1" />
          <InputNumber v-model="aForm.heightCm" placeholder="Altura (cm)" :minFractionDigits="1" />
          <InputNumber v-model="aForm.bodyFatPct" placeholder="% gordura" :minFractionDigits="1" />
          <Textarea v-model="aForm.notes" placeholder="Notas" rows="2" />
          <Button type="submit" label="Salvar" :loading="saving" />
        </form>
      </Dialog>

      <!-- New Goal Dialog -->
      <Dialog v-model:visible="showGoal" header="Nova Meta" :modal="true" style="width: 400px">
        <form @submit.prevent="submitGoal" class="flex flex-col gap-3">
          <InputText v-model="gForm.description" placeholder="Descrição" required />
          <InputText v-model="gForm.metric" placeholder="Métrica (ex: weight)" />
          <Button type="submit" label="Salvar" :loading="saving" />
        </form>
      </Dialog>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import AppLayout from '@/components/AppLayout.vue'
import { useAssessmentStore } from '@/stores/assessment.store'
import { assessmentService } from '@/services/assessment.service'
import { goalService } from '@/services/goal.service'
import type { Goal } from '@/types/api'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Textarea from 'primevue/textarea'
import Tag from 'primevue/tag'

const route = useRoute()
const studentId = route.params.id as string
const assessmentStore = useAssessmentStore()
const goals = ref<Goal[]>([])
const showAssessment = ref(false)
const showGoal = ref(false)
const saving = ref(false)
const aForm = ref({ weightKg: null as number | null, heightCm: null as number | null, bodyFatPct: null as number | null, notes: '' })
const gForm = ref({ description: '', metric: '' })

onMounted(async () => {
  await assessmentStore.fetchAssessments(studentId)
  goals.value = await goalService.list(studentId)
})

function goalSeverity(status: string) {
  return status === 'ACHIEVED' ? 'success' : status === 'EXPIRED' ? 'danger' : 'info'
}

async function submitAssessment() {
  saving.value = true
  try {
    await assessmentService.create(studentId, {
      assessedAt: new Date().toISOString().split('T')[0],
      weightKg: aForm.value.weightKg ?? undefined,
      heightCm: aForm.value.heightCm ?? undefined,
      bodyFatPct: aForm.value.bodyFatPct ?? undefined,
      notes: aForm.value.notes || undefined,
      measurements: [],
    } as any)
    showAssessment.value = false
    await assessmentStore.fetchAssessments(studentId)
  } finally { saving.value = false }
}

async function submitGoal() {
  saving.value = true
  try {
    const g = await goalService.create(studentId, { description: gForm.value.description, metric: gForm.value.metric || undefined })
    goals.value.unshift(g)
    showGoal.value = false
  } finally { saving.value = false }
}
</script>
```

- [ ] **Step 7: Verify build**

```bash
cd /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit/frontend
npm run build 2>&1 | tail -10
```

Expected: zero TypeScript errors.

- [ ] **Step 8: Commit**

```bash
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit add frontend/src/views/admin/ frontend/src/views/trainer/
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit commit -m "feat: Admin and Trainer views (T-13)"
```

---

## Task 4: Student views (T-13 + T-18)

**Files:**
- Create: `frontend/src/views/student/ScheduleView.vue`
- Create: `frontend/src/views/student/BookingsView.vue`
- Create: `frontend/src/views/student/WorkoutView.vue`
- Create: `frontend/src/views/student/RecordsView.vue`
- Create: `frontend/src/views/student/EvolutionView.vue`
- Create: `frontend/src/views/student/GoalsView.vue`
- Create: `frontend/src/views/student/PhotosView.vue`

- [ ] **Step 1: Create `student/ScheduleView.vue`**

```vue
<!-- frontend/src/views/student/ScheduleView.vue -->
<template>
  <AppLayout>
    <div class="p-6">
      <h1 class="text-2xl font-bold mb-6">Aulas Disponíveis</h1>

      <div class="flex gap-3 mb-4">
        <DatePicker v-model="filterDate" placeholder="Data" dateFormat="yy-mm-dd" showButtonBar
          @update:modelValue="loadSchedules" />
        <Select v-model="filterType" :options="['CLASS','PERSONAL']" placeholder="Tipo" showClear
          @update:modelValue="loadSchedules" />
      </div>

      <div v-if="scheduleStore.loading" class="text-center py-8">Carregando...</div>
      <div v-else class="grid gap-3">
        <div v-for="s in openSchedules" :key="s.id"
             class="card flex items-center justify-between p-4">
          <div>
            <div class="font-semibold">{{ s.type === 'CLASS' ? 'Aula em grupo' : 'Sessão individual' }}</div>
            <div class="text-sm text-surface-500">
              {{ new Date(s.startsAt).toLocaleString('pt-BR') }} — {{ new Date(s.endsAt).toLocaleTimeString('pt-BR') }}
            </div>
          </div>
          <Button label="Reservar" size="small" @click="book(s.id)" :loading="booking === s.id" />
        </div>
        <div v-if="openSchedules.length === 0" class="text-surface-400 text-sm">Nenhum horário disponível.</div>
      </div>
      <p v-if="bookError" class="text-red-500 text-sm mt-3">{{ bookError }}</p>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useScheduleStore } from '@/stores/schedule.store'
import { useBookingStore } from '@/stores/booking.store'
import Button from 'primevue/button'
import DatePicker from 'primevue/datepicker'
import Select from 'primevue/select'

const scheduleStore = useScheduleStore()
const bookingStore = useBookingStore()
const filterDate = ref<Date | null>(null)
const filterType = ref<string | null>(null)
const booking = ref<string | null>(null)
const bookError = ref<string | null>(null)

onMounted(() => loadSchedules())

function loadSchedules() {
  scheduleStore.fetchSchedules({
    date: filterDate.value ? filterDate.value.toISOString().split('T')[0] : undefined,
    type: filterType.value ?? undefined,
  })
}

const openSchedules = computed(() => scheduleStore.schedules.filter(s => s.status === 'OPEN'))

async function book(scheduleId: string) {
  booking.value = scheduleId; bookError.value = null
  try { await bookingStore.createBooking(scheduleId) }
  catch (e: any) { bookError.value = e.response?.data?.message ?? 'Erro ao reservar' }
  finally { booking.value = null }
}
</script>
```

- [ ] **Step 2: Create `student/BookingsView.vue`**

```vue
<!-- frontend/src/views/student/BookingsView.vue -->
<template>
  <AppLayout>
    <div class="p-6">
      <h1 class="text-2xl font-bold mb-6">Minhas Reservas</h1>
      <DataTable :value="bookingStore.bookings" :loading="bookingStore.loading" stripedRows>
        <Column field="scheduleId" header="Horário ID" />
        <Column field="status" header="Status" />
        <Column field="bookedAt" header="Reservado em">
          <template #body="{ data }">{{ new Date(data.bookedAt).toLocaleString('pt-BR') }}</template>
        </Column>
        <Column header="Ações">
          <template #body="{ data }">
            <Button v-if="data.status !== 'CANCELLED'" icon="pi pi-times" severity="danger" text
              label="Cancelar" size="small" @click="cancel(data.id)" />
          </template>
        </Column>
      </DataTable>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useBookingStore } from '@/stores/booking.store'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'

const bookingStore = useBookingStore()
onMounted(() => bookingStore.fetchMyBookings())

async function cancel(id: string) { await bookingStore.cancelBooking(id) }
</script>
```

- [ ] **Step 3: Create `student/WorkoutView.vue`**

```vue
<!-- frontend/src/views/student/WorkoutView.vue -->
<template>
  <AppLayout>
    <div class="p-6 max-w-3xl">
      <h1 class="text-2xl font-bold mb-6">Meu Treino</h1>

      <div v-if="workoutStore.loading" class="text-center py-8">Carregando...</div>
      <div v-else-if="!workoutStore.activePlan" class="text-surface-400">Nenhuma ficha ativa.</div>
      <div v-else>
        <h2 class="text-lg font-semibold mb-3">{{ workoutStore.activePlan.name }}</h2>
        <div v-for="item in workoutStore.activePlan.items" :key="item.id"
             class="card p-4 mb-3 flex items-center justify-between">
          <div>
            <div class="font-medium">Exercício #{{ item.exerciseId.slice(0,8) }}</div>
            <div class="text-sm text-surface-500">
              {{ item.division ? `Divisão ${item.division} — ` : '' }}
              {{ item.sets }}x{{ item.reps }}
              {{ item.suggestedLoadKg ? ` @ ${item.suggestedLoadKg}kg` : '' }}
            </div>
          </div>
          <Button icon="pi pi-plus" text size="small" label="Registrar" @click="openLog(item)" />
        </div>
      </div>

      <Dialog v-model:visible="showLog" header="Registrar Execução" :modal="true" style="width: 380px">
        <form @submit.prevent="submitLog" class="flex flex-col gap-3">
          <InputNumber v-model="logForm.sets" placeholder="Séries" :min="1" />
          <InputNumber v-model="logForm.reps" placeholder="Repetições" :min="1" />
          <InputNumber v-model="logForm.loadKg" placeholder="Carga (kg)" :minFractionDigits="1" />
          <InputText v-model="logForm.notes" placeholder="Notas (opcional)" />
          <p v-if="logError" class="text-red-500 text-sm">{{ logError }}</p>
          <Button type="submit" label="Salvar" :loading="saving" />
        </form>
      </Dialog>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useWorkoutStore } from '@/stores/workout.store'
import { useAuthStore } from '@/stores/auth.store'
import { workoutService } from '@/services/workout.service'
import type { WorkoutPlanItem } from '@/types/api'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'

const workoutStore = useWorkoutStore()
const authStore = useAuthStore()
const showLog = ref(false)
const saving = ref(false)
const logError = ref<string | null>(null)
const currentItem = ref<WorkoutPlanItem | null>(null)
const logForm = ref({ sets: null as number | null, reps: null as number | null, loadKg: null as number | null, notes: '' })

onMounted(() => workoutStore.fetchActivePlan(authStore.userId!))

function openLog(item: WorkoutPlanItem) { currentItem.value = item; showLog.value = true }

async function submitLog() {
  if (!currentItem.value) return
  saving.value = true; logError.value = null
  try {
    await workoutService.createLog(authStore.userId!, {
      exerciseId: currentItem.value.exerciseId,
      workoutPlanItemId: currentItem.value.id,
      performedAt: new Date().toISOString().split('T')[0],
      sets: logForm.value.sets ?? undefined,
      reps: logForm.value.reps ?? undefined,
      loadKg: logForm.value.loadKg ?? undefined,
      notes: logForm.value.notes || undefined,
    })
    showLog.value = false
    logForm.value = { sets: null, reps: null, loadKg: null, notes: '' }
  } catch (e: any) {
    logError.value = e.response?.data?.message ?? 'Erro ao registrar'
  } finally { saving.value = false }
}
</script>
```

- [ ] **Step 4: Create `student/RecordsView.vue`**

```vue
<!-- frontend/src/views/student/RecordsView.vue -->
<template>
  <AppLayout>
    <div class="p-6">
      <h1 class="text-2xl font-bold mb-6">Recordes Pessoais</h1>
      <DataTable :value="workoutStore.records" :loading="workoutStore.loading" stripedRows>
        <Column header="Exercício">
          <template #body="{ data }">{{ data.exerciseId.slice(0, 8) }}...</template>
        </Column>
        <Column header="Carga">
          <template #body="{ data }">{{ data.recordLoadKg }} kg</template>
        </Column>
        <Column header="Conquistado em">
          <template #body="{ data }">{{ data.achievedAt }}</template>
        </Column>
      </DataTable>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useWorkoutStore } from '@/stores/workout.store'
import { useAuthStore } from '@/stores/auth.store'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'

const workoutStore = useWorkoutStore()
const authStore = useAuthStore()
onMounted(() => workoutStore.fetchRecords(authStore.userId!))
</script>
```

- [ ] **Step 5: Create `student/EvolutionView.vue`**

```vue
<!-- frontend/src/views/student/EvolutionView.vue -->
<template>
  <AppLayout>
    <div class="p-6 max-w-4xl">
      <h1 class="text-2xl font-bold mb-6">Evolução</h1>

      <div v-if="assessmentStore.loading" class="text-center py-8">Carregando...</div>
      <div v-else-if="assessmentStore.evolution.length === 0" class="text-surface-400">
        Nenhuma avaliação registrada.
      </div>
      <div v-else class="grid gap-6">
        <div class="card p-4">
          <h2 class="text-lg font-semibold mb-3">Peso (kg)</h2>
          <Chart type="line" :data="weightChart" :options="chartOptions" />
        </div>
        <div class="card p-4">
          <h2 class="text-lg font-semibold mb-3">% Gordura Corporal</h2>
          <Chart type="line" :data="bfChart" :options="chartOptions" />
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useAssessmentStore } from '@/stores/assessment.store'
import { useAuthStore } from '@/stores/auth.store'
import Chart from 'primevue/chart'

const assessmentStore = useAssessmentStore()
const authStore = useAuthStore()

onMounted(() => assessmentStore.fetchAssessments(authStore.userId!))

const labels = computed(() => assessmentStore.evolution.map(p => p.assessedAt))

const weightChart = computed(() => ({
  labels: labels.value,
  datasets: [{
    label: 'Peso (kg)',
    data: assessmentStore.evolution.map(p => p.weightKg),
    borderColor: '#6366f1',
    tension: 0.3,
    fill: false,
  }]
}))

const bfChart = computed(() => ({
  labels: labels.value,
  datasets: [{
    label: '% Gordura',
    data: assessmentStore.evolution.map(p => p.bodyFatPct),
    borderColor: '#f59e0b',
    tension: 0.3,
    fill: false,
  }]
}))

const chartOptions = { responsive: true, plugins: { legend: { display: true } } }
</script>
```

- [ ] **Step 6: Create `student/GoalsView.vue`**

```vue
<!-- frontend/src/views/student/GoalsView.vue -->
<template>
  <AppLayout>
    <div class="p-6">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold">Minhas Metas</h1>
        <Button label="Nova meta" icon="pi pi-plus" @click="showCreate = true" />
      </div>

      <div v-if="loading" class="text-center py-8">Carregando...</div>
      <div v-else class="grid gap-3">
        <div v-for="g in goals" :key="g.id" class="card p-4 flex items-center justify-between">
          <div>
            <div class="font-medium">{{ g.description }}</div>
            <div class="text-sm text-surface-500">
              {{ g.metric ?? '' }} {{ g.targetValue ? `— Alvo: ${g.targetValue}` : '' }}
              {{ g.targetDate ? ` até ${g.targetDate}` : '' }}
            </div>
          </div>
          <Tag :severity="goalSeverity(g.status)" :value="g.status" />
        </div>
        <div v-if="goals.length === 0" class="text-surface-400 text-sm">Nenhuma meta registrada.</div>
      </div>

      <Dialog v-model:visible="showCreate" header="Nova Meta" :modal="true" style="width: 400px">
        <form @submit.prevent="submitCreate" class="flex flex-col gap-3">
          <InputText v-model="form.description" placeholder="Descrição" required />
          <InputText v-model="form.metric" placeholder="Métrica (ex: weight, body_fat)" />
          <InputNumber v-model="form.targetValue" placeholder="Valor alvo" :minFractionDigits="1" />
          <Button type="submit" label="Salvar" :loading="saving" />
        </form>
      </Dialog>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { goalService } from '@/services/goal.service'
import { useAuthStore } from '@/stores/auth.store'
import type { Goal } from '@/types/api'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Tag from 'primevue/tag'

const authStore = useAuthStore()
const goals = ref<Goal[]>([])
const loading = ref(false)
const showCreate = ref(false)
const saving = ref(false)
const form = ref({ description: '', metric: '', targetValue: null as number | null })

onMounted(async () => {
  loading.value = true
  try { goals.value = await goalService.list(authStore.userId!) }
  finally { loading.value = false }
})

function goalSeverity(status: string) {
  return status === 'ACHIEVED' ? 'success' : status === 'EXPIRED' ? 'danger' : 'info'
}

async function submitCreate() {
  saving.value = true
  try {
    const g = await goalService.create(authStore.userId!, {
      description: form.value.description,
      metric: form.value.metric || undefined,
      targetValue: form.value.targetValue ?? undefined,
    })
    goals.value.unshift(g)
    showCreate.value = false
    form.value = { description: '', metric: '', targetValue: null }
  } finally { saving.value = false }
}
</script>
```

- [ ] **Step 7: Create `student/PhotosView.vue`**

```vue
<!-- frontend/src/views/student/PhotosView.vue -->
<template>
  <AppLayout>
    <div class="p-6">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold">Fotos de Progresso</h1>
        <Button label="Adicionar foto" icon="pi pi-upload" @click="triggerFileInput" />
      </div>
      <input ref="fileInput" type="file" accept="image/*" class="hidden" @change="handleFileSelect" />

      <div v-if="progressStore.loading" class="text-center py-8">Carregando...</div>
      <div v-else class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
        <div v-for="photo in progressStore.photos" :key="photo.id" class="relative group">
          <img :src="photo.fileUrl" :alt="photo.notes ?? 'Foto'" class="w-full h-40 object-cover rounded-lg" />
          <div class="absolute bottom-0 left-0 right-0 bg-black/50 text-white text-xs p-1 rounded-b-lg">
            {{ photo.takenAt }}
          </div>
          <Button icon="pi pi-trash" severity="danger" text size="small"
            class="absolute top-1 right-1 opacity-0 group-hover:opacity-100 transition-opacity"
            @click="deletePhoto(photo.id)" />
        </div>
        <div v-if="progressStore.photos.length === 0" class="col-span-4 text-surface-400 text-sm">
          Nenhuma foto registrada.
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useProgressStore } from '@/stores/progress.store'
import { useAuthStore } from '@/stores/auth.store'
import Button from 'primevue/button'

const progressStore = useProgressStore()
const authStore = useAuthStore()
const fileInput = ref<HTMLInputElement | null>(null)

onMounted(() => progressStore.fetchPhotos(authStore.userId!))

function triggerFileInput() { fileInput.value?.click() }

async function handleFileSelect(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  await progressStore.uploadPhoto(authStore.userId!, file)
  input.value = ''
}

async function deletePhoto(id: string) { await progressStore.deletePhoto(id) }
</script>
```

- [ ] **Step 8: Final build check**

```bash
cd /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit/frontend
npm run build 2>&1 | tail -10
```

Expected: `built in Xs` — zero TypeScript errors.

- [ ] **Step 9: Run full backend test suite one final time**

```bash
cd /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit/backend
mvn test 2>&1 | tail -5
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 10: Commit**

```bash
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit add frontend/src/views/student/
git -C /Users/marceloferreira/developer/.claude/worktrees/feat+etapa-2/wachafit commit -m "feat: Student views — schedule, bookings, workout, records, evolution, goals, photos (T-13+T-18)"
```
