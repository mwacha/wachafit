export type Role = 'ADMIN' | 'TRAINER' | 'STUDENT'

export interface LoginResponse {
  token: string
  role: Role
  userId: string
}

export interface ErrorResponse {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
}
