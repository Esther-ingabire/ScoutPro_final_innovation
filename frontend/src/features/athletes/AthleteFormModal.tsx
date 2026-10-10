import { useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import type { Athlete } from '../../api/types'
import { useSaveAthlete } from '../../hooks/useAthletes'
import { useSports, useTeams } from '../../hooks/useSports'
import { todayIso } from '../../lib/format'
import { Modal } from '../../components/Modal'
import { Button, ErrorMessage, Input, Select } from '../../components/ui'

const schema = z.object({
  sportId: z.string().min(1, 'Choose a sport'),
  teamId: z.string(), // '' = no team
  athleteCode: z.string().trim().min(1, 'Enter a code, e.g. ATH-0001'),
  fullName: z.string().trim().min(2, 'Enter the full name'),
  dateOfBirth: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'Enter the date of birth').refine((d) => d < todayIso(), 'Date of birth must be in the past'),
  position: z.string().trim().min(1, 'Enter a position, e.g. Striker'),
  nationality: z.string().trim().min(1, 'Enter the nationality'),
  contactNumber: z.string().trim().min(1, 'Enter a contact number'),
})
type FormValues = z.infer<typeof schema>

export function AthleteFormModal({ open, onClose, athlete, onSaved }: {
  open: boolean
  onClose: () => void
  athlete?: Athlete // given = edit mode
  onSaved?: (saved: Athlete) => void
}) {
  return (
    <Modal open={open} onClose={onClose} title={athlete ? 'Edit athlete' : 'Add athlete'} wide>
      <AthleteForm athlete={athlete} onDone={(saved) => { onSaved?.(saved); onClose() }} onCancel={onClose} />
    </Modal>
  )
}

function AthleteForm({ athlete, onDone, onCancel }: { athlete?: Athlete; onDone: (a: Athlete) => void; onCancel: () => void }) {
  const sports = useSports()
  const save = useSaveAthlete()
  const { register, control, handleSubmit, setValue, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      sportId: athlete?.sport.id ?? '',
      teamId: athlete?.team?.id ?? '',
      athleteCode: athlete?.athleteCode ?? '',
      fullName: athlete?.fullName ?? '',
      dateOfBirth: athlete?.dateOfBirth ?? '',
      position: athlete?.position ?? '',
      nationality: athlete?.nationality ?? 'Rwandan',
      contactNumber: athlete?.contactNumber ?? '',
    },
  })
  const sportId = useWatch({ control, name: 'sportId' })
  const teams = useTeams(sportId || undefined) // only teams of the chosen sport (backend rule: 422 otherwise)

  function onSubmit({ sportId, teamId, ...data }: FormValues) {
    save.mutate({ id: athlete?.id, sportId, teamId: teamId || undefined, data }, { onSuccess: onDone })
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
      <div className="grid gap-4 md:grid-cols-2">
        <Select
          id="sportId"
          label="Sport"
          disabled={!!athlete}
          hint={athlete ? "An athlete's sport cannot change." : undefined}
          error={errors.sportId?.message}
          {...register('sportId', { onChange: () => setValue('teamId', '') })}
        >
          <option value="">Choose a sport</option>
          {sports.data?.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
        </Select>
        <Select id="teamId" label="Team (optional)" disabled={!sportId} {...register('teamId')}>
          <option value="">No team</option>
          {teams.data?.map((t) => <option key={t.id} value={t.id}>{t.name}, {t.city}</option>)}
        </Select>
        <Input id="athleteCode" label="Athlete code" placeholder="ATH-0001" error={errors.athleteCode?.message} {...register('athleteCode')} />
        <Input id="fullName" label="Full name" autoComplete="off" error={errors.fullName?.message} {...register('fullName')} />
        <Input id="dateOfBirth" label="Date of birth" type="date" max={todayIso()} error={errors.dateOfBirth?.message} {...register('dateOfBirth')} />
        <Input id="position" label="Position" placeholder="Striker" error={errors.position?.message} {...register('position')} />
        <Input id="nationality" label="Nationality" error={errors.nationality?.message} {...register('nationality')} />
        <Input id="contactNumber" label="Contact number" type="tel" placeholder="+250 7xx xxx xxx" error={errors.contactNumber?.message} {...register('contactNumber')} />
      </div>
      {save.isError && <ErrorMessage error={save.error} />}
      <div className="flex justify-end gap-2">
        <Button variant="secondary" onClick={onCancel}>Cancel</Button>
        <Button type="submit" loading={save.isPending}>{athlete ? 'Save changes' : 'Add athlete'}</Button>
      </div>
    </form>
  )
}
