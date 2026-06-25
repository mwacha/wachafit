import type { Role } from '@/types/api'

export const roleDashboards: Record<Role, string> = {
  ADMIN: '/admin',
  TRAINER: '/trainer',
  STUDENT: '/student',
}

export const publicAuthPaths = ['/login', '/register', '/forgot-password', '/reset-password']
