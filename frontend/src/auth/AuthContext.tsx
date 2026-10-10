import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import type { Role } from '../api/types'
import { refreshSession, revokeRefreshToken, setTokenRefreshedHandler, setUnauthorizedHandler } from '../api/client'
import { decodeJwt, isExpired } from './jwt'
import { tokenStorage } from './tokenStorage'

export interface AuthUser {
  email: string
  userId: string
  roles: Role[]
  expiresAt: number // ms
}

interface AuthContextValue {
  user: AuthUser | null
  /** Store tokens from login/register/Google. Returns the user, or null if the access token is unusable. */
  signIn: (token: string, refreshToken?: string | null) => AuthUser | null
  signOut: () => void
  hasRole: (...roles: Role[]) => boolean
}

const AuthContext = createContext<AuthContextValue | null>(null)

function userFromToken(token: string | null): AuthUser | null {
  if (!token) return null
  const payload = decodeJwt(token)
  if (!payload || isExpired(payload)) return null
  return { email: payload.sub, userId: payload.userId, roles: payload.roles, expiresAt: payload.exp * 1000 }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  // On first load, read the saved token. An expired or broken token counts as logged out.
  const [token, setToken] = useState<string | null>(() => {
    const saved = tokenStorage.get()
    if (userFromToken(saved)) return saved
    // Keep a refresh token so the effect below can ask for a new access token.
    if (!tokenStorage.getRefresh()) tokenStorage.clear()
    return null
  })
  const user = useMemo(() => userFromToken(token), [token])

  const signIn = useCallback((newToken: string, refreshToken?: string | null) => {
    const newUser = userFromToken(newToken)
    if (!newUser) return null
    tokenStorage.set(newToken)
    if (refreshToken) tokenStorage.setRefresh(refreshToken)
    queryClient.clear() // never show cached data from a previous user
    setToken(newToken)
    return newUser
  }, [queryClient])

  const signOut = useCallback(() => {
    revokeRefreshToken()
    tokenStorage.clear()
    queryClient.clear()
    setToken(null)
  }, [queryClient])

  // Any 401 the client could not repair -> sign out and go to /login.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      queryClient.clear()
      setToken(null)
      navigate('/login?expired=1', { replace: true })
    })
    setTokenRefreshedHandler((next) => setToken(next))
  }, [navigate, queryClient])

  // Access token missing but a refresh token is saved: ask for a new access token.
  useEffect(() => {
    if (token || !tokenStorage.getRefresh()) return
    let cancelled = false
    refreshSession().then((next) => {
      if (cancelled) return
      if (next) setToken(next)
      else {
        tokenStorage.clear()
        setToken(null)
      }
    })
    return () => {
      cancelled = true
    }
  }, [token])

  // Renew the access token shortly before it expires. Without a refresh token, sign out at expiry.
  useEffect(() => {
    if (!user) return
    const hasRefresh = !!tokenStorage.getRefresh()
    const lead = hasRefresh ? 30_000 : 0
    const ms = Math.min(Math.max(user.expiresAt - Date.now() - lead, 0), 2_147_483_647)
    const id = setTimeout(() => {
      if (!hasRefresh) {
        signOut()
        navigate('/login?expired=1', { replace: true })
        return
      }
      refreshSession().then((next) => {
        if (!next) {
          signOut()
          navigate('/login?expired=1', { replace: true })
        }
      })
    }, ms)
    return () => clearTimeout(id)
  }, [user, signOut, navigate])

  const hasRole = useCallback((...roles: Role[]) => !!user && roles.some((r) => user.roles.includes(r)), [user])

  const value = useMemo(() => ({ user, signIn, signOut, hasRole }), [user, signIn, signOut, hasRole])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>')
  return ctx
}
