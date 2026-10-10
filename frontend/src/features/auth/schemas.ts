import { z } from 'zod'

// Zod schemas mirror the backend's rules so users get instant feedback,
// but the backend still validates everything (never trust the client).

export const loginSchema = z.object({
  email: z.string().trim().min(1, 'Enter your email').pipe(z.email('Enter a valid email address')),
  password: z.string().min(1, 'Enter your password'),
})
export type LoginForm = z.infer<typeof loginSchema>

export const registerSchema = z
  .object({
    email: z.string().trim().min(1, 'Enter your email').pipe(z.email('Enter a valid email address')),
    password: z.string().min(8, 'Use at least 8 characters'), // same rule as RegisterRequest on the backend
    confirmPassword: z.string(),
  })
  .refine((v) => v.password === v.confirmPassword, { path: ['confirmPassword'], message: 'Passwords do not match' })
export type RegisterForm = z.infer<typeof registerSchema>

export const otpSchema = z.object({
  email: z.string().trim().min(1, 'Enter your email').pipe(z.email('Enter a valid email address')),
  code: z.string().trim().regex(/^\d{6}$/, 'Enter the 6-digit code from the email'),
})
export type OtpForm = z.infer<typeof otpSchema>
