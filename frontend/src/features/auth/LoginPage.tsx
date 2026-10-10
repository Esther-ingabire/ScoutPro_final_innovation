import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { api, ApiError } from '../../api/client'
import type { AuthResponse, Credentials } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { Button, ErrorMessage, Input, Notice } from '../../components/ui'
import { AuthLayout, GoogleButton, OrDivider } from './AuthLayout'
import { loginSchema, type LoginForm } from './schemas'

// Error codes the backend puts in /login?error=... after a failed Google login.
const OAUTH_ERRORS: Record<string, string> = {
  email_not_verified: 'Your Google email address is not verified. Verify it with Google, then try again.',
  account_disabled: 'This account is disabled. Contact an administrator.',
  oauth_failed: 'Google sign-in did not complete. Try again.',
}

export default function LoginPage() {
  const { user, signIn } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const { register, handleSubmit, getValues, formState: { errors } } = useForm<LoginForm>({ resolver: zodResolver(loginSchema) })

  const login = useMutation({
    mutationFn: (data: Credentials) => api<AuthResponse>('/auth/login', { method: 'POST', body: data }),
    onSuccess: (res) => {
      signIn(res.token, res.refreshToken)
      // Always open the dashboard. Sending people back to the last address
      // showed "no permission" when that address belonged to another role.
      navigate('/', { replace: true })
    },
  })

  const oauthError = params.get('error')
  const expired = params.get('expired')

  // Already signed in: skip the form. But always show a Google error, even to a signed-in user.
  if (user && !oauthError) return <Navigate to="/" replace />

  return (
    <AuthLayout title="Sign in">
      <div className="space-y-3">
        {oauthError && <ErrorMessage error={new Error(OAUTH_ERRORS[oauthError] ?? OAUTH_ERRORS.oauth_failed)} />}
        {expired && !oauthError && <Notice>Your session expired. Sign in again to continue.</Notice>}
      </div>

      <div className="mt-4">
        <GoogleButton label="Sign in with Google" />
      </div>
      <OrDivider />

      <form onSubmit={handleSubmit((data) => login.mutate(data))} noValidate className="space-y-4">
        <Input id="email" label="Email" type="email" autoComplete="email" error={errors.email?.message} {...register('email')} />
        <Input id="password" label="Password" type="password" autoComplete="current-password" error={errors.password?.message} {...register('password')} />
        {login.isError && <ErrorMessage error={login.error} />}
        {login.error instanceof ApiError && login.error.status === 403 && login.error.message.includes('6-digit') && (
          <p className="text-sm">
            <Link
              to={`/verify-email?email=${encodeURIComponent(getValues('email') ?? '')}`}
              className="font-semibold text-emerald-800 underline-offset-2 hover:underline"
            >
              Enter the code
            </Link>
          </p>
        )}
        <Button type="submit" size="lg" className="w-full" loading={login.isPending}>Sign in</Button>
      </form>

      <p className="mt-6 text-center text-zinc-600">
        New to ScoutPro? <Link to="/register" className="font-semibold text-emerald-800 underline-offset-2 hover:underline">Create an account</Link>
      </p>
    </AuthLayout>
  )
}
