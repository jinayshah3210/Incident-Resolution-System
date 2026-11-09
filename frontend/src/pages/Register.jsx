import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { ShieldCheck } from 'lucide-react'
import { useAuth } from '../auth/AuthContext'

export default function Register() {
  const { register: field, handleSubmit, watch, formState: { errors, isSubmitting } } = useForm()
  const { register } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState('')

  async function onSubmit({ name, email, password }) {
    try {
      setError('')
      await register(name, email, password)
      navigate('/')
    } catch (e) {
      setError(e.response?.status === 409 ? 'This email is already registered' : 'Registration failed')
    }
  }

  const input = 'w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-200'
  const err = (m) => m && <p className="mt-1 text-xs text-red-600">{m.message}</p>

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-indigo-600 to-slate-900 p-4">
      <div className="w-full max-w-md rounded-2xl bg-white p-8 shadow-xl">
        <div className="mb-6 flex flex-col items-center gap-2">
          <ShieldCheck className="h-10 w-10 text-indigo-600" />
          <h1 className="text-2xl font-bold">Create an account</h1>
          <p className="text-sm text-slate-500">Employee registration</p>
        </div>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="mb-1 block text-sm font-medium">Full name</label>
            <input className={input} {...field('name', { required: 'Name is required' })} />
            {err(errors.name)}
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">Email</label>
            <input className={input} {...field('email', {
              required: 'Email is required',
              pattern: { value: /^\S+@\S+\.\S+$/, message: 'Enter a valid email' } })} />
            {err(errors.email)}
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">Password</label>
            <input type="password" className={input} {...field('password', {
              required: 'Password is required',
              minLength: { value: 8, message: 'At least 8 characters' } })} />
            {err(errors.password)}
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">Confirm password</label>
            <input type="password" className={input} {...field('confirm', {
              validate: (v) => v === watch('password') || 'Passwords do not match' })} />
            {err(errors.confirm)}
          </div>
          {error && <p className="rounded-lg bg-red-50 p-2 text-sm text-red-700">{error}</p>}
          <button type="submit" disabled={isSubmitting}
            className="w-full rounded-lg bg-indigo-600 py-2 font-medium text-white hover:bg-indigo-700 disabled:opacity-60">
            {isSubmitting ? 'Creating...' : 'Register'}
          </button>
        </form>
        <p className="mt-4 text-center text-sm text-slate-500">
          Already have an account? <Link to="/login" className="text-indigo-600 hover:underline">Sign in</Link>
        </p>
      </div>
    </div>
  )
}
