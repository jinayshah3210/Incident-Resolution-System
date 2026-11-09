import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { ShieldCheck, LogOut, UserCircle2 } from 'lucide-react'
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
    <div className="min-h-screen bg-[#eef1f8] text-slate-800">
      <nav className="border-b border-slate-200 bg-[#f9fafc] px-5 py-3 shadow-sm">
        <div className="mx-auto flex max-w-6xl items-center gap-5">
          <div className="flex items-center gap-3 text-sm font-semibold text-slate-700">
            <div className="flex h-8 w-8 items-center justify-center rounded-md border border-slate-200 bg-[#eef5f4] text-[10px] font-black text-slate-700">I</div>
            <span>Incident Resolution System</span>
          </div>

          <div className="flex items-center gap-6 text-sm text-slate-500">
            {links[user.role].map(([to, label]) => (
              <NavLink key={to} to={to} end className={({ isActive }) => `font-medium transition ${isActive ? 'text-indigo-600' : 'text-slate-500 hover:text-slate-800'}`}>
                {label}
              </NavLink>
            ))}
          </div>

          <div className="ml-auto flex items-center gap-3 text-sm">
            <div className="flex items-center gap-2 rounded-full border border-slate-200 bg-white px-2 py-1.5 shadow-sm">
              <UserCircle2 className="h-4 w-4 text-slate-500" />
              <span className="font-medium text-slate-700">{user.name}</span>
            </div>
            <span className="rounded-full border border-indigo-100 bg-indigo-50 px-2 py-1 text-[10px] font-bold uppercase tracking-[0.12em] text-indigo-700">{user.role}</span>
            <button onClick={() => { logout(); navigate('/login') }} className="flex items-center gap-1 rounded-full border border-slate-200 bg-white px-2.5 py-1.5 text-slate-600 shadow-sm transition hover:border-slate-300 hover:text-slate-800">
              <LogOut className="h-3.5 w-3.5" />
              Logout
            </button>
          </div>
        </div>
      </nav>
      <main className="mx-auto max-w-6xl px-4 py-6 md:px-6"><Outlet /></main>
    </div>
  )
}
