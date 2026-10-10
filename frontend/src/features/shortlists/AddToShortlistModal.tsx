import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAddToShortlist, useShortlists } from '../../hooks/useShortlists'
import { Modal } from '../../components/Modal'
import { Button, ErrorMessage, Notice, Select, Spinner } from '../../components/ui'

/** Used from the ranking page and the athlete page (club managers and admins). */
export function AddToShortlistModal({ athlete, onClose }: { athlete: { id: string; name: string } | null; onClose: () => void }) {
  return (
    <Modal open={!!athlete} onClose={onClose} title="Add to shortlist">
      {athlete && <AddForm athlete={athlete} onClose={onClose} />}
    </Modal>
  )
}

function AddForm({ athlete, onClose }: { athlete: { id: string; name: string }; onClose: () => void }) {
  const shortlists = useShortlists()
  const add = useAddToShortlist()
  const [shortlistId, setShortlistId] = useState('')

  if (shortlists.isPending) return <Spinner />
  if (shortlists.isError) return <ErrorMessage error={shortlists.error} />
  if (shortlists.data.length === 0) {
    return <p className="text-zinc-700">You have no shortlists yet. <Link to="/shortlists" className="font-semibold text-emerald-800 underline">Create one first</Link>.</p>
  }

  const chosen = shortlists.data.find((s) => s.id === shortlistId)
  return (
    <form
      onSubmit={(e) => {
        e.preventDefault()
        if (shortlistId) add.mutate({ shortlistId, athleteId: athlete.id })
      }}
      className="space-y-4"
    >
      <p className="text-zinc-700">Choose a shortlist for <strong>{athlete.name}</strong>.</p>
      <Select id="shortlistId" label="Shortlist" value={shortlistId} onChange={(e) => { setShortlistId(e.target.value); add.reset() }}>
        <option value="">Choose a shortlist</option>
        {shortlists.data.map((s) => <option key={s.id} value={s.id}>{s.name} ({s.athleteCount})</option>)}
      </Select>
      {add.isError && <ErrorMessage error={add.error} />}
      {add.isSuccess && <Notice tone="success">Added to {chosen?.name}.</Notice>}
      <div className="flex justify-end gap-2">
        <Button variant="secondary" onClick={onClose}>{add.isSuccess ? 'Done' : 'Cancel'}</Button>
        {!add.isSuccess && <Button type="submit" disabled={!shortlistId} loading={add.isPending}>Add to shortlist</Button>}
      </div>
    </form>
  )
}
