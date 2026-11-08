import { http, HttpResponse } from 'msw'

const users = {
  'emp@test.com':   { id: 1, name: 'Asha Employee',   role: 'EMPLOYEE' },
  'tech@test.com':  { id: 2, name: 'Ravi Technician', role: 'TECHNICIAN' },
  'admin@test.com': { id: 3, name: 'Meera Admin',     role: 'ADMIN' },
}

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

export const handlers = [
  http.post('/api/auth/login', async ({ request }) => {
    const { email } = await request.json()
    const user = users[email]
    if (!user) {
      return HttpResponse.json({ message: 'Invalid credentials' }, { status: 401 })
    }
    return HttpResponse.json({ token: 'fake-jwt-' + user.role, user })
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
