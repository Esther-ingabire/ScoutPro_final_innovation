import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../../api/client'
import type { AuthResponse, RegisterResponse } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { Button, ErrorMessage, Input, Notice } from '../../components/ui'
import { AuthLayout } from './AuthLayout'
import { otpSchema, type OtpForm } from './schemas'

export default function VerifyEmailPage() {
  const { user, signIn } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const emailed = params.get('email') ?? ''

  const { register, handleSubmit, getValues, formState: { errors } } = useForm<OtpForm>({
    resolver: zodResolver(otpSchema),
    defaultValues: { email: emailed, code: '' },
  })

  const confirm = useMutation({
    mutationFn: (data: OtpForm) => api<AuthResponse>('/auth/verify-otp', { method: 'POST', body: data }),
    onSuccess: (res) => {
      signIn(res.token, res.refreshToken)
      navigate('/', { replace: true })
    },
  })

  const resend = useMutation({
    mutationFn: () => api<RegisterResponse>('/auth/resend-otp', {
      method: 'POST',
      body: { email: getValues('email') },
    }),
  })

  if (user) return <Navigate to="/" replace />

  return (
    <AuthLayout title="Enter your code">
      <p className="mb-4 text-zinc-600">
        We sent a 6-digit code to your email. It expires in 10 minutes. Enter it here to finish creating the account and sign in.
      </p>
      <form onSubmit={handleSubmit((data) => confirm.mutate(data))} noValidate className="space-y-4">
        <Input id="email" label="Email" type="email" autoComplete="email" error={errors.email?.message} {...register('email')} />
        <Input
          id="code"
          label="Confirmation code"
          inputMode="numeric"
          autoComplete="one-time-code"
          maxLength={6}
          error={errors.code?.message}
          {...register('code')}
        />
        {confirm.isError && <ErrorMessage error={confirm.error} />}
        {resend.isError && <ErrorMessage error={resend.error} />}
        {resend.isSuccess && <Notice tone="success">{resend.data.message}</Notice>}
        <Button type="submit" size="lg" className="w-full" loading={confirm.isPending}>Confirm and sign in</Button>
      </form>
      <p className="mt-4 text-center">
        <Button variant="ghost" loading={resend.isPending} onClick={() => resend.mutate()}>
          Send a new code
        </Button>
      </p>
      <p className="mt-4 text-center text-zinc-600">
        Already confirmed? <Link to="/login" className="font-semibold text-emerald-800 underline-offset-2 hover:underline">Sign in</Link>
      </p>
    </AuthLayout>
  )
}
