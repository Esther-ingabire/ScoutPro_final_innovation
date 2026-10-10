import { useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ROLE_LABELS } from '../lib/format'
import { Icon } from './Icon'
import { Modal } from './Modal'
import { NAV_ITEMS } from './navigation'

/**
 * App shell. Mobile-first:
 *  - < 768px: slim top bar + bottom navigation (thumb-reachable)
 *  - >= 768px: fixed sidebar on the left
 * Only menu items the user's roles allow are shown.
 */
export default function Layout() {
  const { user, hasRole, signOut } = useAuth()
  const navigate = useNavigate()
  const [moreOpen, setMoreOpen] = useState(false)

  const items = NAV_ITEMS.filter((item) => !item.roles || hasRole(...item.roles))
  // The bottom bar fits 5 buttons. With more items, show 4 + "More".
  const bottomItems = items.length <= 5 ? items : items.slice(0, 4)
  const overflowItems = items.length <= 5 ? [] : items.slice(4)

  function handleSignOut() {
    signOut()
    navigate('/login', { replace: true })
  }

  const roleText = user?.roles.map((r) => ROLE_LABELS[r]).join(', ')

  return (
    <div className="min-h-dvh md:flex">
      <a href="#main" className="sr-only focus:not-sr-only focus:absolute focus:left-2 focus:top-2 focus:z-50 focus:rounded focus:bg-white focus:p-2">
        Skip to content
      </a>

      {/* Desktop sidebar */}
      <aside className="hidden w-64 shrink-0 flex-col bg-emerald-950 text-emerald-50 md:sticky md:top-0 md:flex md:h-dvh">
        <Brand className="px-6 pt-6 pb-8" />
        <nav aria-label="Main" className="flex-1 space-y-1 px-3">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-lg px-3 py-2.5 font-medium focus-visible:outline-2 focus-visible:outline-amber-300 ${
                  isActive ? 'bg-emerald-800 text-white' : 'text-emerald-100 hover:bg-emerald-900'
                }`
              }
            >
              <Icon name={item.icon} />
              {item.to === '/assessments/new' ? 'New assessment' : item.label}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-emerald-900 p-4">
          <p className="truncate text-sm font-medium" title={user?.email}>{user?.email}</p>
          <p className="text-sm text-emerald-300">{roleText}</p>
          <button
            type="button"
            onClick={handleSignOut}
            className="mt-3 flex items-center gap-2 rounded-lg px-2 py-1.5 text-emerald-100 hover:bg-emerald-900 focus-visible:outline-2 focus-visible:outline-amber-300"
          >
            <Icon name="logout" /> Sign out
          </button>
        </div>
      </aside>

      {/* Mobile top bar */}
      <header className="sticky top-0 z-20 flex items-center justify-between bg-emerald-950 px-4 py-3 text-white md:hidden">
        <Brand />
        <button type="button" onClick={handleSignOut} className="flex items-center gap-1 rounded-lg px-2 py-1 text-sm text-emerald-100 focus-visible:outline-2 focus-visible:outline-amber-300">
          <Icon name="logout" className="size-4" /> Sign out
        </button>
      </header>

      <main id="main" className="min-w-0 flex-1 px-4 pt-6 pb-28 md:px-8 md:pb-10">
        <div className="mx-auto max-w-6xl">
          <Outlet />
        </div>
      </main>

      {/* Mobile bottom navigation */}
      <nav aria-label="Main" className="fixed inset-x-0 bottom-0 z-20 border-t border-zinc-200 bg-white pb-[env(safe-area-inset-bottom)] md:hidden">
        <ul className="flex">
          {bottomItems.map((item) => (
            <li key={item.to} className="flex-1">
              <NavLink
                to={item.to}
                end={item.to === '/'}
                className={({ isActive }) =>
                  `flex flex-col items-center gap-0.5 py-2 text-xs font-medium focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-emerald-700 ${
                    isActive ? 'text-emerald-800' : 'text-zinc-600'
                  }`
                }
              >
                <Icon name={item.icon} className="size-6" />
                {item.label}
              </NavLink>
            </li>
          ))}
          {overflowItems.length > 0 && (
            <li className="flex-1">
              <button type="button" onClick={() => setMoreOpen(true)} className="flex w-full flex-col items-center gap-0.5 py-2 text-xs font-medium text-zinc-600 focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-emerald-700">
                <Icon name="more" className="size-6" />
                More
              </button>
            </li>
          )}
        </ul>
      </nav>

      <Modal open={moreOpen} onClose={() => setMoreOpen(false)} title="More">
        <ul className="space-y-1">
          {overflowItems.map((item) => (
            <li key={item.to}>
              <NavLink to={item.to} onClick={() => setMoreOpen(false)} className="flex items-center gap-3 rounded-lg px-3 py-3 font-medium hover:bg-zinc-100 focus-visible:outline-2 focus-visible:outline-emerald-700">
                <Icon name={item.icon} /> {item.label}
              </NavLink>
            </li>
          ))}
        </ul>
        <p className="mt-4 border-t border-zinc-200 pt-3 text-sm text-zinc-600">Signed in as {user?.email} ({roleText})</p>
      </Modal>
    </div>
  )
}

export function Brand({ className = '' }: { className?: string }) {
  return (
    <div className={`flex items-baseline gap-1 font-['Barlow_Condensed',sans-serif] text-2xl font-bold tracking-wide ${className}`}>
      <span>Scout</span>
      <span className="rounded bg-amber-300 px-1.5 text-emerald-950">Pro</span>
    </div>
  )
}
