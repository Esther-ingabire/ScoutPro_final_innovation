import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import type { PhysicalProfile } from '../../api/types'
import { usePhysicalProfile, useSavePhysicalProfile } from '../../hooks/useAthletes'
import { formatDate, todayIso } from '../../lib/format'
import { Button, Card, ErrorMessage, Input, Select, Spinner } from '../../components/ui'

// Same ranges as the backend validation (CLAUDE.md section 6).
const schema = z.object({
  heightCm: z.number({ error: 'Enter the height' }).min(50, 'Between 50 and 250 cm').max(250, 'Between 50 and 250 cm'),
  weightKg: z.number({ error: 'Enter the weight' }).min(20, 'Between 20 and 200 kg').max(200, 'Between 20 and 200 kg'),
  dominantSide: z.enum(['LEFT', 'RIGHT', 'BOTH']),
  measuredAt: z.string(),
})
type FormValues = z.infer<typeof schema>

const SIDE_LABELS = { LEFT: 'Left', RIGHT: 'Right', BOTH: 'Both' }

export function PhysicalProfileCard({ athleteId, canEdit }: { athleteId: string; canEdit: boolean }) {
  const profile = usePhysicalProfile(athleteId)
  const [editing, setEditing] = useState(false)

  return (
    <Card>
      <div className="flex items-center justify-between gap-2">
        <h2 className="text-lg font-bold">Physical profile</h2>
        {canEdit && !editing && profile.data !== undefined && (
          <Button variant="ghost" size="sm" onClick={() => setEditing(true)}>{profile.data ? 'Update' : 'Add'}</Button>
        )}
      </div>
      {profile.isPending && <Spinner />}
      {profile.isError && <ErrorMessage error={profile.error} />}
      {editing ? (
        <ProfileForm athleteId={athleteId} current={profile.data ?? null} onDone={() => setEditing(false)} />
      ) : profile.data ? (
        <dl className="mt-3 grid grid-cols-3 gap-3">
          <Fact label="Height" value={`${profile.data.heightCm} cm`} />
          <Fact label="Weight" value={`${profile.data.weightKg} kg`} />
          <Fact label="Dominant side" value={SIDE_LABELS[profile.data.dominantSide]} />
          <p className="col-span-3 text-sm text-zinc-500">Measured {formatDate(profile.data.measuredAt)}</p>
        </dl>
      ) : (
        profile.data === null && <p className="mt-2 text-zinc-600">No measurements recorded yet.</p>
      )}
    </Card>
  )
}

function Fact({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-sm text-zinc-500">{label}</dt>
      <dd className="font-['Barlow_Condensed',sans-serif] text-2xl font-bold">{value}</dd>
    </div>
  )
}

function ProfileForm({ athleteId, current, onDone }: { athleteId: string; current: PhysicalProfile | null; onDone: () => void }) {
  const save = useSavePhysicalProfile(athleteId)
  const { register, handleSubmit, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      heightCm: current?.heightCm,
      weightKg: current?.weightKg,
      dominantSide: current?.dominantSide ?? 'RIGHT',
      measuredAt: todayIso(),
    },
  })

  return (
    <form
      noValidate
      className="mt-3 space-y-3"
      onSubmit={handleSubmit((v) => save.mutate({ ...v, measuredAt: v.measuredAt || undefined }, { onSuccess: onDone }))}
    >
      <div className="grid grid-cols-2 gap-3">
        {/* valueAsNumber: the input gives a number (or NaN when empty) instead of a string */}
        <Input id="heightCm" label="Height (cm)" type="number" inputMode="decimal" step="0.1" error={errors.heightCm?.message} {...register('heightCm', { valueAsNumber: true })} />
        <Input id="weightKg" label="Weight (kg)" type="number" inputMode="decimal" step="0.1" error={errors.weightKg?.message} {...register('weightKg', { valueAsNumber: true })} />
        <Select id="dominantSide" label="Dominant side" {...register('dominantSide')}>
          <option value="RIGHT">Right</option>
          <option value="LEFT">Left</option>
          <option value="BOTH">Both</option>
        </Select>
        <Input id="measuredAt" label="Measured on" type="date" max={todayIso()} {...register('measuredAt')} />
      </div>
      {save.isError && <ErrorMessage error={save.error} />}
      <div className="flex justify-end gap-2">
        <Button variant="secondary" size="sm" onClick={onDone}>Cancel</Button>
        <Button type="submit" size="sm" loading={save.isPending}>Save profile</Button>
      </div>
    </form>
  )
}
