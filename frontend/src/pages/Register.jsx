import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { Eye, EyeOff, Lock, Mail, User } from 'lucide-react'
import { useAuth } from '../auth/AuthContext'

export default function Register() {
  const { register: field, handleSubmit, watch, formState: { errors, isSubmitting } } = useForm()
  const { register } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)

  async function onSubmit({ name, email, password }) {
    try {
      setError('')
      await register(name, email, password)
      navigate('/')
    } catch (e) {
      setError(e.response?.status === 409 ? 'This email is already registered' : 'Registration failed')
    }
  }

  const err = (m) => m && <p className="mt-1 text-xs text-red-600">{m.message}</p>

  return (
    <div className="flex min-h-screen items-center justify-center bg-[#eef1f8] px-4 py-10">
      <div className="w-full max-w-[540px] rounded-[18px] border border-slate-200 bg-[#f8f9fd] p-6 shadow-[0_20px_45px_rgba(15,23,42,0.08)] ring-1 ring-white/80 md:p-7">
        <div className="mb-6 flex flex-col items-center gap-2">
          <div className="flex h-11 w-11 items-center justify-center rounded-md border border-slate-200 bg-[#f0f4fb] shadow-inner">
            <div className="flex h-7 w-7 items-center justify-center rounded-sm bg-white text-[11px] font-black text-slate-700 shadow-sm">I</div>
          </div>
          <h1 className="text-2xl font-bold text-slate-800">Create an account</h1>
          <p className="text-sm text-slate-500">Employee registration</p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-4">
          <div>
            <label className="mb-2 block text-sm font-medium text-slate-700">Full name</label>
            <div className="flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-100/70 px-3 py-2.5 focus-within:border-indigo-300 focus-within:ring-2 focus-within:ring-indigo-100">
              <User className="h-4 w-4 text-slate-500" />
              <input
                type="text"
                placeholder="e.g. Sarah Jenkins"
                className="w-full border-0 bg-transparent text-sm text-slate-700 placeholder:text-slate-400 focus:outline-none"
                {...field('name', { required: 'Name is required' })}
              />
            </div>
            {err(errors.name)}
          </div>

          <div>
            <label className="mb-2 block text-sm font-medium text-slate-700">Email</label>
            <div className="flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-100/70 px-3 py-2.5 focus-within:border-indigo-300 focus-within:ring-2 focus-within:ring-indigo-100">
              <Mail className="h-4 w-4 text-slate-500" />
              <input
                type="email"
                placeholder="you@company.com"
                className="w-full border-0 bg-transparent text-sm text-slate-700 placeholder:text-slate-400 focus:outline-none"
                {...field('email', {
                  required: 'Email is required',
                  pattern: { value: /^\S+@\S+\.\S+$/, message: 'Enter a valid email' },
                })}
              />
            </div>
            {err(errors.email)}
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <label className="mb-2 block text-sm font-medium text-slate-700">Password</label>
              <div className="flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-100/70 px-3 py-2.5 focus-within:border-indigo-300 focus-within:ring-2 focus-within:ring-indigo-100">
                <Lock className="h-4 w-4 text-slate-500" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  placeholder="Password"
                  className="w-full border-0 bg-transparent text-sm text-slate-700 placeholder:text-slate-400 focus:outline-none"
                  {...field('password', {
                    required: 'Password is required',
                    minLength: { value: 8, message: 'At least 8 characters' },
                  })}
                />
                <button type="button" onClick={() => setShowPassword((value) => !value)} className="text-slate-500 hover:text-slate-700" aria-label="Toggle password visibility">
                  {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
              {err(errors.password)}
            </div>

            <div>
              <label className="mb-2 block text-sm font-medium text-slate-700">Confirm password</label>
              <div className="flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-100/70 px-3 py-2.5 focus-within:border-indigo-300 focus-within:ring-2 focus-within:ring-indigo-100">
                <Lock className="h-4 w-4 text-slate-500" />
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  placeholder="Confirm password"
                  className="w-full border-0 bg-transparent text-sm text-slate-700 placeholder:text-slate-400 focus:outline-none"
                  {...field('confirm', {
                    validate: (v) => v === watch('password') || 'Passwords do not match',
                  })}
                />
                <button type="button" onClick={() => setShowConfirmPassword((value) => !value)} className="text-slate-500 hover:text-slate-700" aria-label="Toggle confirm password visibility">
                  {showConfirmPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
              {err(errors.confirm)}
            </div>
          </div>

          {error && <p className="rounded-lg bg-red-50 p-2 text-sm text-red-700">{error}</p>}

          <button
            type="submit"
            disabled={isSubmitting}
            className="flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-indigo-600 to-indigo-500 py-3 text-base font-semibold text-white shadow-[0_10px_20px_rgba(79,70,229,0.25)] transition hover:brightness-105 disabled:opacity-70"
          >
            {isSubmitting ? 'Creating...' : 'Register'}
            <span aria-hidden="true">→</span>
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-slate-500">
          Already have an account? <Link to="/login" className="font-medium text-indigo-600 hover:text-indigo-500">Sign in</Link>
        </p>
      </div>
    </div>
  )
}
