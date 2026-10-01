import type {
  AdminCategoryView,
  AdminDashboard,
  AdminFeedbackView,
  AdminUserView,
  ApiErrorBody,
  AuditView,
  AuthResponse,
  FeedbackStatus,
  PageResponse,
  UserStatus,
  UserSummary,
} from './types'

/**
 * Single fetch wrapper for the whole admin app.
 *
 * Two decisions are load-bearing:
 *  - the base URL is fixed at build time, so the same bundle works in dev (through
 *    the Vite proxy, empty base) and in Docker (absolute URL to the backend);
 *  - a non-2xx response is turned into a typed ApiError carrying the backend's
 *    machine code, so the UI can react to ACCOUNT_LOCKED or BUDGET_ALREADY_EXISTS
 *    rather than string-matching a message.
 */
const BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')

const TOKEN_KEY = 'finai.admin.token'
const USER_KEY = 'finai.admin.user'

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly violations: { field: string; message: string }[]

  constructor(status: number, code: string, message: string, violations: { field: string; message: string }[] = []) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.violations = violations
  }
}

export function storedToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function storeToken(token: string | null): void {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

/** The cached profile is a display convenience only; the API is the source of truth. */
export function readStoredUser(): UserSummary | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as UserSummary
  } catch {
    localStorage.removeItem(USER_KEY)
    return null
  }
}

export function storeSession(token: string, user: UserSummary): void {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearStoredSession(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

async function request<T>(path: string, init: RequestInit = {}, token?: string | null): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')
  if (init.body) {
    headers.set('Content-Type', 'application/json')
  }
  const bearer = token ?? storedToken()
  if (bearer) {
    headers.set('Authorization', `Bearer ${bearer}`)
  }

  const response = await fetch(`${BASE_URL}${path}`, { ...init, headers })

  if (response.status === 204) {
    return undefined as T
  }

  const text = await response.text()
  let payload: unknown = null
  if (text) {
    try {
      payload = JSON.parse(text)
    } catch {
      payload = text
    }
  }

  if (!response.ok) {
    const body = payload as ApiErrorBody | string | null
    if (body && typeof body === 'object' && 'code' in body) {
      throw new ApiError(response.status, body.code, body.message, body.violations ?? [])
    }
    throw new ApiError(
      response.status,
      'UNEXPECTED_ERROR',
      typeof body === 'string' && body ? body : `Request failed with status ${response.status}`,
    )
  }

  return payload as T
}

export const api = {
  login(identifier: string, password: string) {
    return request<AuthResponse>(
      '/api/auth/login',
      { method: 'POST', body: JSON.stringify({ identifier, password }) },
      null,
    )
  },

  dashboard() {
    return request<AdminDashboard>('/api/admin/dashboard')
  },

  system() {
    return request<Record<string, unknown>>('/api/admin/system')
  },

  users(params: { search?: string; status?: UserStatus | ''; page?: number; size?: number } = {}) {
    const query = new URLSearchParams()
    if (params.search) query.set('search', params.search)
    if (params.status) query.set('status', params.status)
    query.set('page', String(params.page ?? 0))
    query.set('size', String(params.size ?? 20))
    return request<PageResponse<AdminUserView>>(`/api/admin/users?${query.toString()}`)
  },

  updateUserStatus(id: number, status: UserStatus, reason?: string) {
    return request<AdminUserView>(`/api/admin/users/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status, reason: reason ?? null }),
    })
  },

  categories() {
    return request<AdminCategoryView[]>('/api/admin/categories')
  },

  createCategory(payload: {
    name: string
    code: string
    type: string
    icon?: string | null
    color?: string | null
  }) {
    return request<AdminCategoryView>('/api/admin/categories', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  updateCategory(
    id: number,
    payload: {
      name?: string | null
      type?: string | null
      icon?: string | null
      color?: string | null
      active?: boolean | null
    },
  ) {
    return request<AdminCategoryView>(`/api/admin/categories/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    })
  },

  feedback(params: { status?: FeedbackStatus | ''; page?: number; size?: number } = {}) {
    const query = new URLSearchParams()
    if (params.status) query.set('status', params.status)
    query.set('page', String(params.page ?? 0))
    query.set('size', String(params.size ?? 20))
    return request<PageResponse<AdminFeedbackView>>(`/api/admin/feedback?${query.toString()}`)
  },

  updateFeedback(id: number, status: FeedbackStatus, adminReply?: string | null) {
    return request<AdminFeedbackView>(`/api/admin/feedback/${id}`, {
      method: 'PATCH',
      body: JSON.stringify({ status, adminReply: adminReply ?? null }),
    })
  },

  auditLogs(params: { page?: number; size?: number } = {}) {
    const query = new URLSearchParams()
    query.set('page', String(params.page ?? 0))
    query.set('size', String(params.size ?? 50))
    return request<PageResponse<AuditView>>(`/api/admin/audit-logs?${query.toString()}`)
  },
}