import { useEffect, useRef, type ReactNode } from 'react'

/**
 * Built on the native <dialog> element: the browser handles focus trapping,
 * Esc to close and the backdrop, which is good for accessibility.
 * On phones it slides up from the bottom as a sheet; on larger screens it is centred.
 */
export function Modal({ open, onClose, title, children, wide }: {
  open: boolean
  onClose: () => void
  title: string
  children: ReactNode
  wide?: boolean
}) {
  const ref = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal()
    if (!open && dialog.open) dialog.close()
  }, [open])

  return (
    <dialog
      ref={ref}
      onClose={onClose}
      aria-labelledby="modal-title"
      className={`m-0 mt-auto max-h-[90dvh] w-full max-w-none rounded-t-2xl bg-white p-0 text-zinc-900 backdrop:bg-zinc-900/50 md:m-auto md:rounded-2xl ${wide ? 'md:max-w-3xl' : 'md:max-w-lg'}`}
    >
      {open && (
        <div className="p-5">
          <div className="mb-4 flex items-start justify-between gap-4">
            <h2 id="modal-title" className="text-xl font-bold">{title}</h2>
            <button
              type="button"
              onClick={onClose}
              className="rounded-md p-1 text-zinc-500 hover:bg-zinc-100 hover:text-zinc-900 focus-visible:outline-2 focus-visible:outline-emerald-700"
              aria-label="Close"
            >
              <svg viewBox="0 0 24 24" className="size-6" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden><path d="M6 6l12 12M18 6L6 18" /></svg>
            </button>
          </div>
          {children}
        </div>
      )}
    </dialog>
  )
}

/** Small confirm dialog for destructive actions (avoids the blocking window.confirm). */
export function ConfirmModal({ open, title, message, confirmLabel, onConfirm, onCancel, busy }: {
  open: boolean
  title: string
  message: ReactNode
  confirmLabel: string
  onConfirm: () => void
  onCancel: () => void
  busy?: boolean
}) {
  return (
    <Modal open={open} onClose={onCancel} title={title}>
      <div className="text-zinc-700">{message}</div>
      <div className="mt-6 flex justify-end gap-2">
        <button type="button" onClick={onCancel} className="rounded-lg border border-zinc-300 px-4 py-2 font-semibold hover:bg-zinc-100 focus-visible:outline-2 focus-visible:outline-emerald-700">Cancel</button>
        <button type="button" onClick={onConfirm} disabled={busy} className="rounded-lg bg-red-700 px-4 py-2 font-semibold text-white hover:bg-red-800 disabled:opacity-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-700">{confirmLabel}</button>
      </div>
    </Modal>
  )
}
