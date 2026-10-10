import { useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { ALL_ROLES, type Role, type Scout, type User } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { useCreateScout, useDeactivateScout, useScouts, useUpdateRoles, useUsers } from '../../hooks/useUsers'
import { ROLE_LABELS } from '../../lib/format'
import { Modal } from '../../components/Modal'
import { Badge, Button, ErrorMessage, Input, Notice, PageHeader, Spinner } from '../../components/ui'

export default function UsersPage() {
  const users = useUsers()
  const scouts = useScouts()
  const deactivateScout = useDeactivateScout()
  const [search, setSearch] = useState('')
  const [rolesFor, setRolesFor] = useState<User | null>(null)
  const [scoutFor, setScoutFor] = useState<User | null>(null)

  const scoutByUser = useMemo(() => new Map((scouts.data ?? []).map((s) => [s.user.id, s])), [scouts.data])
  const filtered = (users.data ?? []).filter((u) => u.email.toLowerCase().includes(search.trim().toLowerCase()))

  return (
    <>
      <PageHeader title="Users and roles" description="Role changes take effect the next time the user signs in." />
      <div className="mb-4 max-w-sm">
        <Input id="user-search" label="Search by email" type="search" value={search} onChange={(e) => setSearch(e.target.value)} />
      </div>
      {users.isPending && <Spinner label="Loading users" />}
      {users.isError && <ErrorMessage error={users.error} />}
      {deactivateScout.isError && <div className="mb-3"><ErrorMessage error={deactivateScout.error} /></div>}

      <ul className="space-y-2">
        {filtered.map((u) => {
          const scout = scoutByUser.get(u.id)
          const roles = u.roles.map((r) => r.name)
          return (
            <li key={u.id} className="flex flex-col gap-3 rounded-xl border border-zinc-200 bg-white p-4 md:flex-row md:items-center">
              <div className="min-w-0 flex-1">
                <p className="truncate font-semibold">{u.email}</p>
                <p className="text-sm text-zinc-600">
                  {u.provider === 'GOOGLE' ? 'Google account' : 'Email and password'}
                  {!u.enabled && ' · disabled'}
                  {scout && ` · scout ${scout.scoutCode}${scout.active ? '' : ' (inactive)'}`}
                </p>
              </div>
              <div className="flex flex-wrap gap-1">{roles.map((r) => <Badge key={r} tone={r === 'ADMIN' ? 'amber' : 'green'}>{ROLE_LABELS[r]}</Badge>)}</div>
              <div className="flex flex-wrap gap-2">
                <Button variant="secondary" size="sm" onClick={() => setRolesFor(u)}>Edit roles</Button>
                {roles.includes('SCOUT') && !scout && scouts.data && (
                  <Button size="sm" onClick={() => setScoutFor(u)}>Create scout profile</Button>
                )}
                {scout?.active && (
                  <Button variant="ghost" size="sm" loading={deactivateScout.isPending && deactivateScout.variables === scout.id} onClick={() => deactivateScout.mutate(scout.id)}>
                    Deactivate scout
                  </Button>
                )}
              </div>
            </li>
          )
        })}
      </ul>

      <Modal open={!!rolesFor} onClose={() => setRolesFor(null)} title="Edit roles">
        {rolesFor && <RolesForm user={rolesFor} onDone={() => setRolesFor(null)} />}
      </Modal>
      <Modal open={!!scoutFor} onClose={() => setScoutFor(null)} title="Create scout profile">
        {scoutFor && <ScoutForm user={scoutFor} onDone={() => setScoutFor(null)} />}
      </Modal>
    </>
  )
}

function RolesForm({ user, onDone }: { user: User; onDone: () => void }) {
  const { user: me } = useAuth()
  const update = useUpdateRoles()
  const [roles, setRoles] = useState<Role[]>(user.roles.map((r) => r.name))
  const isMe = me?.userId === user.id

  function toggle(role: Role) {
    setRoles((current) => (current.includes(role) ? current.filter((r) => r !== role) : [...current, role]))
  }

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault()
        if (roles.length > 0) update.mutate({ userId: user.id, roles }, { onSuccess: onDone })
      }}
      className="space-y-4"
    >
      <p className="text-zinc-700">{user.email}</p>
      <fieldset className="space-y-2">
        <legend className="sr-only">Roles</legend>
        {ALL_ROLES.map((role) => (
          <label key={role} className="flex items-center gap-3 rounded-lg border border-zinc-200 p-3 has-checked:border-emerald-700 has-checked:bg-emerald-50">
            <input type="checkbox" className="size-5 accent-emerald-800" checked={roles.includes(role)} onChange={() => toggle(role)} />
            <span className="font-medium">{ROLE_LABELS[role]}</span>
          </label>
        ))}
      </fieldset>
      {roles.length === 0 && <p className="text-sm text-red-700">Choose at least one role.</p>}
      {isMe && <Notice>You cannot remove your own admin role.</Notice>}
      {update.isError && <ErrorMessage error={update.error} />}
      <div className="flex justify-end gap-2">
        <Button variant="secondary" onClick={onDone}>Cancel</Button>
        <Button type="submit" disabled={roles.length === 0} loading={update.isPending}>Save roles</Button>
      </div>
    </form>
  )
}

const scoutSchema = z.object({
  scoutCode: z.string().trim().min(1, 'Enter a code, e.g. SC-0002'),
  fullName: z.string().trim().min(2, 'Enter the full name'),
  phoneNumber: z.string().trim().min(1, 'Enter a phone number'),
  organization: z.string().trim().min(1, 'Enter the club or academy'),
})
type ScoutFormValues = z.infer<typeof scoutSchema>

function ScoutForm({ user, onDone }: { user: User; onDone: (s?: Scout) => void }) {
  const create = useCreateScout()
  const { register, handleSubmit, formState: { errors } } = useForm<ScoutFormValues>({ resolver: zodResolver(scoutSchema) })
  return (
    <form noValidate className="space-y-4" onSubmit={handleSubmit((data) => create.mutate({ userId: user.id, data }, { onSuccess: onDone }))}>
      <p className="text-zinc-700">For {user.email}. The email comes from the account.</p>
      <Input id="scoutCode" label="Scout code" placeholder="SC-0002" error={errors.scoutCode?.message} {...register('scoutCode')} />
      <Input id="fullName" label="Full name" error={errors.fullName?.message} {...register('fullName')} />
      <Input id="phoneNumber" label="Phone number" type="tel" placeholder="+250 7xx xxx xxx" error={errors.phoneNumber?.message} {...register('phoneNumber')} />
      <Input id="organization" label="Organization" placeholder="Club or academy" error={errors.organization?.message} {...register('organization')} />
      {create.isError && <ErrorMessage error={create.error} />}
      <div className="flex justify-end gap-2">
        <Button variant="secondary" onClick={() => onDone()}>Cancel</Button>
        <Button type="submit" loading={create.isPending}>Create profile</Button>
      </div>
    </form>
  )
}
