/**
 * Response shapes mirrored from the Spring Boot API.
 *
 * These are declared by hand rather than generated so that a drift between the
 * backend and this client shows up as a TypeScript error at build time instead of
 * as an undefined field at runtime.
 */

export type UserStatus = 'ACTIVE' | 'LOCKED' | 'DEACTIVATED'
export type CategoryType = 'INCOME' | 'EXPENSE'
export type FeedbackStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED'
export type FeedbackCategory = 'BUG' | 'FEATURE' | 'UI' | 'PERFORMANCE' | 'OTHER'

/**
 * Nullable fields are marked optional on purpose.
 *
 * Jackson is configured with `default-property-inclusion: non_null`, so a null
 * value is omitted from the payload entirely rather than serialised as `null`.
 * Treating these as required-but-nullable would make every consumer handle a
 * distinction the wire format never makes.
 */

export interface UserSummary {
  id: number
  email: string
  username: string
  fullName?: string | null
  phone?: string | null
  role: string
  status: UserStatus
  lastLoginAt?: string | null
  createdAt: string
}

export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: UserSummary
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
  empty: boolean
}

export interface AdminUserView {
  id: number
  email: string
  username: string
  fullName?: string | null
  role: string
  status: UserStatus
  transactionCount: number
  lastLoginAt?: string | null
  createdAt: string
}

export interface AdminCategoryView {
  id: number
  name: string
  code: string
  type: CategoryType
  icon?: string | null
  color?: string | null
  active: boolean
  usageCount: number
}

export interface AdminFeedbackView {
  id: number
  userId: number
  userEmail: string
  title: string
  content: string
  category: FeedbackCategory
  status: FeedbackStatus
  adminReply?: string | null
  createdAt: string
  updatedAt: string
}

export interface AuditView {
  id: number
  adminId?: number | null
  adminName?: string | null
  action: string
  targetType?: string | null
  targetId?: number | null
  result: string
  detail?: string | null
  ipAddress?: string | null
  createdAt: string
}

export interface RuntimeMetrics {
  [key: string]: unknown
  uptimeSinceRestart: boolean
}

export interface AdminDashboard {
  totalUsers: number
  activeUsers: number
  lockedUsers: number
  deactivatedUsers: number
  newUsersLast7Days: number
  totalTransactions: number
  systemCategories: number
  personalCategories: number
  feedbackByStatus: Record<string, number>
  ai: {
    engine: string
    providerAvailable: boolean
    totalQuestions: number
  }
  runtime: RuntimeMetrics
}

/** The error envelope emitted by GlobalExceptionHandler. */
export interface ApiErrorBody {
  timestamp: string
  status: number
  code: string
  message: string
  path: string
  violations?: { field: string; message: string }[]
}