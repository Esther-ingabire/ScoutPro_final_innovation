import { useState } from 'react'
import { useFieldArray, useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { downloadPdf } from '../../api/client'
import type { ScoutingReport, ScoutingReportRequest } from '../../api/types'
import { useDeleteReport, useReport, useSaveReport } from '../../hooks/useReports'
import { splitList } from '../../lib/format'
import { ConfirmModal } from '../../components/Modal'
import { Badge, Button, Card, ErrorMessage, Input, Select, Spinner, Textarea } from '../../components/ui'

/** The scouting report lives in MongoDB, one per assessment. Only the assessing scout can write it. */
export function ReportSection({ assessmentId, canWrite }: { assessmentId: string; canWrite: boolean }) {
  const report = useReport(assessmentId)
  const remove = useDeleteReport(assessmentId)
  const [editing, setEditing] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)

  return (
    <Card>
      <div className="flex items-center justify-between gap-2">
        <h2 className="text-lg font-bold">Scouting report</h2>
        <div className="flex gap-1">
          {report.data && <DownloadReportButton assessmentId={assessmentId} />}
          {canWrite && report.data && !editing && (
            <>
              <Button variant="ghost" size="sm" onClick={() => setEditing(true)}>Edit</Button>
              <Button variant="ghost" size="sm" onClick={() => setConfirmDelete(true)}>Delete</Button>
            </>
          )}
        </div>
      </div>
      {report.isPending && <Spinner />}
      {report.isError && <ErrorMessage error={report.error} />}
      {remove.isError && <ErrorMessage error={remove.error} />}

      {editing ? (
        <ReportForm assessmentId={assessmentId} current={report.data ?? null} onDone={() => setEditing(false)} />
      ) : report.data ? (
        <ReportView report={report.data} />
      ) : (
        report.data === null && (
          <div className="mt-2">
            <p className="text-zinc-600">No scouting report for this assessment yet.</p>
            {canWrite && <Button className="mt-3" onClick={() => setEditing(true)}>Write scouting report</Button>}
          </div>
        )
      )}

      <ConfirmModal
        open={confirmDelete}
        title="Delete scouting report?"
        message="The report text, tags and media links will be removed. The assessment and its scores stay."
        confirmLabel="Delete report"
        busy={remove.isPending}
        onCancel={() => setConfirmDelete(false)}
        onConfirm={() => remove.mutate(undefined, { onSettled: () => setConfirmDelete(false) })}
      />
    </Card>
  )
}

