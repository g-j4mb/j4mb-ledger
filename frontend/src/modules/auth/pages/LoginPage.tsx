import { useState } from 'react'
import { useNavigate, useLocation } from 'react-router'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useAuth } from '@/shell/auth/AuthContext'
import { Button } from '@/shell/components/ui/Button'
import { Input } from '@/shell/components/ui/Input'

const schema = z.object({
  email: z.string().email('Please enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
})

type FormData = z.infer<typeof schema>

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [serverError, setServerError] = useState('')

  const from = (location.state as { from?: { pathname: string } })?.from?.pathname ?? '/dashboard'

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({
    resolver: zodResolver(schema),
  })

  const onSubmit = async (data: FormData) => {
    setServerError('')
    try {
      await login(data.email, data.password)
      navigate(from, { replace: true })
    } catch (e) {
      setServerError(e instanceof Error ? e.message : 'Invalid email or password')
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-[#f8fafc] p-4">
      <div className="w-full max-w-sm">
        {/* Brand */}
        <div className="mb-8 text-center">
          <div className="inline-flex h-12 w-12 items-center justify-center rounded-xl bg-[#1d4ed8] mb-4">
            <span className="text-lg font-bold text-white">J4</span>
          </div>
          <h1 className="text-2xl font-bold text-[#0f172a]">J4MB ERP</h1>
          <p className="mt-1 text-sm text-[#64748b]">Sign in to your account</p>
        </div>

        {/* Card */}
        <div className="rounded-xl border border-[#e2e8f0] bg-white p-8 shadow-sm">
          <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
            <Input
              id="email"
              type="email"
              label="Email"
              placeholder="admin@j4mb.com"
              autoComplete="email"
              error={errors.email?.message}
              {...register('email')}
            />

            <Input
              id="password"
              type="password"
              label="Password"
              placeholder="••••••••"
              autoComplete="current-password"
              error={errors.password?.message}
              {...register('password')}
            />

            {serverError && (
              <div className="rounded-md bg-red-50 border border-red-200 px-3 py-2">
                <p className="text-sm text-red-600">{serverError}</p>
              </div>
            )}

            <Button type="submit" loading={isSubmitting} className="mt-2 w-full">
              Sign In
            </Button>
          </form>

          {/* Dev hint */}
          <p className="mt-6 rounded-md bg-slate-50 p-3 text-center text-xs text-[#64748b]">
            Demo: <span className="font-mono font-medium">admin@j4mb.com</span> /{' '}
            <span className="font-mono font-medium">demo123</span>
          </p>
        </div>
      </div>
    </div>
  )
}
