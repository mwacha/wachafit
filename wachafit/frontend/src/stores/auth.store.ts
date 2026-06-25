import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/services/api'
import type { Role, LoginResponse } from '@/types/api'

function decodeJwtPayload(token: string): { sub: string; role: Role } | null {
  try {
    const payload = token.split('.')[1]
    return JSON.parse(atob(payload))
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const userId = ref<string | null>(localStorage.getItem('userId'))
  const role = ref<Role | null>((localStorage.getItem('role') as Role) ?? null)

  const isAuthenticated = computed(() => token.value !== null)
  const userRole = computed(() => role.value)

  function setSession(data: LoginResponse) {
    token.value = data.token
    userId.value = data.userId
    role.value = data.role
    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', data.userId)
    localStorage.setItem('role', data.role)
  }

  function clearSession() {
    token.value = null
    userId.value = null
    role.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('role')
  }

  async function login(email: string, password: string): Promise<LoginResponse> {
    const { data } = await api.post<LoginResponse>('/api/auth/login', { email, password })
    setSession(data)
    return data
  }

  async function register(name: string, email: string, password: string): Promise<LoginResponse> {
    const { data } = await api.post<LoginResponse>('/api/auth/register', { name, email, password })
    setSession(data)
    return data
  }

  function logout() {
    clearSession()
  }

  // Validate stored token on store init: clear if malformed
  if (token.value) {
    const payload = decodeJwtPayload(token.value)
    if (!payload) {
      clearSession()
    }
  }

  return { token, userId, role, isAuthenticated, userRole, login, register, logout, clearSession }
})
