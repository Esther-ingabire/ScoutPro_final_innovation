import { useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from './AuthContext'
import { Spinner } from '../components/ui'

/**
 * Google login lands here: http://localhost:5173/oauth2/callback#token=<JWT>
 * The token is in the URL fragment (#...). Browsers never send the fragment to
 * a server, so it does not end up in server logs.
 */
export default function OAuthCallback() {
  const { signIn, signOut } = useAuth()
  const navigate = useNavigate()
  const handled = useRef(false) // React StrictMode runs effects twice in dev; only handle once

  useEffect(() => {
    if (handled.current) return
    handled.current = true

    const hash = new URLSearchParams(window.location.hash.slice(1))
    const token = hash.get('token')
    const refreshToken = hash.get('refreshToken')
    // Remove the tokens from the address bar and history so they are not copied or bookmarked.
    window.history.replaceState(null, '', window.location.pathname)

    if (token && signIn(token, refreshToken)) {
      navigate('/', { replace: true })
    } else {
      signOut() // never stay signed in as a previous user after a failed Google login
      navigate('/login?error=oauth_failed', { replace: true })
    }
  }, [signIn, signOut, navigate])

  return (
    <main className="grid min-h-dvh place-items-center">
      <Spinner label="Signing you in" />
    </main>
  )
}
