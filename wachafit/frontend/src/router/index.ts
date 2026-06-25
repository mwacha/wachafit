import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
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
      meta: { requiresAuth: true, roles: ['ADMIN'] },
    },
    { path: '/', redirect: '/login' },
    { path: '/:pathMatch(.*)*', redirect: '/login' },
  ],
})

router.beforeEach(to => {
  const auth = useAuthStore()
  const dashboards: Record<Role, string> = { ADMIN: '/admin', TRAINER: '/trainer', STUDENT: '/student' }

  // Authenticated user hitting public auth pages → redirect to dashboard
  if ((to.path === '/login' || to.path === '/register') && auth.isAuthenticated) {
    return dashboards[auth.role!]
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
