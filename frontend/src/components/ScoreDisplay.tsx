import { formatScore } from '../lib/format'

// The app's signature element: scores set in a tall condensed "scoreboard" face.
// 80+ gets the amber training-bib highlight so top talent stands out at a glance.

function tone(score: number) {
  if (score >= 80) return 'bg-amber-300 text-zinc-950'
  if (score >= 60) return 'bg-emerald-800 text-white'
  return 'bg-zinc-200 text-zinc-900'
}

export function ScoreDisplay({ score, size = 'md', label }: { score: number; size?: 'sm' | 'md' | 'xl'; label?: string }) {
  const sizes = { sm: 'min-w-12 px-2 text-xl', md: 'min-w-16 px-3 text-3xl', xl: 'min-w-32 px-5 py-1 text-7xl' }
  return (
    <span
      className={`inline-flex items-center justify-center rounded-lg font-['Barlow_Condensed',sans-serif] font-bold tabular-nums leading-tight ${sizes[size]} ${tone(score)}`}
      aria-label={`${label ?? 'Score'} ${formatScore(score)} out of 100`}
    >
      {formatScore(score)}
      <span className="ml-0.5 text-[0.45em] font-semibold opacity-80">/100</span>
    </span>
  )
}

/** A thin bar for one criterion score (0-100). */
export function ScoreBar({ score }: { score: number }) {
  const width = Math.max(0, Math.min(100, score))
  return (
    <div className="h-2 w-full overflow-hidden rounded-full bg-zinc-200" aria-hidden>
      <div className={`h-full rounded-full ${score >= 80 ? 'bg-amber-400' : 'bg-emerald-700'}`} style={{ width: `${width}%` }} />
    </div>
  )
}
