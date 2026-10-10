import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { useRanking } from '../../hooks/useAssessments'
import { useAthletes } from '../../hooks/useAthletes'
import { useSports } from '../../hooks/useSports'
import { formatDate } from '../../lib/format'
import { ScoreDisplay } from '../../components/ScoreDisplay'
import { Button, EmptyState, ErrorMessage, PageHeader, Spinner } from '../../components/ui'
import { AddToShortlistModal } from '../shortlists/AddToShortlistModal'

export default function RankingPage() {
  const { hasRole } = useAuth()
  const ranking = useRanking() // already reduced to each athlete's best assessment
  const athletes = useAthletes()
  const sports = useSports()
  const [sportId, setSportId] = useState('')
  const [shortlistTarget, setShortlistTarget] = useState<{ id: string; name: string } | null>(null)
  const canShortlist = hasRole('ADMIN', 'CLUB_MANAGER')

  // The ranking response has no sport, so look each athlete up in the athletes list.
  const athleteById = useMemo(() => new Map((athletes.data ?? []).map((a) => [a.id, a])), [athletes.data])
  const rows = (ranking.data ?? [])
    .map((r) => ({ ...r, athlete: athleteById.get(r.athleteId) }))
    .filter((r) => !sportId || r.athlete?.sport.id === sportId)

  return (
    <>
      <PageHeader title="Ranking" description="Each athlete's best assessment, highest overall score first." />

      <div role="group" aria-label="Filter by sport" className="mb-4 flex gap-2 overflow-x-auto pb-1">
        {[{ id: '', name: 'All sports' }, ...(sports.data ?? [])].map((s) => (
          <button
            key={s.id}
            type="button"
            aria-pressed={sportId === s.id}
            onClick={() => setSportId(s.id)}
            className={`shrink-0 rounded-full border px-4 py-1.5 font-medium focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-700 ${
              sportId === s.id ? 'border-emerald-800 bg-emerald-800 text-white' : 'border-zinc-300 bg-white text-zinc-800 hover:bg-zinc-100'
            }`}
          >
            {s.name}
          </button>
        ))}
      </div>

      {ranking.isPending && <Spinner label="Loading ranking" />}
      {ranking.isError && <ErrorMessage error={ranking.error} />}
      {ranking.data && rows.length === 0 && (
        <EmptyState title="Nobody ranked yet">Athletes appear here after their first assessment.</EmptyState>
      )}

      {rows.length > 0 && (
        <>
          <ol className="space-y-2 md:hidden">
            {rows.map((r, i) => (
              <li key={r.id} className="flex items-center gap-3 rounded-xl border border-zinc-200 bg-white p-3">
                <span className="w-8 text-center font-['Barlow_Condensed',sans-serif] text-3xl font-bold text-zinc-400">{i + 1}</span>
                <div className="min-w-0 flex-1">
                  <Link to={`/athletes/${r.athleteId}`} className="font-semibold hover:underline">{r.athleteName}</Link>
                  <p className="truncate text-sm text-zinc-600">{r.athlete ? `${r.athlete.position} · ${r.athlete.sport.name}` : ''}</p>
                  {canShortlist && <button type="button" onClick={() => setShortlistTarget({ id: r.athleteId, name: r.athleteName })} className="mt-1 text-sm font-semibold text-emerald-800">Add to shortlist</button>}
                </div>
                <ScoreDisplay score={r.overallScore} label="Best score" />
              </li>
            ))}
          </ol>

          <div className="hidden overflow-x-auto rounded-xl border border-zinc-200 bg-white md:block">
            <table className="w-full text-left">
              <thead className="border-b border-zinc-200 bg-zinc-50 text-sm text-zinc-600">
                <tr>
                  <th scope="col" className="px-4 py-3 font-semibold">Rank</th>
                  <th scope="col" className="px-4 py-3 font-semibold">Athlete</th>
                  <th scope="col" className="px-4 py-3 font-semibold">Sport</th>
                  <th scope="col" className="px-4 py-3 font-semibold">Position</th>
                  <th scope="col" className="px-4 py-3 font-semibold">Assessed</th>
                  <th scope="col" className="px-4 py-3 font-semibold">Best score</th>
                  {canShortlist && <th scope="col" className="px-4 py-3"><span className="sr-only">Actions</span></th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-100">
                {rows.map((r, i) => (
                  <tr key={r.id}>
                    <td className="px-4 py-3 font-['Barlow_Condensed',sans-serif] text-2xl font-bold text-zinc-400">{i + 1}</td>
                    <td className="px-4 py-3 font-semibold"><Link to={`/athletes/${r.athleteId}`} className="hover:underline">{r.athleteName}</Link></td>
                    <td className="px-4 py-3">{r.athlete?.sport.name ?? '-'}</td>
                    <td className="px-4 py-3">{r.athlete?.position ?? '-'}</td>
                    <td className="px-4 py-3 text-zinc-600"><Link to={`/assessments/${r.id}`} className="hover:underline">{formatDate(r.assessmentDate)}</Link> by {r.scoutName}</td>
                    <td className="px-4 py-3"><ScoreDisplay score={r.overallScore} size="sm" label="Best score" /></td>
                    {canShortlist && (
                      <td className="px-4 py-3 text-right">
                        <Button variant="secondary" size="sm" onClick={() => setShortlistTarget({ id: r.athleteId, name: r.athleteName })}>Add to shortlist</Button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      <AddToShortlistModal athlete={shortlistTarget} onClose={() => setShortlistTarget(null)} />
    </>
  )
}
