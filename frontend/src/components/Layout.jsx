import { Link, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

const links = {
  EMPLOYEE: [['/employee', 'My Incidents'], ['/employee/report', 'Report Incident']],
  TECHNICIAN: [['/technician', 'Assigned'], ['/technician/workload', 'Workload']],
  ADMIN: [['/admin', 'Dashboard'], ['/admin/users', 'Users'], ['/admin/sla', 'SLA']],
}

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  return (
    <div>
      <nav style={{ display: 'flex', gap: 16, padding: 12, background: '#eee' }}>
        <strong>I-ITSM</strong>
        {links[user.role].map(([to, label]) => <Link key={to} to={to}>{label}</Link>)}
        <span style={{ marginLeft: 'auto' }}>{user.name} ({user.role})</span>
        <button onClick={() => { logout(); navigate('/login') }}>Logout</button>
      </nav>
      <main style={{ padding: 16 }}><Outlet /></main>
    </div>
  )
}