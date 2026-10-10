import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import { useAthleteAssessments } from '../../hooks/useAssessments'
import { useAthlete, useDeactivateAthlete, useDeleteAthlete, useLinkAccount, useUpdateMyContact } from '../../hooks/useAthletes'
import { useUsers } from '../../hooks/useUsers'
import { useAthleteReports } from '../../hooks/useReports'
import { ageFrom, formatDate } from '../../lib/format'
import { ConfirmModal } from '../../components/Modal'
import { ScoreDisplay } from '../../components/ScoreDisplay'
import { DownloadReportButton } from '../assessments/ReportSection'
import { Badge, Button, buttonClass, Card, ErrorMessage, Input, Select, Spinner } from '../../components/ui'
import { AddToShortlistModal } from '../shortlists/AddToShortlistModal'
import { AthleteFormModal } from './AthleteFormModal'
import { PhysicalProfileCard } from './PhysicalProfileCard'

type Tab = 'info' | 'physical' | 'history'
const TABS: { id: Tab; label: string }[] = [
  { id: 'info', label: 'Info' },
  { id: 'physical', label: 'Physical' },
  { id: 'history', label: 'History' },
]

/**
 * Mobile: three tabs (one section visible at a time).
 * Desktop: tabs are hidden and the sections sit side by side: profile left, history right.
 * Trick: each section is `hidden` when its tab is not selected, but `md:block` always shows it on desktop.
 */
export default function AthleteDetailPage() {
  const { id } = useParams()
  const { hasRole } = useAuth()
  const navigate = useNavigate()
  const athlete = useAthlete(id)
  const [tab, setTab] = useState<Tab>('info')
  const [editOpen, setEditOpen] = useState(false)
  const [confirm, setConfirm] = useState<'deactivate' | 'delete' | null>(null)
  const [shortlistOpen, setShortlistOpen] = useState(false)
  const deactivate = useDeactivateAthlete()
  const remove = useDeleteAthlete()

  if (athlete.isPending) return <Spinner label="Loading athlete" />
  if (athlete.isError) return <ErrorMessage error={athlete.error} />
  const a = athlete.data
  const isAdmin = hasRole('ADMIN')
  const isStaff = hasRole('ADMIN', 'SCOUT', 'CLUB_MANAGER')
  const athleteOnly = hasRole('ATHLETE') && !isStaff
  const actionError = deactivate.error ?? remove.error
  const show = (t: Tab) => (tab === t ? 'block' : 'hidden md:block')

  return (
    <>
      <header className="mb-6">
        <Link to={isStaff ? '/athletes' : '/'} className="text-sm font-semibold text-emerald-800 hover:underline">
          {isStaff ? 'All athletes' : 'Dashboard'}
        </Link>
        <div className="mt-2 flex flex-wrap items-start justify-between gap-3">
          <div>
            <h1 className="font-['Barlow_Condensed',sans-serif] text-4xl font-bold">{a.fullName}</h1>
            <p className="mt-1 flex flex-wrap items-center gap-2 text-zinc-600">
              {a.position} · {a.sport.name} {a.active ? <Badge tone="green">Active</Badge> : <Badge tone="red">Inactive</Badge>}
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            {hasRole('SCOUT') && a.active && (
              <Link to={`/assessments/new?athleteId=${a.id}`} className={buttonClass('primary')}>Assess</Link>
            )}
            {hasRole('ADMIN', 'CLUB_MANAGER') && a.active && (
              <Button variant="secondary" onClick={() => setShortlistOpen(true)}>Add to shortlist</Button>
            )}
            {hasRole('ADMIN', 'SCOUT') && <Button variant="secondary" onClick={() => setEditOpen(true)}>Edit</Button>}
            {isAdmin && a.active && <Button variant="secondary" onClick={() => setConfirm('deactivate')}>Deactivate</Button>}
            {isAdmin && <Button variant="danger" onClick={() => setConfirm('delete')}>Delete</Button>}
          </div>
        </div>
        {actionError && (
          <div className="mt-3">
            <ErrorMessage
              error={actionError instanceof ApiError && actionError.status === 409
                ? new Error(`${actionError.message} Deactivate the athlete instead to keep their history.`)
                : actionError}
            />
          </div>
        )}
      </header>

      <div role="tablist" aria-label="Athlete sections" className="mb-4 flex rounded-lg bg-zinc-200 p-1 md:hidden">
        {TABS.map((t) => (
          <button
            key={t.id}
            role="tab"
            type="button"
            aria-selected={tab === t.id}
            onClick={() => setTab(t.id)}
            className={`flex-1 rounded-md py-2 font-semibold focus-visible:outline-2 focus-visible:outline-emerald-700 ${tab === t.id ? 'bg-white text-zinc-900 shadow-sm' : 'text-zinc-600'}`}
          >
            {t.label}
          </button>
        ))}
      </div>

      <div className="grid gap-4 md:grid-cols-[minmax(0,2fr)_minmax(0,3fr)]">
        <div className="space-y-4">
          <div className={show('info')}>
            <Card>
              <h2 className="text-lg font-bold">Details</h2>
              <dl className="mt-3 grid grid-cols-2 gap-x-4 gap-y-3">
                <Detail label="Athlete code" value={a.athleteCode} />
                <Detail label="Date of birth" value={`${formatDate(a.dateOfBirth)} (${ageFrom(a.dateOfBirth) ?? '-'})`} />
                <Detail label="Nationality" value={a.nationality} />
                <Detail label="Contact" value={a.contactNumber} />
                <Detail label="Team" value={a.team ? `${a.team.name}, ${a.team.city}` : 'No team'} />
                <Detail label="Linked account" value={a.user?.email ?? 'Not linked'} />
              </dl>
              {athleteOnly && <ContactEditor current={a.contactNumber} />}
              {isAdmin && <LinkAccount athleteId={a.id} currentUserId={a.user?.id ?? ''} />}
            </Card>
          </div>
          <div className={show('physical')}>
            <PhysicalProfileCard athleteId={a.id} canEdit={hasRole('ADMIN', 'SCOUT')} />
          </div>
        </div>
        <div className={show('history')}>
          <History athleteId={a.id} canOpenAssessment={isStaff} />
        </div>
      </div>

      <AthleteFormModal open={editOpen} onClose={() => setEditOpen(false)} athlete={a} />
      <AddToShortlistModal athlete={shortlistOpen ? { id: a.id, name: a.fullName } : null} onClose={() => setShortlistOpen(false)} />
      <ConfirmModal
        open={confirm === 'deactivate'}
        title="Deactivate athlete?"
        message={`${a.fullName} will stay in the system with their history, but can no longer be assessed or shortlisted.`}
        confirmLabel="Deactivate"
        busy={deactivate.isPending}
        onCancel={() => setConfirm(null)}
        onConfirm={() => deactivate.mutate(a.id, { onSettled: () => setConfirm(null) })}
      />
      <ConfirmModal
        open={confirm === 'delete'}
        title="Delete athlete?"
        message={`This permanently deletes ${a.fullName}. Athletes with assessments or shortlist entries cannot be deleted.`}
        confirmLabel="Delete"
        busy={remove.isPending}
        onCancel={() => setConfirm(null)}
        onConfirm={() => remove.mutate(a.id, { onSuccess: () => navigate('/athletes', { replace: true }), onSettled: () => setConfirm(null) })}
      />
    </>
  )
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div className="min-w-0">
      <dt className="text-sm text-zinc-500">{label}</dt>
      <dd className="break-words font-medium">{value}</dd>
    </div>
  )
}

