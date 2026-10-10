import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import type { Criterion, Sport, Team } from '../../api/types'
import {
  useCriteria, useDeleteCriterion, useDeleteSport, useDeleteTeam, useSaveCriterion, useSaveSport, useSaveTeam, useSports, useTeams,
} from '../../hooks/useSports'
import { ConfirmModal, Modal } from '../../components/Modal'
import { Badge, Button, Card, EmptyState, ErrorMessage, Input, PageHeader, Select, Spinner } from '../../components/ui'

/** Desktop: sports list on the left, the chosen sport's criteria and teams on the right. Mobile: stacked. */
export default function SportsPage() {
  const sports = useSports()
  const removeSport = useDeleteSport()
  const [selectedId, setSelectedId] = useState('')
  const [editing, setEditing] = useState<Sport | 'new' | null>(null)
  const [deleting, setDeleting] = useState<Sport | null>(null)

  const selected = sports.data?.find((s) => s.id === selectedId) ?? sports.data?.[0]

  return (
    <>
      <PageHeader
        title="Sports"
        description="Each skill is scored from 0 to 100. The overall score is the average, also out of 100."
        actions={<Button onClick={() => setEditing('new')}>Add sport</Button>}
      />
      {sports.isPending && <Spinner label="Loading sports" />}
      {sports.isError && <ErrorMessage error={sports.error} />}
      {removeSport.isError && <div className="mb-4"><ErrorMessage error={removeSport.error} /></div>}
      {sports.data?.length === 0 && <EmptyState title="No sports yet" action={<Button onClick={() => setEditing('new')}>Add the first sport</Button>} />}

      {sports.data && sports.data.length > 0 && (
        <div className="grid gap-4 md:grid-cols-[16rem_minmax(0,1fr)]">
          <nav aria-label="Sports">
            <ul className="space-y-1">
              {sports.data.map((s) => (
                <li key={s.id}>
                  <button
                    type="button"
                    aria-current={selected?.id === s.id ? 'true' : undefined}
                    onClick={() => setSelectedId(s.id)}
                    className={`flex w-full items-center justify-between rounded-lg px-3 py-2.5 text-left font-medium focus-visible:outline-2 focus-visible:outline-emerald-700 ${
                      selected?.id === s.id ? 'bg-emerald-800 text-white' : 'bg-white hover:bg-zinc-100'
                    }`}
                  >
                    {s.name} <span className="text-sm opacity-75">{s.code}</span>
                  </button>
                </li>
              ))}
            </ul>
          </nav>

          {selected && (
            <div className="space-y-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <h2 className="font-['Barlow_Condensed',sans-serif] text-3xl font-bold">{selected.name} <Badge>{selected.code}</Badge></h2>
                <div className="flex gap-2">
                  <Button variant="secondary" size="sm" onClick={() => setEditing(selected)}>Edit sport</Button>
                  <Button variant="danger" size="sm" onClick={() => setDeleting(selected)}>Delete sport</Button>
                </div>
              </div>
              <CriteriaSection sport={selected} />
              <TeamsSection sport={selected} />
            </div>
          )}
        </div>
      )}

      <Modal open={!!editing} onClose={() => setEditing(null)} title={editing === 'new' ? 'Add sport' : 'Edit sport'}>
        {editing && <SportForm current={editing === 'new' ? null : editing} onDone={(s) => { setEditing(null); if (s) setSelectedId(s.id) }} />}
      </Modal>
      <ConfirmModal
        open={!!deleting}
        title="Delete sport?"
        message={`Delete ${deleting?.name}? Sports that still have athletes, criteria or teams cannot be deleted.`}
        confirmLabel="Delete sport"
        busy={removeSport.isPending}
        onCancel={() => setDeleting(null)}
        onConfirm={() => deleting && removeSport.mutate(deleting.id, { onSuccess: () => setSelectedId(''), onSettled: () => setDeleting(null) })}
      />
    </>
  )
}

// ---------- sport ----------
const sportSchema = z.object({
  name: z.string().trim().min(1, 'Enter the sport name'),
  code: z.string().trim().min(2, 'Enter a short code, e.g. FOOT').max(10, 'Keep the code short'),
})
type SportValues = z.infer<typeof sportSchema>

