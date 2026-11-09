import { http, HttpResponse } from 'msw'

// Mock "database". The real backend will store BCrypt hashes, never plain passwords.
let users = [
  { id: 1, name: 'Asha Employee',   email: 'emp@test.com',   password: 'Emp@1234',   role: 'EMPLOYEE' },
  { id: 2, name: 'Ravi Technician', email: 'tech@test.com',  password: 'Tech@1234',  role: 'TECHNICIAN' },
  { id: 3, name: 'Meera Admin',     email: 'admin@test.com', password: 'Admin@1234', role: 'ADMIN' },
]

let incidents = [
  { id: 'INC1001', title: 'Payment app unavailable', category: 'APPLICATION',
    service: 'Payment', priority: 'P1', status: 'ASSIGNED',
    reporter: 'Asha Employee', technician: 'Ravi Technician',
    createdAt: '2026-11-07T10:05:00Z' },
  { id: 'INC1002', title: 'VPN keeps disconnecting', category: 'NETWORK',
    service: 'Network', priority: 'P3', status: 'IN_PROGRESS',
    reporter: 'Asha Employee', technician: 'Ravi Technician',
    createdAt: '2026-11-07T11:20:00Z' },
]

const publicUser = ({ id, name, role }) => ({ id, name, role })

export const handlers = [
  http.post('/api/auth/register', async ({ request }) => {
    const { name, email, password } = await request.json()
    if (!name || !email || !password || password.length < 8) {
      return HttpResponse.json({ message: 'Invalid input' }, { status: 400 })
    }
    if (users.some((u) => u.email === email)) {
      return HttpResponse.json({ message: 'Email already registered' }, { status: 409 })
    }
    // Role is forced to EMPLOYEE on the server side, whatever the client sends.
    const user = { id: users.length + 1, name, email, password, role: 'EMPLOYEE' }
    users.push(user)
    return HttpResponse.json({ token: 'fake-jwt-' + user.id, user: publicUser(user) }, { status: 201 })
  }),

  http.post('/api/auth/login', async ({ request }) => {
    const { email, password } = await request.json()
    const user = users.find((u) => u.email === email && u.password === password)
    if (!user) {
      return HttpResponse.json({ message: 'Invalid credentials' }, { status: 401 })
    }
    return HttpResponse.json({ token: 'fake-jwt-' + user.id, user: publicUser(user) })
  }),

  http.get('/api/incidents', () => HttpResponse.json(incidents)),

  http.post('/api/incidents', async ({ request }) => {
    const body = await request.json()
    const created = {
      id: 'INC' + (1001 + incidents.length),
      status: 'NEW',
      priority: 'P3',
      createdAt: new Date().toISOString(),
      ...body,
    }
    incidents = [created, ...incidents]
    return HttpResponse.json(created, { status: 201 })
  }),
]
