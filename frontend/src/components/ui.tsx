import type { ComponentProps, ReactNode } from 'react'
import { ApiError, errorMessage } from '../api/client'

// Small, reusable building blocks. Keeping styles here means every button,
// input and message looks and behaves the same across the app.

const focusRing = 'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-700'

type Variant = 'primary' | 'secondary' | 'danger' | 'ghost'
const variants: Record<Variant, string> = {
  primary: 'bg-emerald-800 text-white hover:bg-emerald-900',
  secondary: 'border border-zinc-300 bg-white text-zinc-900 hover:bg-zinc-100',
  danger: 'bg-red-700 text-white hover:bg-red-800',
  ghost: 'text-emerald-900 hover:bg-emerald-50',
}

// eslint-disable-next-line react-refresh/only-export-components
export function buttonClass(variant: Variant = 'primary', size: 'md' | 'sm' | 'lg' = 'md') {
  const sizes = { sm: 'px-3 py-1.5 text-sm', md: 'px-4 py-2', lg: 'px-5 py-3 text-lg' }
  return `inline-flex items-center justify-center gap-2 rounded-lg font-semibold transition-colors disabled:cursor-not-allowed disabled:opacity-50 ${sizes[size]} ${variants[variant]} ${focusRing}`
}

interface ButtonProps extends ComponentProps<'button'> {
  variant?: Variant
  size?: 'md' | 'sm' | 'lg'
  loading?: boolean
}
export function Button({ variant = 'primary', size = 'md', loading, className = '', children, disabled, type = 'button', ...rest }: ButtonProps) {
  return (
    <button type={type} className={`${buttonClass(variant, size)} ${className}`} disabled={disabled || loading} {...rest}>
      {loading && <span className="size-4 animate-spin rounded-full border-2 border-current border-t-transparent" aria-hidden />}
      {children}
    </button>
  )
}

export function Spinner({ label = 'Loading' }: { label?: string }) {
  return (
    <div role="status" className="flex items-center gap-3 py-6 text-zinc-600">
      <span className="size-5 animate-spin rounded-full border-2 border-emerald-800 border-t-transparent" aria-hidden />
      <span>{label}…</span>
    </div>
  )
}

/** Shows an API error. 403 gets a calm "no permission" tone instead of a red alarm. */
export function ErrorMessage({ error, title }: { error: unknown; title?: string }) {
  const forbidden = error instanceof ApiError && error.status === 403
  return (
    <div role="alert" className={`rounded-lg border p-3 ${forbidden ? 'border-amber-300 bg-amber-50 text-amber-950' : 'border-red-200 bg-red-50 text-red-800'}`}>
      {title && <p className="font-semibold">{title}</p>}
      <p>{errorMessage(error)}</p>
    </div>
  )
}

export function Notice({ children, tone = 'info' }: { children: ReactNode; tone?: 'info' | 'success' }) {
  const tones = { info: 'border-zinc-200 bg-white text-zinc-700', success: 'border-emerald-200 bg-emerald-50 text-emerald-900' }
  return <div role="status" className={`rounded-lg border p-3 ${tones[tone]}`}>{children}</div>
}

export function EmptyState({ title, children, action }: { title: string; children?: ReactNode; action?: ReactNode }) {
  return (
    <div className="rounded-xl border border-dashed border-zinc-300 bg-white px-6 py-10 text-center">
      <p className="text-lg font-semibold text-zinc-900">{title}</p>
      {children && <div className="mx-auto mt-1 max-w-md text-zinc-600">{children}</div>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}

export function PageHeader({ title, description, actions }: { title: string; description?: ReactNode; actions?: ReactNode }) {
  return (
    <header className="mb-6 flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 className="font-['Barlow_Condensed',sans-serif] text-3xl font-bold text-zinc-900 md:text-4xl">{title}</h1>
        {description && <p className="mt-1 text-zinc-600">{description}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </header>
  )
}

export function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <section className={`rounded-xl border border-zinc-200 bg-white p-4 ${className}`}>{children}</section>
}

export function Badge({ children, tone = 'neutral' }: { children: ReactNode; tone?: 'neutral' | 'green' | 'red' | 'amber' }) {
  const tones = {
    neutral: 'bg-zinc-100 text-zinc-800',
    green: 'bg-emerald-100 text-emerald-900',
    red: 'bg-red-100 text-red-800',
    amber: 'bg-amber-100 text-amber-900',
  }
  return <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-sm font-medium ${tones[tone]}`}>{children}</span>
}

// ---------- form fields ----------
// In React 19 `ref` is a normal prop, so react-hook-form's register() works on these directly.

const inputClass = `block w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-zinc-900 placeholder:text-zinc-400 disabled:bg-zinc-100 aria-invalid:border-red-600 ${focusRing}`

interface FieldShellProps {
  id: string
  label: string
  error?: string
  hint?: string
  children: ReactNode
}
export function FieldShell({ id, label, error, hint, children }: FieldShellProps) {
  return (
    <div>
      <label htmlFor={id} className="mb-1 block font-medium text-zinc-800">{label}</label>
      {children}
      {hint && !error && <p id={`${id}-hint`} className="mt-1 text-sm text-zinc-500">{hint}</p>}
      {error && <p id={`${id}-error`} className="mt-1 text-sm text-red-700">{error}</p>}
    </div>
  )
}

function describedBy(id: string, error?: string, hint?: string) {
  return error ? `${id}-error` : hint ? `${id}-hint` : undefined
}

interface InputProps extends ComponentProps<'input'> { id: string; label: string; error?: string; hint?: string }
export function Input({ id, label, error, hint, className = '', ...rest }: InputProps) {
  return (
    <FieldShell id={id} label={label} error={error} hint={hint}>
      <input id={id} className={`${inputClass} ${className}`} aria-invalid={!!error} aria-describedby={describedBy(id, error, hint)} {...rest} />
    </FieldShell>
  )
}

interface SelectProps extends ComponentProps<'select'> { id: string; label: string; error?: string; hint?: string }
export function Select({ id, label, error, hint, className = '', children, ...rest }: SelectProps) {
  return (
    <FieldShell id={id} label={label} error={error} hint={hint}>
      <select id={id} className={`${inputClass} ${className}`} aria-invalid={!!error} aria-describedby={describedBy(id, error, hint)} {...rest}>
        {children}
      </select>
    </FieldShell>
  )
}

interface TextareaProps extends ComponentProps<'textarea'> { id: string; label: string; error?: string; hint?: string }
export function Textarea({ id, label, error, hint, className = '', ...rest }: TextareaProps) {
  return (
    <FieldShell id={id} label={label} error={error} hint={hint}>
      <textarea id={id} className={`${inputClass} ${className}`} aria-invalid={!!error} aria-describedby={describedBy(id, error, hint)} {...rest} />
    </FieldShell>
  )
}
