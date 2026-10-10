import { useMemo, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import type { AssessmentResponse, Criterion } from '../../api/types'
import { useAssessment, useSaveAssessment } from '../../hooks/useAssessments'
import { useAthletes } from '../../hooks/useAthletes'
import { useCriteria } from '../../hooks/useSports'
import { useMyScout } from '../../hooks/useUsers'
import { useAuth } from '../../auth/AuthContext'
import { formatScore, todayIso, weightedScore } from '../../lib/format'
import { ScoreDisplay } from '../../components/ScoreDisplay'
import { Button, Card, EmptyState, ErrorMessage, Input, Select, Spinner, Textarea } from '../../components/ui'

const DEFAULT_SCORE = 50

/** /assessments/new (scouts) and /assessments/:id/edit (admin or the scout who wrote it). */
export default function AssessmentFormPage() {
  const { id } = useParams()
  const existing = useAssessment(id)
  if (id && existing.isPending) return <Spinner label="Loading assessment" />
  if (id && existing.isError) return <ErrorMessage error={existing.error} />
  return <AssessmentForm existing={existing.data} />
}

function AssessmentForm({ existing }: { existing?: AssessmentResponse }) {
  const { hasRole } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const myScout = useMyScout(hasRole('SCOUT') && !existing)
  const athletes = useAthletes()
  const save = useSaveAssessment()

  const [athleteId, setAthleteId] = useState(existing?.athleteId ?? params.get('athleteId') ?? '')
  const [date, setDate] = useState(existing?.assessmentDate ?? todayIso())
  const [remarks, setRemarks] = useState(existing?.remarks ?? '')
  // Scores the scout has moved. Untouched criteria use the saved value or 50.
  const [scores, setScores] = useState<Record<string, number>>(
    () => Object.fromEntries(existing?.scores.map((s) => [s.criterionId, s.score]) ?? []),
  )
  const [touched, setTouched] = useState(false)

  const activeAthletes = useMemo(() => (athletes.data ?? []).filter((a) => a.active || a.id === existing?.athleteId), [athletes.data, existing])
  const athlete = activeAthletes.find((a) => a.id === athleteId)
  const criteria = useCriteria(athlete?.sport.id)

  const rows = (criteria.data ?? []).map((c) => ({ criterion: c, score: scores[c.id] ?? DEFAULT_SCORE }))
  const preview = weightedScore(rows.map((r) => ({ score: r.score, weight: r.criterion.weight })))

  function submit(e: FormEvent) {
    e.preventDefault()
    setTouched(true)
    if (!athlete || rows.length === 0) return
    save.mutate(
      {
        id: existing?.id,
        data: { athleteId, assessmentDate: date, remarks: remarks.trim(), scores: rows.map((r) => ({ criterionId: r.criterion.id, score: r.score })) },
      },
      { onSuccess: (saved) => navigate(`/assessments/${saved.id}`, { replace: !!existing, state: { saved: true } }) },
    )
  }

  // A SCOUT without a scout profile would get 404 from the backend; tell them before they fill the form.
  if (!existing && myScout.data === null) {
    return (
      <EmptyState title="You don't have a scout profile yet">
        An administrator must create your scout profile before you can record assessments.
      </EmptyState>
    )
  }

  const title = existing ? `Edit assessment ${existing.assessmentCode}` : 'New assessment'

  return (
    <form onSubmit={submit} noValidate>
      <h1 className="mb-6 font-['Barlow_Condensed',sans-serif] text-3xl font-bold md:text-4xl">{title}</h1>

      <div className="grid gap-6 md:grid-cols-[minmax(0,1fr)_17rem]">
        <div className="space-y-6">
          <Card>
            <div className="grid gap-4 sm:grid-cols-2">
              <Select
                id="athleteId"
                label="Athlete"
                value={athleteId}
                onChange={(e) => { setAthleteId(e.target.value); setScores({}) }}
                disabled={!!existing || athletes.isPending}
                error={touched && !athlete ? 'Choose the athlete you are assessing' : undefined}
              >
                <option value="">{athletes.isPending ? 'Loading athletes…' : 'Choose an athlete'}</option>
                {activeAthletes.map((a) => <option key={a.id} value={a.id}>{a.fullName} ({a.sport.name}, {a.position})</option>)}
              </Select>
              <Input id="date" label="Date" type="date" value={date} max={todayIso()} onChange={(e) => setDate(e.target.value)} />
            </div>
            {athletes.isError && <div className="mt-3"><ErrorMessage error={athletes.error} /></div>}
          </Card>

          {athlete && (
            <Card>
              <h2 className="text-lg font-bold">Scores for {athlete.sport.name}</h2>
              <p className="text-sm text-zinc-600">Score each skill from 0 to 100. The overall score is the average, also out of 100.</p>
              {criteria.isPending && <Spinner label="Loading criteria" />}
              {criteria.isError && <ErrorMessage error={criteria.error} />}
              {criteria.data?.length === 0 && (
                <p className="mt-3 text-red-700">{athlete.sport.name} has no scoring criteria yet. An administrator must add them first.</p>
              )}
              <ul className="mt-4 space-y-5">
                {rows.map(({ criterion, score }) => (
                  <ScoreRow key={criterion.id} criterion={criterion} score={score} onChange={(v) => setScores((s) => ({ ...s, [criterion.id]: v }))} />
                ))}
              </ul>
            </Card>
          )}

          <Card>
            <Textarea id="remarks" label="Remarks" rows={3} placeholder="What stood out in this session?" value={remarks} onChange={(e) => setRemarks(e.target.value)} />
          </Card>

          {save.isError && <ErrorMessage error={save.error} title="The assessment was not saved" />}
        </div>

        {/* Desktop: live preview on the right */}
        <aside className="hidden md:block">
          <div className="sticky top-6 rounded-xl border border-zinc-200 bg-white p-5 text-center">
            <p className="font-medium text-zinc-600">Overall score preview</p>
            <div className="my-3"><ScoreDisplay score={preview} size="xl" label="Overall score preview" /></div>
            <p className="text-sm text-zinc-500">Average of {rows.length} skills, out of 100. The server saves the final score.</p>
            <Button type="submit" size="lg" className="mt-4 w-full" loading={save.isPending}>{existing ? 'Save changes' : 'Save assessment'}</Button>
            <Link to={existing ? `/assessments/${existing.id}` : '/'} className="mt-2 block text-sm font-semibold text-emerald-800 hover:underline">Cancel</Link>
          </div>
        </aside>
      </div>

      {/* Mobile: sticky bar above the bottom navigation, always within thumb reach */}
      <div className="sticky bottom-16 z-10 mt-6 flex items-center gap-3 rounded-xl border border-zinc-200 bg-white p-3 shadow-lg md:hidden">
        <ScoreDisplay score={preview} size="md" label="Overall score preview" />
        <span className="flex-1 text-sm text-zinc-600">Preview</span>
        <Button type="submit" size="lg" loading={save.isPending}>Save</Button>
      </div>
    </form>
  )
}

function ScoreRow({ criterion, score, onChange }: { criterion: Criterion; score: number; onChange: (v: number) => void }) {
  const sliderId = `score-${criterion.id}`
  const clamp = (v: number) => Math.max(0, Math.min(100, Math.round(v)))
  return (
    <li>
      <div className="flex items-baseline justify-between gap-2">
        <label htmlFor={sliderId} className="font-semibold">{criterion.name}</label>
        <label className="sr-only" htmlFor={`${sliderId}-num`}>{criterion.name} score</label>
        <input
          id={`${sliderId}-num`}
          type="number"
          inputMode="numeric"
          min={0}
          max={100}
          value={score}
          onChange={(e) => onChange(clamp(Number(e.target.value)))}
          className="w-16 rounded-md border border-zinc-300 px-2 py-1 text-right font-['Barlow_Condensed',sans-serif] text-xl font-bold focus-visible:outline-2 focus-visible:outline-emerald-700"
        />
        <span className="text-sm font-semibold text-zinc-500">/ 100</span>
      </div>
      {/* Large custom thumb (32px) so it is easy to drag with a thumb on a 360px phone */}
      <input
        id={sliderId}
        type="range"
        min={0}
        max={100}
        step={1}
        value={score}
        aria-valuetext={`${score} out of 100`}
        onChange={(e) => onChange(Number(e.target.value))}
        style={{ background: `linear-gradient(to right, #065f46 ${score}%, #e4e4e7 ${score}%)` }}
        className="mt-3 h-3 w-full cursor-pointer appearance-none rounded-full focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-emerald-700 [&::-moz-range-thumb]:size-7 [&::-moz-range-thumb]:rounded-full [&::-moz-range-thumb]:border-4 [&::-moz-range-thumb]:border-white [&::-moz-range-thumb]:bg-emerald-800 [&::-moz-range-thumb]:shadow [&::-webkit-slider-thumb]:size-8 [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:border-4 [&::-webkit-slider-thumb]:border-white [&::-webkit-slider-thumb]:bg-emerald-800 [&::-webkit-slider-thumb]:shadow"
      />
      <span className="sr-only">Current: {formatScore(score)}</span>
    </li>
  )
}
