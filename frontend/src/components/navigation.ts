import type { Role } from '../api/types'
import type { IconName } from './Icon'

// One list drives both the desktop sidebar and the mobile bottom bar.
// `roles` mirrors the RBAC table in CLAUDE.md section 5.
export interface NavItem {
  to: string
  label: string
  icon: IconName
  roles?: Role[] // undefined = every signed-in user
}

export const NAV_ITEMS: NavItem[] = [
  { to: '/', label: 'Dashboard', icon: 'home' },
  { to: '/athletes', label: 'Athletes', icon: 'athletes', roles: ['ADMIN', 'SCOUT', 'CLUB_MANAGER'] },
  { to: '/assessments/new', label: 'Assess', icon: 'plus', roles: ['SCOUT'] },
  { to: '/ranking', label: 'Ranking', icon: 'ranking', roles: ['ADMIN', 'SCOUT', 'CLUB_MANAGER'] },
  { to: '/shortlists', label: 'Shortlists', icon: 'shortlist', roles: ['ADMIN', 'CLUB_MANAGER'] },
  { to: '/reports', label: 'Reports', icon: 'reports', roles: ['ADMIN', 'SCOUT', 'CLUB_MANAGER'] },
  { to: '/admin/users', label: 'Users', icon: 'users', roles: ['ADMIN'] },
  { to: '/admin/sports', label: 'Sports', icon: 'sports', roles: ['ADMIN'] },
]
