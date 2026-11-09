import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { ShieldCheck, LogOut } from 'lucide-react'
import { useAuth } from '../auth/AuthContext'

const links = {
  EMPLOYEE: [['/employee', 'My Incidents']],
  TECHNICIAN: [['/technician', 'Assigned Incidents']],
  ADMIN: [['/admin', 'Dashboard']],
}

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  return (
    <div className="min-h-screen">
      <nav className="flex items-center gap-6 bg-slate-900 px-6 py-3 text-white">
        <div className="flex items-center gap-2 font-bold">
          <ShieldCheck className="h-5 w-5 text-indigo-400" /> I-ITSM
        </div>
        <div className="flex gap-4 text-sm">
          {links[user.role].map(([to, label]) => (
            <NavLink key={to} to={to} end
              className={({ isActive }) =>
                isActive ? 'text-white font-medium' : 'text-slate-300 hover:text-white'}>
              {label}
            </NavLink>
          ))}
        </div>
        <div className="ml-auto flex items-center gap-3 text-sm">
          <span>{user.name}</span>
          <span className="rounded bg-indigo-500/30 px-2 py-0.5 text-xs">{user.role}</span>
          <button onClick={() => { logout(); navigate('/login') }}
            className="flex items-center gap-1 text-slate-300 hover:text-white">
            <LogOut className="h-4 w-4" /> Logout
          </button>
        </div>
      </nav>
      <main className="mx-auto max-w-6xl p-6"><Outlet /></main>
    </div>
  )
}