function SportForm({ current, onDone }: { current: Sport | null; onDone: (s?: Sport) => void }) {
  const save = useSaveSport()
  const { register, handleSubmit, formState: { errors } } = useForm<SportValues>({
    resolver: zodResolver(sportSchema),
    defaultValues: { name: current?.name ?? '', code: current?.code ?? '' },
  })
  return (
    <form noValidate className="space-y-4" onSubmit={handleSubmit((v) => save.mutate({ id: current?.id, data: { ...v, code: v.code.toUpperCase() } }, { onSuccess: onDone }))}>
      <Input id="sport-name" label="Name" placeholder="Football" error={errors.name?.message} {...register('name')} />
      <Input id="sport-code" label="Code" placeholder="FOOT" hint="Stored in capitals. Must be unique." error={errors.code?.message} {...register('code')} />
      {save.isError && <ErrorMessage error={save.error} />}
      <div className="flex justify-end gap-2">
        <Button variant="secondary" onClick={() => onDone()}>Cancel</Button>
        <Button type="submit" loading={save.isPending}>{current ? 'Save sport' : 'Add sport'}</Button>
      </div>
    </form>
  )
}

// ---------- criteria ----------
const criterionSchema = z.object({
  name: z.string().trim().min(1, 'Enter the criterion, e.g. Speed'),
  weight: z.number({ error: 'Enter how much this skill counts' }).positive('Use 1 or more'),
})
type CriterionValues = z.infer<typeof criterionSchema>

function CriteriaSection({ sport }: { sport: Sport }) {
  const criteria = useCriteria(sport.id)
  const remove = useDeleteCriterion(sport.id)
  const [editing, setEditing] = useState<Criterion | 'new' | null>(null)
  const totalWeight = criteria.data?.reduce((sum, c) => sum + c.weight, 0) ?? 0
  const sameImportance = !criteria.data?.length || criteria.data.every((c) => c.weight === criteria.data![0].weight)

  return (
    <Card>
      <div className="flex items-center justify-between gap-2">
        <h3 className="text-lg font-bold">Scoring criteria</h3>
        <Button size="sm" onClick={() => setEditing('new')}>Add criterion</Button>
      </div>
      <p className="text-sm text-zinc-600">Scouts give every skill a mark out of 100. Use importance 1 for each skill so they count equally and the overall mark is a simple average out of 100.</p>
      {criteria.isPending && <Spinner />}
      {criteria.isError && <ErrorMessage error={criteria.error} />}
      {remove.isError && <ErrorMessage error={remove.error} />}
      {criteria.data?.length === 0 && <p className="mt-3 text-zinc-600">No criteria yet. Scouts cannot assess {sport.name} athletes until you add some.</p>}
      <ul className="mt-3 divide-y divide-zinc-100">
        {criteria.data?.map((c) => (
          <li key={c.id} className="flex items-center gap-3 py-2">
            <span className="flex-1 font-medium">{c.name}</span>
            <span className="text-sm text-zinc-600">
              scored out of 100{sameImportance || !totalWeight ? '' : ` · counts for ${Math.round((c.weight / totalWeight) * 100)}%`}
            </span>
            <Button variant="ghost" size="sm" onClick={() => setEditing(c)}>Edit</Button>
            <Button variant="ghost" size="sm" onClick={() => remove.mutate(c.id)} aria-label={`Delete ${c.name}`}>Delete</Button>
          </li>
        ))}
      </ul>
      <Modal open={!!editing} onClose={() => setEditing(null)} title={editing === 'new' ? 'Add criterion' : 'Edit criterion'}>
        {editing && <CriterionForm sportId={sport.id} current={editing === 'new' ? null : editing} onDone={() => setEditing(null)} />}
      </Modal>
    </Card>
  )
}

