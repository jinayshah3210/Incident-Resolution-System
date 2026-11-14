import { http, HttpResponse } from 'msw'

let users = [
  { id: 1, name: 'Asha Employee', email: 'emp@test.com', password: 'Emp@1234', role: 'EMPLOYEE' },
  { id: 2, name: 'Ravi Technician', email: 'tech@test.com', password: 'Tech@1234', role: 'TECHNICIAN' },
  { id: 3, name: 'Meera Admin', email: 'admin@test.com', password: 'Admin@1234', role: 'ADMIN' },
]

let incidents = [
  {
    id: 'INC1001',
    title: 'Payment app unavailable',
    category: 'APPLICATION',
    service: 'Payment',
    priority: 'P1',
    status: 'ASSIGNED',
    reporter: 'Asha Employee',
    technician: 'Ravi Technician',
    createdAt: '2026-11-07T10:05:00Z',
  },
  {
    id: 'INC1002',
    title: 'VPN keeps disconnecting',
    category: 'NETWORK',
    service: 'Network',
    priority: 'P3',
    status: 'IN_PROGRESS',
    reporter: 'Asha Employee',
    technician: 'Ravi Technician',
    createdAt: '2026-11-07T11:20:00Z',
  },
  {
    id: 'INC1003',
    title: 'Laptop unable to connect to Wi-Fi',
    category: 'ENDPOINT',
    service: 'Endpoint Support',
    priority: 'P2',
    status: 'ACKNOWLEDGED',
    reporter: 'Asha Employee',
    technician: 'Ravi Technician',
    createdAt: '2026-11-08T09:30:00Z',
  },
  {
    id: 'INC1004',
    title: 'Access request approval delayed',
    category: 'ACCESS',
    service: 'IAM',
    priority: 'P4',
    status: 'NEW',
    reporter: 'Asha Employee',
    technician: 'Ravi Technician',
    createdAt: '2026-11-08T14:00:00Z',
  },
  {
    id: 'INC1005',
    title: 'CRM dashboard loads slowly',
    category: 'APPLICATION',
    service: 'CRM',
    priority: 'P2',
    status: 'RESOLVED',
    reporter: 'Asha Employee',
    technician: 'Ravi Technician',
    createdAt: '2026-11-06T16:10:00Z',
  },
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
      id: 'INC' + (1000 + incidents.length + 1),
      status: 'NEW',
      reporter: 'Asha Employee',
      technician: 'Ravi Technician',
      createdAt: new Date().toISOString(),
      ...body,
      priority: 'P3',
    }
    incidents = [created, ...incidents]
    return HttpResponse.json(created, { status: 201 })
  }),

  http.patch('/api/incidents/:id/status', async ({ params, request }) => {
    const { id } = params
    const { status } = await request.json()
    const match = incidents.find((incident) => incident.id === id)

    if (!match) {
      return HttpResponse.json({ message: 'Incident not found' }, { status: 404 })
    }

    match.status = status
    return HttpResponse.json(match)
  }),

  http.patch('/api/incidents/:id/priority', async ({ params, request }) => {
    const { id } = params
    const { priority } = await request.json()
    const match = incidents.find((incident) => incident.id === id)

    if (!match) {
      return HttpResponse.json({ message: 'Incident not found' }, { status: 404 })
    }

    match.priority = priority
    return HttpResponse.json(match)
  }),
]
