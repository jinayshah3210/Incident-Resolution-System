import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function Login() {
  const { register, handleSubmit, formState: { errors } } = useForm()
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

  return (
    <div style={{ maxWidth: 360, margin: '80px auto' }}>
      <h2>I-ITSM Login</h2>
      <form onSubmit={handleSubmit(onSubmit)}>
        <input placeholder="Email" {...register('email', { required: 'Email is required' })} />
        {errors.email && <p>{errors.email.message}</p>}
        <input type="password" placeholder="Password"
          {...register('password', { required: 'Password is required' })} />
        {errors.password && <p>{errors.password.message}</p>}
        {error && <p style={{ color: 'red' }}>{error}</p>}
        <button type="submit">Login</button>
      </form>
      <p>Demo: emp@test.com, tech@test.com, admin@test.com (any password)</p>
    </div>
  )
}