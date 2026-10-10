import type { ReactNode } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import type { Role } from '../api/types'
import { useAuth } from './AuthContext'
import { Forbidden } from '../components/Forbidden'

/**
 * Route guard. Not signed in -> /login (remembering where you wanted to go).
 * Signed in but missing a role -> "no permission" page.
 * This is for convenience only: the backend still checks every request.
 */
export function RequireAuth({ roles, children }: { roles?: Role[]; children?: ReactNode }) {
  const { user, hasRole } = useAuth()
  const location = useLocation()

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  }
  if (roles && !hasRole(...roles)) return <Forbidden />
  return children ?? <Outlet />
}
