/** Horizontal bars drawn with CSS. The number is also written beside each bar so the chart is readable without colour. */
export function BarList({ title, rows, suffix = '' }: {
  title: string
  rows: { label: string; value: number }[]
  suffix?: string
}) {
  const max = Math.max(1, ...rows.map((row) => row.value))
  return (
    <section>
      <h2 className="text-lg font-bold">{title}</h2>
      {rows.length === 0 ? (
        <p className="mt-3 text-zinc-600">No data yet.</p>
      ) : (
        <ul className="mt-4 space-y-3">
          {rows.map((row) => {
            const shown = Number.isInteger(row.value) ? String(row.value) : row.value.toFixed(0)
            return (
              <li key={row.label}>
                <div className="mb-1 flex justify-between gap-3 text-sm">
                  <span className="truncate">{row.label}</span>
                  <span className="font-semibold tabular-nums">{shown}{suffix}</span>
                </div>
                <div className="h-2 rounded-full bg-zinc-100" role="img" aria-label={`${row.label}: ${shown}${suffix}`}>
                  <div className="h-2 rounded-full bg-emerald-700" style={{ width: `${(row.value / max) * 100}%` }} />
                </div>
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}
