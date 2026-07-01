export type Role = 'ADMIN' | 'MANAGER' | 'RECEPTIONIST' | 'CASHIER' | 'TRAINER' | 'STUDENT'

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
