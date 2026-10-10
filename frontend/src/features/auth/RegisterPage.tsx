import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { api } from '../../api/client'
import type { Credentials, RegisterResponse } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { Button, ErrorMessage, Input } from '../../components/ui'
import { AuthLayout, GoogleButton, OrDivider } from './AuthLayout'
import { registerSchema, type RegisterForm } from './schemas'

export default function RegisterPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const { register, handleSubmit, formState: { errors } } = useForm<RegisterForm>({ resolver: zodResolver(registerSchema) })

  const signUp = useMutation({
    mutationFn: (data: Credentials) => api<RegisterResponse>('/auth/register', { method: 'POST', body: data }),
    onSuccess: (res) => {
      navigate(`/verify-email?email=${encodeURIComponent(res.email)}`, { replace: true })
    },
  })

  if (user) return <Navigate to="/" replace />

  return (
    <AuthLayout title="Create your account">
      <GoogleButton label="Sign up with Google" />
      <OrDivider />

      <form
        onSubmit={handleSubmit(({ email, password }) => signUp.mutate({ email, password }))}
        noValidate
        className="space-y-4"
      >
        <Input id="email" label="Email" type="email" autoComplete="email" error={errors.email?.message} {...register('email')} />
        <Input id="password" label="Password" type="password" autoComplete="new-password" hint="At least 8 characters" error={errors.password?.message} {...register('password')} />
        <Input id="confirmPassword" label="Confirm password" type="password" autoComplete="new-password" error={errors.confirmPassword?.message} {...register('confirmPassword')} />
        {signUp.isError && <ErrorMessage error={signUp.error} />}
        <Button type="submit" size="lg" className="w-full" loading={signUp.isPending}>Create account</Button>
        <p className="text-sm text-zinc-500">We email a 6-digit code. You enter it on the next page before you can sign in. New accounts start as athletes.</p>
      </form>

      <p className="mt-6 text-center text-zinc-600">
        Already have an account? <Link to="/login" className="font-semibold text-emerald-800 underline-offset-2 hover:underline">Sign in</Link>
      </p>
    </AuthLayout>
  )
}
