import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { Eye, EyeOff, Lock, Mail } from 'lucide-react'
import { useAuth } from '../auth/AuthContext'

export default function Login() {
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm()
  const { login } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState('')
  const [showPassword, setShowPassword] = useState(false)

  async function onSubmit({ email, password }) {
    try {
      setError('')
      await login(email, password)
      navigate('/')
    } catch {
      setError('Invalid email or password')
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-[#eef1f8] px-4 py-10">
      <div className="w-full max-w-[470px] rounded-[18px] border border-slate-200 bg-[#f8f9fd] p-6 shadow-[0_20px_45px_rgba(15,23,42,0.08)] ring-1 ring-white/80 md:p-7">
        <div className="mb-6 flex flex-col items-center gap-2">
          <div className="flex h-12 w-12 items-center justify-center rounded-md border border-slate-200 bg-[#f0f4fb] shadow-inner">
            <div className="flex h-7 w-7 items-center justify-center rounded-sm bg-white text-[11px] font-black text-slate-700 shadow-sm">I</div>
          </div>
          <h1 className="text-2xl font-bold text-slate-800">Incident Resolution System</h1>
          <p className="text-sm text-slate-500">User access</p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="mb-2 block text-sm font-medium text-slate-700">Email</label>
            <div className="flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-100/70 px-3 py-2.5 focus-within:border-indigo-300 focus-within:ring-2 focus-within:ring-indigo-100">
              <Mail className="h-4 w-4 text-slate-500" />
              <input
                type="email"
                placeholder="you@company.com"
                className="w-full border-0 bg-transparent text-sm text-slate-700 placeholder:text-slate-400 focus:outline-none"
                {...register('email', { required: 'Email is required' })}
              />
            </div>
            {errors.email && <p className="mt-1 text-xs text-red-600">{errors.email.message}</p>}
          </div>

          <div>
            <label className="mb-2 block text-sm font-medium text-slate-700">Password</label>
            <div className="flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-100/70 px-3 py-2.5 focus-within:border-indigo-300 focus-within:ring-2 focus-within:ring-indigo-100">
              <Lock className="h-4 w-4 text-slate-500" />
              <input
                type={showPassword ? 'text' : 'password'}
                placeholder="Password"
                className="w-full border-0 bg-transparent text-sm text-slate-700 placeholder:text-slate-400 focus:outline-none"
                {...register('password', { required: 'Password is required' })}
              />
              <button type="button" onClick={() => setShowPassword((value) => !value)} className="text-slate-500 hover:text-slate-700" aria-label="Toggle password visibility">
                {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              </button>
            </div>
            {errors.password && <p className="mt-1 text-xs text-red-600">{errors.password.message}</p>}
          </div>

          {error && <p className="rounded-lg bg-red-50 p-2 text-sm text-red-700">{error}</p>}

          <button
            type="submit"
            disabled={isSubmitting}
            className="flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-indigo-600 to-indigo-500 py-3 text-base font-semibold text-white shadow-[0_10px_20px_rgba(79,70,229,0.25)] transition hover:brightness-105 disabled:opacity-70"
          >
            {isSubmitting ? 'Signing in...' : 'Sign in'}
            <span aria-hidden="true">→</span>
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-slate-500">
          New employee? <Link to="/register" className="font-medium text-indigo-600 hover:text-indigo-500">Register</Link>
        </p>

        {import.meta.env.VITE_USE_MOCKS === 'true' && (
          <div className="mt-4 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-500">
            Mock mode: emp@test.com / Emp@1234, tech@test.com / Tech@1234, admin@test.com / Admin@1234
          </div>
        )}
      </div>
    </div>
  )
}
