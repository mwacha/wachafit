import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/services/api'
import type { Role, LoginResponse, LoginResult, LoginNeedsTenantSelection, TenantMembershipSummary, SignupRequest } from '@/types/api'

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
  const tenantId = ref<string | null>(localStorage.getItem('tenantId'))

  const isAuthenticated = computed(() => token.value !== null)
  const userRole = computed(() => role.value)

  function setSession(data: LoginResponse) {
    token.value = data.token
    userId.value = data.userId
    role.value = data.role
    tenantId.value = data.tenantId
    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', data.userId)
    localStorage.setItem('role', data.role)
    localStorage.setItem('tenantId', data.tenantId)
  }

  function clearSession() {
    token.value = null
    userId.value = null
    role.value = null
    tenantId.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('role')
    localStorage.removeItem('tenantId')
  }

  async function login(email: string, password: string): Promise<LoginResult> {
    const { data } = await api.post<any>('/api/auth/login', { email, password })
    if (data.token) {
      setSession(data as LoginResponse)
      return data as LoginResponse
    }
    return data as LoginNeedsTenantSelection
  }

  async function selectTenant(selectTenantToken: string, tenantId: string): Promise<LoginResponse> {
    const { data } = await api.post<LoginResponse>('/api/auth/select-tenant', { selectTenantToken, tenantId })
    setSession(data)
    return data
  }

  async function switchTenant(tenantId: string): Promise<LoginResponse> {
    const { data } = await api.post<LoginResponse>('/api/auth/switch-tenant', { tenantId })
    setSession(data)
    return data
  }

  async function myTenants(): Promise<TenantMembershipSummary[]> {
    const { data } = await api.get<TenantMembershipSummary[]>('/api/auth/my-tenants')
    return data
  }

  async function register(name: string, email: string, password: string, tenantSlug: string): Promise<LoginResponse> {
    const { data } = await api.post<LoginResponse>('/api/auth/register', { name, email, password, tenantSlug })
    setSession(data)
    return data
  }

  async function signup(payload: SignupRequest): Promise<LoginResponse> {
    const { data } = await api.post<LoginResponse>('/api/public/signup', payload)
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

  return {
    token, userId, role, tenantId, isAuthenticated, userRole,
    login, selectTenant, switchTenant, myTenants, register, signup, logout, clearSession,
  }
})
