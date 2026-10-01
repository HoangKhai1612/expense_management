import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { ApiError, api, readStoredUser, storeSession, storedToken, clearStoredSession } from '../api/client'
import type { UserSummary } from '../api/types'

interface AuthState {
  token: string | null
  user: UserSummary | null
  initialising: boolean
  error: string | null
  login: (identifier: string, password: string) => Promise<void>
  logout: () => void
  clearError: () => void
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => storedToken())
  const [user, setUser] = useState<UserSummary | null>(() => readStoredUser())
  const [initialising, setInitialising] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // A stored token is not proof of a valid session: the backend may have locked the
  // account, or the token may have expired. Verify it once against a cheap route so
  // a stale session is dropped here rather than surfacing as a 401 mid-screen.
  useEffect(() => {
    if (!token) {
      setInitialising(false)
      return
    }
    let cancelled = false
    api
      .categories()
      .then(() => {
        if (!cancelled) setInitialising(false)
      })
      .catch((err: unknown) => {
        if (cancelled) return
        if (err instanceof ApiError && (err.status === 401 || err.status === 403)) {
          clearStoredSession()
          setToken(null)
          setUser(null)
        }
      })
      .finally(() => {
        if (!cancelled) setInitialising(false)
      })
    return () => {
      cancelled = true
    }
  }, [token])

  const login = useCallback(async (identifier: string, password: string) => {
    setError(null)
    try {
      const response = await api.login(identifier, password)
      storeSession(response.accessToken, response.user)
      setToken(response.accessToken)
      setUser(response.user)
    } catch (err) {
      const message =
        err instanceof ApiError
          ? err.code === 'ACCOUNT_LOCKED'
            ? 'This account is locked. Contact an administrator.'
            : err.code === 'ACCOUNT_DEACTIVATED'
              ? 'This account has been deactivated.'
              : err.message
          : 'Unable to reach the server.'
      setError(message)
      throw err
    }
  }, [])

  const logout = useCallback(() => {
    clearStoredSession()
    setToken(null)
    setUser(null)
  }, [])

  const value = useMemo<AuthState>(
    () => ({ token, user, initialising, error, login, logout, clearError: () => setError(null) }),
    [token, user, initialising, error, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthState {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used inside an AuthProvider')
  }
  return context
}