export function ReportView({ report }: { report: ScoutingReport }) {
  return (
    <div className="mt-3 space-y-4">
      <p className="whitespace-pre-line text-zinc-800">{report.summary}</p>
      {report.tags.length > 0 && (
        <ul className="flex flex-wrap gap-1" aria-label="Tags">
          {report.tags.map((t) => <li key={t}><Badge tone="amber">{t}</Badge></li>)}
        </ul>
      )}
      <div className="grid gap-4 sm:grid-cols-2">
        <ListBlock title="Strengths" items={report.strengths} />
        <ListBlock title="Weaknesses" items={report.weaknesses} />
      </div>
      {report.media.length > 0 && (
        <div>
          <h3 className="font-semibold">Media</h3>
          <ul className="mt-1 space-y-1">
            {report.media.map((m, i) => (
              <li key={i} className="text-sm">
                <a href={m.url} target="_blank" rel="noopener noreferrer" className="font-medium text-emerald-800 underline">{m.type.toLowerCase()}</a>
                {m.startSec != null && ` from ${m.startSec}s`}
                {m.note && `: ${m.note}`}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}

export function DownloadReportButton({ assessmentId }: { assessmentId: string }) {
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<unknown>(null)

  async function download() {
    setPending(true)
    setError(null)
    try {
      await downloadPdf(`/assessments/${assessmentId}/report.pdf`, 'scoutpro-report.pdf')
    } catch (e) {
      setError(e)
    } finally {
      setPending(false)
    }
  }

  return (
    <div>
      <Button variant="secondary" size="sm" loading={pending} onClick={download}>Download PDF</Button>
      {error != null && <div className="mt-2"><ErrorMessage error={error} /></div>}
    </div>
  )
}

function ListBlock({ title, items }: { title: string; items: string[] }) {
  return (
    <div>
      <h3 className="font-semibold">{title}</h3>
      {items.length === 0 ? <p className="text-sm text-zinc-500">None noted</p> : (
        <ul className="mt-1 list-disc space-y-0.5 pl-5 text-zinc-700">{items.map((s) => <li key={s}>{s}</li>)}</ul>
      )}
    </div>
  )
}

const schema = z.object({
  summary: z.string().trim().min(1, 'Write a short summary'),
  strengths: z.string(),
  weaknesses: z.string(),
  tags: z.string(),
  media: z.array(
    z.object({
      url: z.url('Enter a full link starting with https://'),
      type: z.enum(['VIDEO', 'IMAGE', 'LINK']),
      startSec: z.string().regex(/^\d*$/, 'Whole seconds only'),
      note: z.string(),
    }),
  ),
})
type FormValues = z.infer<typeof schema>

function ReportForm({ assessmentId, current, onDone }: { assessmentId: string; current: ScoutingReport | null; onDone: () => void }) {
  const save = useSaveReport(assessmentId)
  const { register, control, handleSubmit, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      summary: current?.summary ?? '',
      strengths: current?.strengths.join('\n') ?? '',
      weaknesses: current?.weaknesses.join('\n') ?? '',
      tags: current?.tags.join(', ') ?? '',
      media: current?.media.map((m) => ({ ...m, startSec: m.startSec == null ? '' : String(m.startSec) })) ?? [],
    },
  })
  // useFieldArray manages the repeating "media" rows (add / remove).
  const media = useFieldArray({ control, name: 'media' })

  function onSubmit(v: FormValues) {
    const data: ScoutingReportRequest = {
      summary: v.summary.trim(),
      strengths: splitList(v.strengths),
      weaknesses: splitList(v.weaknesses),
      tags: splitList(v.tags).map((t) => t.toLowerCase()),
      media: v.media.map((m) => ({ url: m.url, type: m.type, note: m.note.trim(), startSec: m.startSec === '' ? null : Number(m.startSec) })),
    }
    save.mutate({ exists: !!current, data }, { onSuccess: onDone })
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="mt-3 space-y-4">
      <Textarea id="summary" label="Summary" rows={4} error={errors.summary?.message} {...register('summary')} />
      <div className="grid gap-4 sm:grid-cols-2">
        <Textarea id="strengths" label="Strengths" rows={3} hint="One per line" {...register('strengths')} />
        <Textarea id="weaknesses" label="Weaknesses" rows={3} hint="One per line" {...register('weaknesses')} />
      </div>
      <Input id="tags" label="Tags" hint="Separate with commas, e.g. winger, left-footed. Used by report search." {...register('tags')} />

      <fieldset>
        <legend className="font-medium">Media links</legend>
        <ul className="mt-2 space-y-3">
          {media.fields.map((field, i) => (
            <li key={field.id} className="rounded-lg border border-zinc-200 p-3">
              <div className="grid gap-3 sm:grid-cols-[minmax(0,1fr)_8rem_7rem]">
                <Input id={`media-${i}-url`} label="Link" type="url" placeholder="https://" error={errors.media?.[i]?.url?.message} {...register(`media.${i}.url`)} />
                <Select id={`media-${i}-type`} label="Type" {...register(`media.${i}.type`)}>
                  <option value="VIDEO">Video</option>
                  <option value="IMAGE">Image</option>
                  <option value="LINK">Link</option>
                </Select>
                <Input id={`media-${i}-start`} label="Start (sec)" inputMode="numeric" error={errors.media?.[i]?.startSec?.message} {...register(`media.${i}.startSec`)} />
              </div>
              <div className="mt-3 flex items-end gap-2">
                <div className="flex-1"><Input id={`media-${i}-note`} label="Note" {...register(`media.${i}.note`)} /></div>
                <Button variant="secondary" onClick={() => media.remove(i)} aria-label={`Remove media link ${i + 1}`}>Remove</Button>
              </div>
            </li>
          ))}
        </ul>
        <Button variant="ghost" size="sm" className="mt-2" onClick={() => media.append({ url: '', type: 'VIDEO', startSec: '', note: '' })}>Add media link</Button>
      </fieldset>

      {save.isError && <ErrorMessage error={save.error} />}
      <div className="flex justify-end gap-2">
        <Button variant="secondary" onClick={onDone}>Cancel</Button>
        <Button type="submit" loading={save.isPending}>{current ? 'Save report' : 'Publish report'}</Button>
      </div>
    </form>
  )
}
