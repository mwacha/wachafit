export type Role = 'ADMIN' | 'MANAGER' | 'RECEPTIONIST' | 'CASHIER' | 'TRAINER' | 'PROFESSOR' | 'STUDENT'

export interface LoginRequest {
  email: string
  password: string
  tenantSlug: string
}

export interface LoginResponse {
  token: string
  role: Role
  userId: string
  tenantId: string
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
export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED'

export interface BookedStudentSummary {
  studentId: string
  studentName: string
  status: BookingStatus
}

export interface Schedule {
  id: string
  groupClassId: string | null
  groupClassName: string | null
  trainerId: string
  type: ScheduleType
  startsAt: string
  endsAt: string
  status: ScheduleStatus
  createdAt: string
  bookedStudents: BookedStudentSummary[]
}

export interface Booking {
  id: string
  scheduleId: string
  studentId: string
  status: BookingStatus
  bookedAt: string
  startsAt: string
  endsAt: string
  type: ScheduleType
  groupClassName: string | null
  trainerName: string | null
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
  daysOfWeek: string[] | null
  enrolledCount: number
  id: string
  name: string
  description: string | null
  capacity: number
  durationMinutes: number
  trainerId: string
  trainerName: string | null
  active: boolean
  createdAt: string
  scheduleType: 'FIXED' | 'FLEX'
  startTime: string | null
  endTime: string | null
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

// --- Membership ---
export interface MembershipPlan {
  id: string
  name: string
  description: string | null
  durationMonths: number
  price: number
  maxClassesPerWeek: number | null
  active: boolean
  createdAt: string
}

export interface MemberSubscription {
  id: string
  studentId: string
  planId: string
  planName: string
  status: 'ACTIVE' | 'SUSPENDED' | 'CANCELLED'
  startedAt: string
  expiresAt: string
  createdAt: string
}

export interface CreateSubscriptionRequest {
  planId: string
  startedAt: string
}

// --- Billing ---
export interface PaymentCharge {
  id: string
  studentId: string
  subscriptionId: string
  amount: number
  dueDate: string
  status: 'PENDING' | 'PAID' | 'OVERDUE' | 'CANCELLED'
  paidAt: string | null
  paymentMethod: string | null
  externalPaymentUrl: string | null
  createdAt: string
}

export interface CreateChargeRequest {
  amount: number
  dueDate: string
}

export interface ManualPaymentRequest {
  paymentMethod: string
}

// --- Reports ---
export interface RevenueReport {
  month: string
  total: number
  chargesCount: number
}

export interface OverdueStudent {
  studentId: string
  name: string
  totalDue: number
  daysOverdue: number
}

export interface SubscriptionStats {
  active: number
  suspended: number
  cancelled: number
  expired: number
}

export interface TrainerCommission {
  trainerId: string
  name: string
  commissionType: string
  commissionDue: number
  classesCount: number
}

export interface CashFlowDay {
  date: string
  received: number
  pending: number
  overdue: number
}

export interface EnrollmentTrend {
  month: string
  newEnrollments: number
}

export interface AttendanceRank {
  studentName: string
  bookingCount: number
}

// --- Trainer Profile ---
export interface TrainerProfile {
  userId: string
  cref: string | null
  specialties: string | null
  bio: string | null
  profilePhotoKey: string | null
  contractType: 'CLT' | 'PJ' | 'FREELANCE' | null
  commissionType: 'FIXED' | 'PERCENTAGE' | null
  commissionValue: number | null
}

export interface UpdateTrainerProfileRequest {
  cref?: string
  specialties?: string
  bio?: string
  contractType?: string
  commissionType?: string
  commissionValue?: number
}

// --- Student Profile ---
export interface StudentProfile {
  id: string
  userId: string
  cpf: string
  rg: string | null
  birthDate: string | null
  gender: string | null
  maritalStatus: string | null
  profession: string | null
  phone: string | null
  addressZip: string | null
  addressLine: string | null
  addressNumber: string | null
  addressComplement: string | null
  addressNeighborhood: string | null
  addressCity: string | null
  addressState: string | null
  emergencyContactName: string | null
  emergencyContactPhone: string | null
  emergencyContactRelationship: string | null
  createdAt: string
}

export interface StudentHealth {
  id: string
  userId: string
  hasHeartCondition: boolean
  hasDiabetes: boolean
  hasHypertension: boolean
  hasRespiratoryCondition: boolean
  hasOrthopedicCondition: boolean
  hadSurgery: boolean
  surgeryDescription: string | null
  hasChronicPain: boolean
  chronicPainLocation: string | null
  medications: string | null
  physicalRestrictions: string | null
  smokes: boolean
  drinksAlcohol: boolean
  alcoholFrequency: string | null
  sleepHours: number | null
  stressLevel: number | null
  activityLevel: string | null
  fitnessGoal: string | null
  fitnessLevel: string | null
  exerciseHistory: string | null
  parqHeartProblem: boolean
  parqChestPainExercise: boolean
  parqChestPainRest: boolean
  parqDizziness: boolean
  parqBoneJoint: boolean
  parqBloodPressureMeds: boolean
  parqOtherReason: boolean
  parqOtherReasonDetail: string | null
  parqSignedAt: string | null
  notes: string | null
}

// --- Class Enrollment ---
export interface EnrolledStudent {
  studentId: string
  name: string
  email: string
}

export interface EnrolledClass {
  classId: string
  className: string
  trainerName: string
  startTime: string | null
  endTime: string | null
  daysOfWeek: string[]
}

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
