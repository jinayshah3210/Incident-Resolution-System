import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { ShieldCheck } from 'lucide-react'
import { useAuth } from '../auth/AuthContext'

export default function Login() {
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm()
  const { login } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState('')

  async function onSubmit({ email, password }) {
    try {
      setError('')
      await login(email, password)
      navigate('/')
    } catch {
      setError('Invalid email or password')
    }
  }

  const input = 'w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-200'

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-indigo-600 to-slate-900 p-4">
      <div className="w-full max-w-md rounded-2xl bg-white p-8 shadow-xl">
        <div className="mb-6 flex flex-col items-center gap-2">
          <ShieldCheck className="h-10 w-10 text-indigo-600" />
          <h1 className="text-2xl font-bold">I-ITSM</h1>
          <p className="text-sm text-slate-500">Incident Resolution System</p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="mb-1 block text-sm font-medium">Email</label>
            <input className={input} placeholder="you@company.com"
              {...register('email', { required: 'Email is required' })} />
            {errors.email && <p className="mt-1 text-xs text-red-600">{errors.email.message}</p>}
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">Password</label>
            <input type="password" className={input} placeholder="Password"
              {...register('password', { required: 'Password is required' })} />
            {errors.password && <p className="mt-1 text-xs text-red-600">{errors.password.message}</p>}
          </div>
          {error && <p className="rounded-lg bg-red-50 p-2 text-sm text-red-700">{error}</p>}
          <button type="submit" disabled={isSubmitting}
            className="w-full rounded-lg bg-indigo-600 py-2 font-medium text-white hover:bg-indigo-700 disabled:opacity-60">
            {isSubmitting ? 'Signing in...' : 'Sign in'}
          </button>
        </form>

        <p className="mt-4 text-center text-sm text-slate-500">
          New employee? <Link to="/register" className="text-indigo-600 hover:underline">Register</Link>
        </p>
        {import.meta.env.VITE_USE_MOCKS === 'true' && (
          <div className="mt-4 rounded-lg bg-slate-50 p-3 text-xs text-slate-500">
            Mock mode: emp@test.com / Emp@1234, tech@test.com / Tech@1234, admin@test.com / Admin@1234
          </div>
        )}
      </div>
    </div>
  )
}
