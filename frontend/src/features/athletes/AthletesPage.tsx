import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import type { Athlete } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { useAthletes } from '../../hooks/useAthletes'
import { useSports } from '../../hooks/useSports'
import { ageFrom } from '../../lib/format'
import { Icon } from '../../components/Icon'
import { Modal } from '../../components/Modal'
import { Badge, Button, EmptyState, ErrorMessage, PageHeader, Select, Spinner } from '../../components/ui'
import { AthleteFormModal } from './AthleteFormModal'

type Status = 'active' | 'inactive' | 'all'

export default function AthletesPage() {
  const { hasRole } = useAuth()
  const navigate = useNavigate()
  const athletes = useAthletes()
  const sports = useSports()

  const [search, setSearch] = useState('')
  const [sportId, setSportId] = useState('')
  const [status, setStatus] = useState<Status>('active')
  const [filtersOpen, setFiltersOpen] = useState(false)
  const [formOpen, setFormOpen] = useState(false)

  // Filtering happens in the browser: the backend returns all athletes in one list.
  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    return (athletes.data ?? []).filter((a) =>
      (!q || [a.fullName, a.athleteCode, a.position, a.team?.name ?? ''].some((v) => v.toLowerCase().includes(q))) &&
      (!sportId || a.sport.id === sportId) &&
      (status === 'all' || (status === 'active') === a.active),
    )
  }, [athletes.data, search, sportId, status])

  const filters = (
    <>
      <Select id="filter-sport" label="Sport" value={sportId} onChange={(e) => setSportId(e.target.value)}>
        <option value="">All sports</option>
        {sports.data?.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
      </Select>
      <Select id="filter-status" label="Status" value={status} onChange={(e) => setStatus(e.target.value as Status)}>
        <option value="active">Active</option>
        <option value="inactive">Inactive</option>
        <option value="all">All</option>
      </Select>
    </>
  )
  const activeFilterCount = (sportId ? 1 : 0) + (status !== 'active' ? 1 : 0)

  return (
    <>
      <PageHeader
        title="Athletes"
        description={athletes.data && `${filtered.length} of ${athletes.data.length} athletes`}
        actions={hasRole('ADMIN', 'SCOUT') && <Button onClick={() => setFormOpen(true)}><Icon name="plus" /> Add athlete</Button>}
      />

      <div className="mb-4 flex flex-wrap items-end gap-3">
        <div className="relative min-w-0 flex-1 basis-60">
          <label htmlFor="search" className="sr-only">Search athletes</label>
          <Icon name="search" className="pointer-events-none absolute top-1/2 left-3 size-5 -translate-y-1/2 text-zinc-400" />
          <input
            id="search"
            type="search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search name, code, position or team"
            className="w-full rounded-lg border border-zinc-300 bg-white py-2 pr-3 pl-10 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-700"
          />
        </div>
        <div className="hidden gap-3 md:flex">{filters}</div>
        <Button variant="secondary" className="md:hidden" onClick={() => setFiltersOpen(true)}>
          <Icon name="filter" /> Filters{activeFilterCount > 0 && ` (${activeFilterCount})`}
        </Button>
      </div>

      <Modal open={filtersOpen} onClose={() => setFiltersOpen(false)} title="Filters">
        <div className="space-y-4">{filters}</div>
        <Button className="mt-6 w-full" onClick={() => setFiltersOpen(false)}>Show {filtered.length} athletes</Button>
      </Modal>

      {athletes.isPending && <Spinner label="Loading athletes" />}
      {athletes.isError && <ErrorMessage error={athletes.error} />}
      {athletes.data && filtered.length === 0 && (
        <EmptyState title={athletes.data.length === 0 ? 'No athletes yet' : 'No athletes match'}>
          {athletes.data.length === 0 ? 'Add the first athlete to start scouting.' : 'Try another search or clear the filters.'}
        </EmptyState>
      )}

      {filtered.length > 0 && (
        <>
          {/* Mobile: cards */}
          <ul className="space-y-2 md:hidden">
            {filtered.map((a) => (
              <li key={a.id}>
                <Link to={`/athletes/${a.id}`} className="block rounded-xl border border-zinc-200 bg-white p-4 focus-visible:outline-2 focus-visible:outline-emerald-700">
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <p className="font-semibold">{a.fullName}</p>
                      <p className="text-sm text-zinc-600">{a.position} · {a.sport.name}{a.team && ` · ${a.team.name}`}</p>
                    </div>
                    {!a.active && <Badge tone="red">Inactive</Badge>}
                  </div>
                  <p className="mt-1 text-sm text-zinc-500">{a.athleteCode} · {ageLabel(a)}</p>
                </Link>
              </li>
            ))}
          </ul>

          {/* Desktop: table */}
          <div className="hidden overflow-x-auto rounded-xl border border-zinc-200 bg-white md:block">
            <table className="w-full text-left">
              <thead className="border-b border-zinc-200 bg-zinc-50 text-sm text-zinc-600">
                <tr>
                  <th scope="col" className="px-4 py-3 font-semibold">Name</th>
                  <th scope="col" className="px-4 py-3 font-semibold">Code</th>
                  <th scope="col" className="px-4 py-3 font-semibold">Sport</th>
                  <th scope="col" className="px-4 py-3 font-semibold">Position</th>
                  <th scope="col" className="px-4 py-3 font-semibold">Team</th>
                  <th scope="col" className="px-4 py-3 font-semibold">Age</th>
                  <th scope="col" className="px-4 py-3 font-semibold">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-100">
                {filtered.map((a) => (
                  <tr key={a.id} className="cursor-pointer hover:bg-emerald-50/50" onClick={() => navigate(`/athletes/${a.id}`)}>
                    <td className="px-4 py-3 font-semibold">
                      <Link to={`/athletes/${a.id}`} className="hover:underline focus-visible:outline-2 focus-visible:outline-emerald-700">{a.fullName}</Link>
                    </td>
                    <td className="px-4 py-3 text-zinc-600">{a.athleteCode}</td>
                    <td className="px-4 py-3">{a.sport.name}</td>
                    <td className="px-4 py-3">{a.position}</td>
                    <td className="px-4 py-3">{a.team?.name ?? '-'}</td>
                    <td className="px-4 py-3">{ageFrom(a.dateOfBirth) ?? '-'}</td>
                    <td className="px-4 py-3">{a.active ? <Badge tone="green">Active</Badge> : <Badge tone="red">Inactive</Badge>}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      <AthleteFormModal open={formOpen} onClose={() => setFormOpen(false)} onSaved={(a) => navigate(`/athletes/${a.id}`)} />
    </>
  )
}

function ageLabel(a: Athlete) {
  const age = ageFrom(a.dateOfBirth)
  return age === null ? '' : `${age} years`
}
