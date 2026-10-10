import { Link } from 'react-router-dom'
import { buttonClass } from './ui'

export function Forbidden() {
  return (
    <div className="mx-auto max-w-md py-16 text-center">
      <h1 className="font-['Barlow_Condensed',sans-serif] text-4xl font-bold">You don't have permission</h1>
      <p className="mt-2 text-zinc-600">
        Your account's role can't open this page. If your role was changed recently, sign out and sign in again.
      </p>
      <Link to="/" className={`${buttonClass('secondary')} mt-6`}>Back to dashboard</Link>
    </div>
  )
}