function ContactEditor({ current }: { current: string }) {
  const [value, setValue] = useState(current)
  const save = useUpdateMyContact()
  return (
    <form
      className="mt-4 flex flex-wrap items-end gap-2"
      onSubmit={(e) => {
        e.preventDefault()
        save.mutate(value)
      }}
    >
      <Input id="my-contact" label="Your contact number" value={value} onChange={(e) => setValue(e.target.value)} />
      <Button type="submit" loading={save.isPending}>Save contact</Button>
      {save.isError && <ErrorMessage error={save.error} />}
    </form>
  )
}

function LinkAccount({ athleteId, currentUserId }: { athleteId: string; currentUserId: string }) {
  const users = useUsers(true)
  const link = useLinkAccount(athleteId)
  const [userId, setUserId] = useState(currentUserId)
  return (
    <form
      className="mt-4 max-w-md"
      onSubmit={(e) => {
        e.preventDefault()
        link.mutate(userId || null)
      }}
    >
      <Select id="link-user" label="Link a login account" value={userId} onChange={(e) => setUserId(e.target.value)} hint="The athlete will see this profile after they sign in again.">
        <option value="">Not linked</option>
        {users.data?.map((u) => <option key={u.id} value={u.id}>{u.email}</option>)}
      </Select>
      <Button type="submit" className="mt-3" loading={link.isPending}>Save link</Button>
      {link.isError && <ErrorMessage error={link.error} />}
    </form>
  )
}

function History({ athleteId, canOpenAssessment }: { athleteId: string; canOpenAssessment: boolean }) {
  const history = useAthleteAssessments(athleteId)
  const reports = useAthleteReports(athleteId)
  const withReport = new Set(reports.data?.map((r) => r.assessmentId))

  return (
    <Card>
      <h2 className="text-lg font-bold">Assessment history</h2>
      {history.isPending && <Spinner />}
      {history.isError && <ErrorMessage error={history.error} />}
      {history.data?.length === 0 && <p className="mt-2 text-zinc-600">Not assessed yet.</p>}
      <ul className="mt-3 divide-y divide-zinc-100">
        {history.data?.map((h) => {
          const report = reports.data?.find((r) => r.assessmentId === h.id)
          const row = (
            <>
              <ScoreDisplay score={h.overallScore} size="sm" label="Overall score" />
              <div className="min-w-0 flex-1">
                <p className="font-semibold">{formatDate(h.assessmentDate)}</p>
                <p className="truncate text-sm text-zinc-600">by {h.scoutName}{h.remarks && `: ${h.remarks}`}</p>
                {!canOpenAssessment && report?.summary && <p className="mt-1 text-sm text-zinc-700">{report.summary}</p>}
              </div>
              {withReport.has(h.id) && <Badge>Report</Badge>}
            </>
          )
          return (
            <li key={h.id} className="flex items-center gap-3 py-3">
              {canOpenAssessment ? (
                <Link to={`/assessments/${h.id}`} className="flex min-w-0 flex-1 items-center gap-3 rounded-lg hover:bg-zinc-50 focus-visible:outline-2 focus-visible:outline-emerald-700">
                  {row}
                </Link>
              ) : (
                <div className="flex min-w-0 flex-1 items-center gap-3">{row}</div>
              )}
              {report && <DownloadReportButton assessmentId={h.id} />}
            </li>
          )
        })}
      </ul>
    </Card>
  )
}
