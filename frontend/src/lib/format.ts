import type { Role } from '../api/types'

export const ROLE_LABELS: Record<Role, string> = {
  ADMIN: 'Admin',
  SCOUT: 'Scout',
  CLUB_MANAGER: 'Club manager',
  ATHLETE: 'Athlete',
}

/** Today's date as YYYY-MM-DD in the user's local time zone (not UTC). */
export function todayIso(): string {
  return new Date().toLocaleDateString('en-CA') // en-CA formats as YYYY-MM-DD
}

/** "2026-10-03" or an ISO date-time -> "3 Oct 2026". */
export function formatDate(value: string | null | undefined): string {
  if (!value) return '-'
  // Parse date-only strings as local dates, otherwise "2026-10-03" is read as UTC midnight.
  const [y, m, d] = value.slice(0, 10).split('-').map(Number)
  if (!y || !m || !d) return value
  return new Date(y, m - 1, d).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })
}

export function ageFrom(dateOfBirth: string): number | null {
  const [y, m, d] = dateOfBirth.split('-').map(Number)
  if (!y || !m || !d) return null
  const now = new Date()
  let age = now.getFullYear() - y
  if (now.getMonth() + 1 < m || (now.getMonth() + 1 === m && now.getDate() < d)) age--
  return age
}

export function formatScore(score: number): string {
  return (Math.round(score * 10) / 10).toFixed(1)
}

/** Same formula as the backend: sum(score x weight) / sum(weight). Used only for the live preview. */
export function weightedScore(items: { score: number; weight: number }[]): number {
  const totalWeight = items.reduce((sum, i) => sum + i.weight, 0)
  if (totalWeight === 0) return 0
  return items.reduce((sum, i) => sum + i.score * i.weight, 0) / totalWeight
}

/** "fast, left foot\nstrong" -> ["fast", "left foot", "strong"] */
export function splitList(text: string): string[] {
  return text.split(/[\n,]/).map((s) => s.trim()).filter(Boolean)
}
