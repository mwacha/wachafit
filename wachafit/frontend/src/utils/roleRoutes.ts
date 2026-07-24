import type { Role } from '@/types/api'

export const roleDashboards: Record<Role, string> = {
  ADMIN: '/admin',
  MANAGER: '/manager',
  CASHIER: '/cashier',
  RECEPTIONIST: '/reception',
  TRAINER: '/trainer',
  PROFESSOR: '/trainer',
  STUDENT: '/student',
}

export const publicAuthPaths = ['/login', '/register', '/signup-academia', '/forgot-password', '/reset-password']
