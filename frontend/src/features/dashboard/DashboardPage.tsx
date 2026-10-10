import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { useAthletes, useMyAthlete } from '../../hooks/useAthletes'
import { useRanking } from '../../hooks/useAssessments'
import { useShortlists } from '../../hooks/useShortlists'
import { useSports } from '../../hooks/useSports'
import { useMyScout, useUsers } from '../../hooks/useUsers'
import { ROLE_LABELS } from '../../lib/format'
import { BarList } from '../../components/BarList'
import { Badge, buttonClass, Card, EmptyState, ErrorMessage, Notice, PageHeader, Spinner } from '../../components/ui'
import { ScoreDisplay } from '../../components/ScoreDisplay'

export default function DashboardPage() {
  const { user, hasRole } = useAuth()
  const isAdmin = hasRole('ADMIN')
  const isScout = hasRole('SCOUT')
  const isManager = hasRole('CLUB_MANAGER')
  const canSeeData = isAdmin || isScout || isManager

  // Each query only runs when the user's role is allowed to call that endpoint.
  const athletes = useAthletes(canSeeData)
  const mine = useMyAthlete(!canSeeData)
  const sports = useSports()
  const ranking = useRanking(canSeeData)
  const shortlists = useShortlists(isAdmin || isManager)
  const users = useUsers(isAdmin)
  const myScout = useMyScout(isScout)

  const activeAthletes = athletes.data?.filter((a) => a.active).length
  const ranked = ranking.data ?? []
  const averageScore = ranked.length
    ? Math.round(ranked.reduce((sum, row) => sum + row.overallScore, 0) / ranked.length)
    : undefined
  const strongCount = ranking.data ? ranked.filter((row) => row.overallScore >= 70).length : undefined
  const scoreBars = ranked.slice(0, 5).map((row) => ({ label: row.athleteName, value: row.overallScore }))
  const sportCounts = new Map<string, number>()
  for (const athlete of athletes.data ?? []) {
    const name = athlete.sport?.name ?? 'Unknown'
    sportCounts.set(name, (sportCounts.get(name) ?? 0) + 1)
  }
  const sportBars = [...sportCounts.entries()]
    .map(([label, value]) => ({ label, value }))
    .sort((a, b) => b.value - a.value)

  return (
    <>
      <PageHeader
        title="Dashboard"
        description={
          <span className="flex flex-wrap items-center gap-2">
            {user?.email}
            {user?.roles.map((r) => <Badge key={r} tone="green">{ROLE_LABELS[r]}</Badge>)}
          </span>
        }
        actions={isScout && <Link to="/assessments/new" className={buttonClass('primary', 'lg')}>New assessment</Link>}
      />

      {!canSeeData && mine.isPending && <Spinner label="Loading your profile" />}
      {!canSeeData && mine.data && (
        <Card>
          <h2 className="text-lg font-bold">{mine.data.fullName}</h2>
          <p className="mt-1 text-zinc-600">{mine.data.position} · {mine.data.sport.name}</p>
          <p className="mt-1 text-zinc-600">Contact {mine.data.contactNumber || 'not set'}</p>
          <Link to={`/athletes/${mine.data.id}`} className={`${buttonClass('primary')} mt-4`}>Open your profile</Link>
        </Card>
      )}
      {!canSeeData && mine.data === null && !mine.isPending && (
        <EmptyState title="Your athlete profile will appear here">
          Your profile will appear here once an administrator links it to your account. If you are a scout or club manager, ask an administrator to give you that role, then sign in again.
        </EmptyState>
      )}

      {isScout && myScout.data === null && (
        <div className="mb-6">
          <Notice>You have the scout role but no scout profile yet. Ask an administrator to create it before recording assessments.</Notice>
        </div>
      )}

      {canSeeData && (
        <>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <Stat label="Active athletes" value={activeAthletes} loading={athletes.isPending} to="/athletes" />
            <Stat label="Sports" value={sports.data?.length} loading={sports.isPending} to={isAdmin ? '/admin/sports' : undefined} />
            <Stat label="Ranked athletes" value={ranking.data?.length} loading={ranking.isPending} to="/ranking" />
            <Stat label="Average best score" value={averageScore} loading={ranking.isPending} to="/ranking" />
            <Stat label="Scores of 70 or more" value={strongCount} loading={ranking.isPending} to="/ranking" />
            {(isAdmin || isManager) && <Stat label={isAdmin ? 'Shortlists (all)' : 'My shortlists'} value={shortlists.data?.length} loading={shortlists.isPending} to="/shortlists" />}
            {isScout && !isAdmin && !isManager && myScout.data && <Stat label="Scout code" value={myScout.data.scoutCode} />}
          </div>
          {isAdmin && users.data && (
            <p className="mt-3 text-zinc-600">
              {users.data.length} user accounts. <Link to="/admin/users" className="font-semibold text-emerald-800 hover:underline">Manage users and roles</Link>
            </p>
          )}

          <div className="mt-6 grid grid-cols-1 gap-4 lg:grid-cols-2">
            <Card>
              <BarList title="Best scores" rows={scoreBars} />
              {ranking.isPending && <Spinner />}
              {ranking.isError && <ErrorMessage error={ranking.error} />}
            </Card>
            <Card>
              <BarList title="Athletes by sport" rows={sportBars} />
              {athletes.isPending && <Spinner />}
              {athletes.isError && <ErrorMessage error={athletes.error} />}
            </Card>
          </div>
          <p className="mt-2 text-sm text-zinc-500">Charts use each athlete's best score and the first 100 loaded rows.</p>

          <Card className="mt-6">
            <div className="flex items-center justify-between gap-2">
              <h2 className="text-lg font-bold">Top athletes</h2>
              <Link to="/ranking" className="font-semibold text-emerald-800 hover:underline">Full ranking</Link>
            </div>
            {ranking.isPending && <Spinner />}
            {ranking.isError && <ErrorMessage error={ranking.error} />}
            {ranking.data && ranking.data.length === 0 && <p className="mt-3 text-zinc-600">No assessments yet. Rankings appear after a scout records the first assessment.</p>}
            <ol className="mt-3 divide-y divide-zinc-100">
              {ranking.data?.slice(0, 5).map((a, i) => (
                <li key={a.id} className="flex items-center gap-3 py-2">
                  <span className="w-6 font-['Barlow_Condensed',sans-serif] text-2xl font-bold text-zinc-400">{i + 1}</span>
                  <Link to={`/athletes/${a.athleteId}`} className="flex-1 font-medium hover:underline">{a.athleteName}</Link>
                  <ScoreDisplay score={a.overallScore} size="sm" label="Best score" />
                </li>
              ))}
            </ol>
          </Card>
        </>
      )}
    </>
  )
}

function Stat({ label, value, loading, to }: { label: string; value?: ReactNode; loading?: boolean; to?: string }) {
  const content = (
    <>
      <p className="text-zinc-600">{label}</p>
      <p className="mt-1 font-['Barlow_Condensed',sans-serif] text-5xl font-bold text-zinc-900">{loading ? '…' : value ?? '-'}</p>
    </>
  )
  const cls = 'block rounded-xl border border-zinc-200 bg-white p-4'
  return to ? (
    <Link to={to} className={`${cls} hover:border-emerald-700 focus-visible:outline-2 focus-visible:outline-emerald-700`}>{content}</Link>
  ) : (
    <div className={cls}>{content}</div>
  )
}