function CriterionForm({ sportId, current, onDone }: { sportId: string; current: Criterion | null; onDone: () => void }) {
  const save = useSaveCriterion(sportId)
  const { register, handleSubmit, formState: { errors } } = useForm<CriterionValues>({
    resolver: zodResolver(criterionSchema),
    defaultValues: { name: current?.name ?? '', weight: current?.weight ?? 1 },
  })
  return (
    <form noValidate className="space-y-4" onSubmit={handleSubmit((data) => save.mutate({ id: current?.id, data }, { onSuccess: onDone }))}>
      <Input id="criterion-name" label="Skill" placeholder="Speed" error={errors.name?.message} {...register('name')} />
      <Input id="criterion-weight" label="Importance" type="number" step="1" min="1" inputMode="numeric" hint="Use 1 so this skill counts the same as the others. The scout still scores it out of 100." error={errors.weight?.message} {...register('weight', { valueAsNumber: true })} />
      {save.isError && <ErrorMessage error={save.error} />}
      <div className="flex justify-end gap-2">
        <Button variant="secondary" onClick={onDone}>Cancel</Button>
        <Button type="submit" loading={save.isPending}>{current ? 'Save criterion' : 'Add criterion'}</Button>
      </div>
    </form>
  )
}

// ---------- teams ----------
const teamSchema = z.object({
  name: z.string().trim().min(1, 'Enter the team name'),
  city: z.string().trim().min(1, 'Enter the city'),
  level: z.enum(['CLUB', 'ACADEMY']),
})
type TeamValues = z.infer<typeof teamSchema>

function TeamsSection({ sport }: { sport: Sport }) {
  const teams = useTeams(sport.id)
  const remove = useDeleteTeam(sport.id)
  const [editing, setEditing] = useState<Team | 'new' | null>(null)

  return (
    <Card>
      <div className="flex items-center justify-between gap-2">
        <h3 className="text-lg font-bold">Teams</h3>
        <Button size="sm" onClick={() => setEditing('new')}>Add team</Button>
      </div>
      {teams.isPending && <Spinner />}
      {teams.isError && <ErrorMessage error={teams.error} />}
      {remove.isError && <ErrorMessage error={remove.error} />}
      {teams.data?.length === 0 && <p className="mt-3 text-zinc-600">No {sport.name} teams yet.</p>}
      <ul className="mt-3 divide-y divide-zinc-100">
        {teams.data?.map((t) => (
          <li key={t.id} className="flex flex-wrap items-center gap-3 py-2">
            <span className="flex-1 font-medium">{t.name} <span className="font-normal text-zinc-600">{t.city}</span></span>
            <Badge>{t.level === 'CLUB' ? 'Club' : 'Academy'}</Badge>
            <Button variant="ghost" size="sm" onClick={() => setEditing(t)}>Edit</Button>
            <Button variant="ghost" size="sm" onClick={() => remove.mutate(t.id)} aria-label={`Delete ${t.name}`}>Delete</Button>
          </li>
        ))}
      </ul>
      <Modal open={!!editing} onClose={() => setEditing(null)} title={editing === 'new' ? 'Add team' : 'Edit team'}>
        {editing && <TeamForm sportId={sport.id} current={editing === 'new' ? null : editing} onDone={() => setEditing(null)} />}
      </Modal>
    </Card>
  )
}

function TeamForm({ sportId, current, onDone }: { sportId: string; current: Team | null; onDone: () => void }) {
  const save = useSaveTeam(sportId)
  const { register, handleSubmit, formState: { errors } } = useForm<TeamValues>({
    resolver: zodResolver(teamSchema),
    defaultValues: { name: current?.name ?? '', city: current?.city ?? '', level: current?.level ?? 'CLUB' },
  })
  return (
    <form noValidate className="space-y-4" onSubmit={handleSubmit((data) => save.mutate({ id: current?.id, data }, { onSuccess: onDone }))}>
      <Input id="team-name" label="Name" placeholder="APR FC" error={errors.name?.message} {...register('name')} />
      <Input id="team-city" label="City" placeholder="Kigali" error={errors.city?.message} {...register('city')} />
      <Select id="team-level" label="Level" {...register('level')}>
        <option value="CLUB">Club</option>
        <option value="ACADEMY">Academy</option>
      </Select>
      {save.isError && <ErrorMessage error={save.error} />}
      <div className="flex justify-end gap-2">
        <Button variant="secondary" onClick={onDone}>Cancel</Button>
        <Button type="submit" loading={save.isPending}>{current ? 'Save team' : 'Add team'}</Button>
      </div>
    </form>
  )
}
