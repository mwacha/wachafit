import axios, { type AxiosInstance } from 'axios'
import type { Router } from 'vue-router'

const api: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080',
})

let _router: Router | null = null

export function setRouter(router: Router) {
  _router = router
}

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      // Lazy import to avoid circular dependency at module load time
      import('@/stores/auth.store').then(({ useAuthStore }) => {
        useAuthStore().clearSession()
      })
      _router?.push('/login')
    }
    return Promise.reject(error)
  }
)

export default api
