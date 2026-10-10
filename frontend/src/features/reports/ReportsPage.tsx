import { useMemo, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import type { ScoutingReport } from '../../api/types'
import { useAssessment } from '../../hooks/useAssessments'
import { useAthletes } from '../../hooks/useAthletes'
import { useSearchReports } from '../../hooks/useReports'
import { formatDate } from '../../lib/format'
import { Icon } from '../../components/Icon'
import { ScoreBar, ScoreDisplay } from '../../components/ScoreDisplay'
import { Button, EmptyState, ErrorMessage, PageHeader, Spinner } from '../../components/ui'
import { DownloadReportButton, ReportView } from '../assessments/ReportSection'

/** Full-text search over report summaries and tags (MongoDB text index on the backend). */
export default function ReportsPage() {
  // The search term lives in the URL (?q=winger), so results survive a refresh and can be shared.
  const [params, setParams] = useSearchParams()
  const q = params.get('q') ?? ''
  const [draft, setDraft] = useState(q)
  const results = useSearchReports(q)
  const athletes = useAthletes()
  const names = useMemo(() => new Map((athletes.data ?? []).map((a) => [a.id, a.fullName])), [athletes.data])

  function submit(e: FormEvent) {
    e.preventDefault()
    setParams(draft.trim() ? { q: draft.trim() } : {})
  }

  return (
    <>
      <PageHeader title="Scouting reports" description="Search words in report summaries and tags." />
      <form onSubmit={submit} role="search" className="mb-6 flex gap-2">
        <div className="relative flex-1">
          <label htmlFor="report-search" className="sr-only">Search reports</label>
          <Icon name="search" className="pointer-events-none absolute top-1/2 left-3 size-5 -translate-y-1/2 text-zinc-400" />
          <input
            id="report-search"
            type="search"
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            placeholder="winger, pace, left-footed"
            className="w-full rounded-lg border border-zinc-300 bg-white py-2 pr-3 pl-10 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-700"
          />
        </div>
        <Button type="submit">Search</Button>
      </form>

      {!q && <EmptyState title="Search scouting reports">Type a word that scouts use in their reports, for example a position or a quality.</EmptyState>}
      {q && results.isPending && <Spinner label="Searching" />}
      {results.isError && <ErrorMessage error={results.error} />}
      {results.data?.length === 0 && <EmptyState title={`No reports mention "${q}"`}>Try a broader word or a tag.</EmptyState>}
      {results.data && results.data.length > 0 && (
        <>
          <p className="mb-3 text-zinc-600">{results.data.length} reports for "{q}"</p>
          <ul className="space-y-4">
            {results.data.map((r) => <ReportResult key={r.id} report={r} athleteName={names.get(r.athleteId)} />)}
          </ul>
        </>
      )}
    </>
  )
}

/** Desktop: report on the left, the assessment's scores on the right. Mobile: stacked. */
function ReportResult({ report, athleteName }: { report: ScoutingReport; athleteName?: string }) {
  const assessment = useAssessment(report.assessmentId)
  return (
    <li className="grid gap-4 rounded-xl border border-zinc-200 bg-white p-4 lg:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
      <div>
        <h2 className="text-lg font-bold">
          <Link to={`/athletes/${report.athleteId}`} className="hover:underline">{athleteName ?? 'Athlete'}</Link>
        </h2>
        <p className="text-sm text-zinc-500">Updated {formatDate(report.updatedAt)}</p>
        <div className="mt-2"><DownloadReportButton assessmentId={report.assessmentId} /></div>
        <ReportView report={report} />
      </div>
      <div className="rounded-lg bg-zinc-50 p-3">
        {assessment.isPending && <Spinner />}
        {assessment.isError && <ErrorMessage error={assessment.error} />}
        {assessment.data && (
          <>
            <div className="flex items-center gap-3">
              <ScoreDisplay score={assessment.data.overallScore} label="Overall score" />
              <Link to={`/assessments/${assessment.data.id}`} className="text-sm font-semibold text-emerald-800 hover:underline">
                {formatDate(assessment.data.assessmentDate)} by {assessment.data.scoutName}
              </Link>
            </div>
            <ul className="mt-3 space-y-2">
              {assessment.data.scores.map((s) => (
                <li key={s.criterionId}>
                  <div className="flex justify-between text-sm"><span>{s.criterionName}</span><span className="font-semibold">{s.score} / 100</span></div>
                  <ScoreBar score={s.score} />
                </li>
              ))}
            </ul>
          </>
        )}
      </div>
    </li>
  )
}
