import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link } from 'react-router-dom'
import { z } from 'zod'
import type { Shortlist } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { useDeleteShortlist, useRemoveFromShortlist, useSaveShortlist, useShortlists } from '../../hooks/useShortlists'
import { ConfirmModal, Modal } from '../../components/Modal'
import { Button, EmptyState, ErrorMessage, Input, PageHeader, Spinner, Textarea } from '../../components/ui'

/** Mobile: shortlists stacked. Desktop: a board with shortlists side by side (scrolls sideways). */
export default function ShortlistsPage() {
  const { hasRole } = useAuth()
  const shortlists = useShortlists()
  const remove = useDeleteShortlist()
  const [editing, setEditing] = useState<Shortlist | 'new' | null>(null)
  const [deleting, setDeleting] = useState<Shortlist | null>(null)

  return (
    <>
      <PageHeader
        title="Shortlists"
        description={hasRole('ADMIN') ? 'All shortlists from every club manager.' : 'Add athletes from the ranking or an athlete page.'}
        actions={<Button onClick={() => setEditing('new')}>New shortlist</Button>}
      />

      {shortlists.isPending && <Spinner label="Loading shortlists" />}
      {shortlists.isError && <ErrorMessage error={shortlists.error} />}
      {remove.isError && <div className="mb-4"><ErrorMessage error={remove.error} /></div>}
      {shortlists.data?.length === 0 && (
        <EmptyState title="No shortlists yet" action={<Button onClick={() => setEditing('new')}>Create your first shortlist</Button>}>
          Group the athletes you are considering, for example "U20 strikers" or "Trial week".
        </EmptyState>
      )}

      <div className="space-y-4 md:flex md:items-start md:gap-4 md:space-y-0 md:overflow-x-auto md:pb-4">
        {shortlists.data?.map((s) => (
          <ShortlistColumn key={s.id} shortlist={s} showOwner={hasRole('ADMIN')} onEdit={() => setEditing(s)} onDelete={() => setDeleting(s)} />
        ))}
      </div>

      <Modal open={!!editing} onClose={() => setEditing(null)} title={editing === 'new' ? 'New shortlist' : 'Edit shortlist'}>
        {editing && <ShortlistForm current={editing === 'new' ? null : editing} onDone={() => setEditing(null)} />}
      </Modal>
      <ConfirmModal
        open={!!deleting}
        title="Delete shortlist?"
        message={`"${deleting?.name}" will be deleted. The athletes themselves are not affected.`}
        confirmLabel="Delete shortlist"
        busy={remove.isPending}
        onCancel={() => setDeleting(null)}
        onConfirm={() => deleting && remove.mutate(deleting.id, { onSettled: () => setDeleting(null) })}
      />
    </>
  )
}

function ShortlistColumn({ shortlist, showOwner, onEdit, onDelete }: { shortlist: Shortlist; showOwner: boolean; onEdit: () => void; onDelete: () => void }) {
  const removeAthlete = useRemoveFromShortlist()
  return (
    <section aria-labelledby={`sl-${shortlist.id}`} className="rounded-xl border border-zinc-200 bg-white p-4 md:w-80 md:shrink-0">
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <h2 id={`sl-${shortlist.id}`} className="text-lg font-bold">{shortlist.name}</h2>
          <p className="text-sm text-zinc-500">{shortlist.athleteCount} athletes{showOwner && ` · ${shortlist.ownerEmail}`}</p>
        </div>
        <div className="flex shrink-0">
          <Button variant="ghost" size="sm" onClick={onEdit}>Edit</Button>
          <Button variant="ghost" size="sm" onClick={onDelete}>Delete</Button>
        </div>
      </div>
      {shortlist.notes && <p className="mt-2 text-zinc-700">{shortlist.notes}</p>}
      {removeAthlete.isError && <div className="mt-2"><ErrorMessage error={removeAthlete.error} /></div>}
      {shortlist.athletes.length === 0 ? (
        <p className="mt-3 rounded-lg bg-zinc-50 p-3 text-sm text-zinc-600">Empty. Add athletes from the <Link to="/ranking" className="font-semibold text-emerald-800 underline">ranking</Link>.</p>
      ) : (
        <ul className="mt-3 space-y-2">
          {shortlist.athletes.map((a) => (
            <li key={a.id} className="flex items-center gap-2 rounded-lg border border-zinc-100 bg-zinc-50 p-2">
              <div className="min-w-0 flex-1">
                <Link to={`/athletes/${a.id}`} className="font-semibold hover:underline">{a.fullName}</Link>
                <p className="truncate text-sm text-zinc-600">{a.position} · {a.sportName}</p>
              </div>
              <Button
                variant="ghost"
                size="sm"
                aria-label={`Remove ${a.fullName} from ${shortlist.name}`}
                disabled={removeAthlete.isPending}
                onClick={() => removeAthlete.mutate({ shortlistId: shortlist.id, athleteId: a.id })}
              >
                Remove
              </Button>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

const schema = z.object({
  name: z.string().trim().min(1, 'Give the shortlist a name'),
  notes: z.string(),
})
type FormValues = z.infer<typeof schema>

function ShortlistForm({ current, onDone }: { current: Shortlist | null; onDone: () => void }) {
  const save = useSaveShortlist()
  const { register, handleSubmit, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { name: current?.name ?? '', notes: current?.notes ?? '' },
  })
  return (
    <form noValidate className="space-y-4" onSubmit={handleSubmit((data) => save.mutate({ id: current?.id, data }, { onSuccess: onDone }))}>
      <Input id="name" label="Name" placeholder="U20 strikers" error={errors.name?.message} {...register('name')} />
      <Textarea id="notes" label="Notes" rows={3} {...register('notes')} />
      {save.isError && <ErrorMessage error={save.error} />}
      <div className="flex justify-end gap-2">
        <Button variant="secondary" onClick={onDone}>Cancel</Button>
        <Button type="submit" loading={save.isPending}>{current ? 'Save changes' : 'Create shortlist'}</Button>
      </div>
    </form>
  )
}
