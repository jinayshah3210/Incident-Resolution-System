import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './auth/AuthContext'
import ProtectedRoute from './auth/ProtectedRoute'
import Layout from './components/Layout'
import Login from './pages/Login'
import EmployeeHome from './pages/employee/EmployeeHome'
import TechnicianHome from './pages/technician/TechnicianHome'
import AdminHome from './pages/admin/AdminHome'

function HomeRedirect() {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  return <Navigate to={`/${user.role.toLowerCase()}`} replace />
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/" element={<HomeRedirect />} />

          <Route element={<ProtectedRoute allowedRoles={['EMPLOYEE']} />}>
            <Route element={<Layout />}>
              <Route path="/employee" element={<EmployeeHome />} />
            </Route>
          </Route>

          <Route element={<ProtectedRoute allowedRoles={['TECHNICIAN']} />}>
            <Route element={<Layout />}>
              <Route path="/technician" element={<TechnicianHome />} />
            </Route>
          </Route>

          <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
            <Route element={<Layout />}>
              <Route path="/admin" element={<AdminHome />} />
            </Route>
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}