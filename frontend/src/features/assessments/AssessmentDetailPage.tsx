import { useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { useAssessment, useDeleteAssessment } from '../../hooks/useAssessments'
import { useMyScout } from '../../hooks/useUsers'
import { formatDate } from '../../lib/format'
import { ConfirmModal } from '../../components/Modal'
import { ScoreBar, ScoreDisplay } from '../../components/ScoreDisplay'
import { Button, buttonClass, Card, ErrorMessage, Notice, Spinner } from '../../components/ui'
import { ReportSection } from './ReportSection'

export default function AssessmentDetailPage() {
  const { id } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const { hasRole } = useAuth()
  const assessment = useAssessment(id)
  const myScout = useMyScout(hasRole('SCOUT'))
  const remove = useDeleteAssessment()
  const [confirmDelete, setConfirmDelete] = useState(false)
  const justSaved = (location.state as { saved?: boolean } | null)?.saved

  if (assessment.isPending) return <Spinner label="Loading assessment" />
  if (assessment.isError) return <ErrorMessage error={assessment.error} />
  const a = assessment.data
  // Mirrors the backend rule: admins, or the scout who wrote it. The backend still enforces it.
  const isOwner = !!myScout.data && myScout.data.id === a.scoutId
  const canModify = hasRole('ADMIN') || isOwner

  return (
    <>
      {justSaved && <div className="mb-4"><Notice tone="success">Assessment saved. The overall score below was calculated by the server.</Notice></div>}

      <header className="mb-6">
        <Link to={`/athletes/${a.athleteId}`} className="text-sm font-semibold text-emerald-800 hover:underline">{a.athleteName}</Link>
        <div className="mt-2 flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="font-['Barlow_Condensed',sans-serif] text-4xl font-bold">Assessment {a.assessmentCode}</h1>
            <p className="text-zinc-600">{formatDate(a.assessmentDate)} by {a.scoutName}</p>
          </div>
          {canModify && (
            <div className="flex gap-2">
              <Link to={`/assessments/${a.id}/edit`} className={buttonClass('secondary')}>Edit</Link>
              <Button variant="danger" onClick={() => setConfirmDelete(true)}>Delete</Button>
            </div>
          )}
        </div>
        {remove.isError && <div className="mt-3"><ErrorMessage error={remove.error} /></div>}
      </header>

      <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
        <Card>
          <div className="flex items-center gap-4">
            <ScoreDisplay score={a.overallScore} size="xl" label="Overall score" />
            <p className="text-zinc-600">Overall score out of 100, from {a.scores.length} skills</p>
          </div>
          <ul className="mt-6 space-y-3">
            {a.scores.map((s) => (
              <li key={s.criterionId}>
                <div className="flex justify-between gap-2">
                  <span className="font-medium">{s.criterionName}</span>
                  <span className="font-['Barlow_Condensed',sans-serif] text-xl font-bold">{s.score} / 100</span>
                </div>
                <ScoreBar score={s.score} />
              </li>
            ))}
          </ul>
          {a.remarks && (
            <div className="mt-6 border-t border-zinc-100 pt-4">
              <h2 className="font-semibold">Remarks</h2>
              <p className="whitespace-pre-line text-zinc-700">{a.remarks}</p>
            </div>
          )}
        </Card>
        <ReportSection assessmentId={a.id} canWrite={isOwner} />
      </div>

      <ConfirmModal
        open={confirmDelete}
        title="Delete assessment?"
        message="This removes the assessment and its scores. The athlete's ranking will be recalculated."
        confirmLabel="Delete"
        busy={remove.isPending}
        onCancel={() => setConfirmDelete(false)}
        onConfirm={() => remove.mutate(a.id, { onSuccess: () => navigate(`/athletes/${a.athleteId}`, { replace: true }), onSettled: () => setConfirmDelete(false) })}
      />
    </>
  )
}
