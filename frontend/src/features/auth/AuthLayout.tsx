import type { ReactNode } from 'react'
import { Brand } from '../../components/Layout'

/** Mobile: one column. Desktop (>=1024px): brand panel on the left, form on the right. */
export function AuthLayout({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="min-h-dvh lg:grid lg:grid-cols-2">
      <aside className="hidden min-h-dvh flex-col justify-between bg-emerald-950 p-12 text-emerald-50 lg:flex">
        <Brand className="text-3xl" />
        <div>
          <p className="font-['Barlow_Condensed',sans-serif] text-5xl leading-none font-bold text-white">
            Assess, rank, and shortlist athletes.
          </p>
          <ul className="mt-8 max-w-md space-y-4 text-lg text-emerald-100">
            <li>Scouts score each skill from 0 to 100. The overall score is the average, also out of 100.</li>
            <li>Rankings keep each athlete's best assessment so clubs can compare them.</li>
            <li>Club managers build shortlists, and a written report can sit with an assessment.</li>
          </ul>
        </div>
        <p className="text-sm text-emerald-300">© 2026 ScoutPro</p>
      </aside>

      <main className="flex min-h-dvh items-center justify-center px-4 py-10 lg:min-h-0">
        <div className="w-full max-w-sm">
          <Brand className="mb-8 text-emerald-950 lg:hidden" />
          <h1 className="font-['Barlow_Condensed',sans-serif] text-4xl font-bold text-zinc-900">{title}</h1>
          <div className="mt-6">{children}</div>
        </div>
      </main>
    </div>
  )
}

export const GOOGLE_LOGIN_URL = 'http://localhost:8080/oauth2/authorization/google'

/**
 * A plain link, not fetch(): Google login is a series of full-page redirects
 * (our backend -> Google -> our backend -> /oauth2/callback). It goes to port 8080
 * directly, NOT through the Vite proxy, so the redirect_uri Google sees matches
 * the one registered in Google Cloud Console.
 */
export function GoogleButton({ label }: { label: string }) {
  return (
    <a
      href={GOOGLE_LOGIN_URL}
      className="flex w-full items-center justify-center gap-3 rounded-lg border border-zinc-300 bg-white px-4 py-3 font-semibold text-zinc-900 hover:bg-zinc-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-700"
    >
      <svg viewBox="0 0 24 24" className="size-5" aria-hidden>
        <path fill="#4285F4" d="M22.6 12.3c0-.8-.1-1.6-.2-2.3H12v4.4h5.9a5 5 0 0 1-2.2 3.3v2.7h3.6c2.1-1.9 3.3-4.8 3.3-8.1" />
        <path fill="#34A853" d="M12 23c3 0 5.5-1 7.3-2.7l-3.6-2.7c-1 .7-2.2 1.1-3.7 1.1-2.9 0-5.3-1.9-6.2-4.5H2.1v2.8A11 11 0 0 0 12 23" />
        <path fill="#FBBC05" d="M5.8 14.2a6.6 6.6 0 0 1 0-4.3V7.1H2.1a11 11 0 0 0 0 9.9z" />
        <path fill="#EA4335" d="M12 5.4c1.6 0 3.1.6 4.2 1.7l3.2-3.2A11 11 0 0 0 2.1 7.1l3.7 2.8C6.7 7.3 9.1 5.4 12 5.4" />
      </svg>
      {label}
    </a>
  )
}

export function OrDivider() {
  return (
    <div className="my-6 flex items-center gap-3 text-sm text-zinc-500" aria-hidden>
      <span className="h-px flex-1 bg-zinc-200" /> or with email <span className="h-px flex-1 bg-zinc-200" />
    </div>
  )
}
