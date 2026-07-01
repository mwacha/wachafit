import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import { roleDashboards, publicAuthPaths } from '@/utils/roleRoutes'
import type { Role } from '@/types/api'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    roles?: Role[]
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: () => import('@/views/auth/LoginView.vue') },
    { path: '/register', component: () => import('@/views/auth/RegisterView.vue') },
    { path: '/forgot-password', component: () => import('@/views/auth/ForgotPasswordView.vue') },
    { path: '/reset-password', component: () => import('@/views/auth/ResetPasswordView.vue') },
    { path: '/unauthorized', component: () => import('@/views/UnauthorizedView.vue') },
    {
      path: '/student',
      component: () => import('@/views/student/StudentDashboard.vue'),
      meta: { requiresAuth: true, roles: ['STUDENT'] },
    },
    {
      path: '/trainer',
      component: () => import('@/views/trainer/TrainerDashboard.vue'),
      meta: { requiresAuth: true, roles: ['TRAINER'] },
    },
    {
      path: '/admin',
      component: () => import('@/views/admin/AdminDashboard.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN'] as Role[] },
    },
    {
      path: '/manager',
      component: () => import('@/views/manager/ManagerDashboard.vue'),
      meta: { requiresAuth: true, roles: ['MANAGER'] as Role[] },
    },
    {
      path: '/cashier',
      component: () => import('@/views/cashier/CashierDashboard.vue'),
      meta: { requiresAuth: true, roles: ['CASHIER'] as Role[] },
    },
    {
      path: '/reception',
      component: () => import('@/views/receptionist/ReceptionistDashboard.vue'),
      meta: { requiresAuth: true, roles: ['RECEPTIONIST'] as Role[] },
    },

    // --- Admin routes ---
    {
      path: '/admin/users',
      component: () => import('@/views/admin/UsersView.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN', 'MANAGER'] as Role[] },
    },
    {
      path: '/admin/classes',
      component: () => import('@/views/admin/ClassesView.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN', 'MANAGER'] as Role[] },
    },
    {
      path: '/admin/schedules',
      component: () => import('@/views/admin/SchedulesView.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN', 'MANAGER', 'RECEPTIONIST', 'CASHIER', 'TRAINER'] as Role[] },
    },

    // --- Trainer routes ---
    {
      path: '/trainer/schedule',
      component: () => import('@/views/trainer/ScheduleView.vue'),
      meta: { requiresAuth: true, roles: ['TRAINER'] as Role[] },
    },
    {
      path: '/trainer/students',
      component: () => import('@/views/trainer/StudentsView.vue'),
      meta: { requiresAuth: true, roles: ['TRAINER'] as Role[] },
    },
    {
      path: '/trainer/students/:id/overview',
      component: () => import('@/views/trainer/StudentOverviewView.vue'),
      meta: { requiresAuth: true, roles: ['TRAINER'] as Role[] },
    },

    // --- Student routes ---
    {
      path: '/student/schedule',
      component: () => import('@/views/student/ScheduleView.vue'),
      meta: { requiresAuth: true, roles: ['STUDENT'] as Role[] },
    },
    {
      path: '/student/bookings',
      component: () => import('@/views/student/BookingsView.vue'),
      meta: { requiresAuth: true, roles: ['STUDENT'] as Role[] },
    },
    {
      path: '/student/workout',
      component: () => import('@/views/student/WorkoutView.vue'),
      meta: { requiresAuth: true, roles: ['STUDENT'] as Role[] },
    },
    {
      path: '/student/records',
      component: () => import('@/views/student/RecordsView.vue'),
      meta: { requiresAuth: true, roles: ['STUDENT'] as Role[] },
    },
    {
      path: '/student/evolution',
      component: () => import('@/views/student/EvolutionView.vue'),
      meta: { requiresAuth: true, roles: ['STUDENT'] as Role[] },
    },
    {
      path: '/student/goals',
      component: () => import('@/views/student/GoalsView.vue'),
      meta: { requiresAuth: true, roles: ['STUDENT'] as Role[] },
    },
    {
      path: '/student/photos',
      component: () => import('@/views/student/PhotosView.vue'),
      meta: { requiresAuth: true, roles: ['STUDENT'] as Role[] },
    },

    { path: '/', redirect: '/login' },
    { path: '/:pathMatch(.*)*', redirect: '/login' },
  ],
})

router.beforeEach(to => {
  const auth = useAuthStore()

  // Authenticated user on any public-auth path → redirect to their dashboard
  if (publicAuthPaths.includes(to.path) && auth.isAuthenticated) {
    return auth.role ? roleDashboards[auth.role] : '/login'
  }

  // Protected route, not authenticated → login
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return '/login'
  }

  // Role-restricted route, wrong role → unauthorized
  if (to.meta.roles && auth.role && !to.meta.roles.includes(auth.role)) {
    return '/unauthorized'
  }
})

export default router
