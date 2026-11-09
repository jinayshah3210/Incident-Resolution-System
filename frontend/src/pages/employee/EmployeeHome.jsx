import { useMemo, useState } from 'react'
import { ShieldCheck } from 'lucide-react'
import Card from '../../components/Card'
import IncidentTable from '../../components/IncidentTable'
import IncidentDetails from '../../components/IncidentDetails'
import MetricCard from '../../components/MetricCard'
import { useAuth } from '../../auth/AuthContext'
import client from '../../api/client'
import useFetch from '../../api/useFetch'

const initialForm = {
  title: '',
  category: 'APPLICATION',
  service: 'Application',
  priority: 'P3',
}

export default function EmployeeHome() {
  const { user } = useAuth()
  const { data, loading, error, reload } = useFetch('/api/incidents')
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState('ALL')
  const [form, setForm] = useState(initialForm)
  const [submitting, setSubmitting] = useState(false)
  const [message, setMessage] = useState('')
  const [selectedIncident, setSelectedIncident] = useState(null)

  const incidents = useMemo(() => {
    const rows = (data || []).filter((incident) => incident.reporter === user.name)
    return rows.filter((incident) => {
      const matchesQuery = !query || `${incident.id} ${incident.title} ${incident.service}`.toLowerCase().includes(query.toLowerCase())
      const matchesStatus = status === 'ALL' || incident.status === status
      return matchesQuery && matchesStatus
    })
  }, [data, query, status, user.name])

  const stats = useMemo(() => {
    const myIncidents = data ? data.filter((incident) => incident.reporter === user.name) : []
    return {
      total: myIncidents.length,
      open: myIncidents.filter((incident) => !['RESOLVED', 'VERIFIED', 'CLOSED'].includes(incident.status)).length,
      resolved: myIncidents.filter((incident) => ['RESOLVED', 'VERIFIED', 'CLOSED'].includes(incident.status)).length,
      critical: myIncidents.filter((incident) => incident.priority === 'P1').length,
    }
  }, [data, user.name])

  async function handleCreateIncident(e) {
    e.preventDefault()
    setMessage('')
    if (!form.title.trim() || !form.service.trim()) {
      setMessage('Title and service are required')
      return
    }

    try {
      setSubmitting(true)
      await client.post('/api/incidents', {
        ...form,
        title: form.title.trim(),
        service: form.service.trim(),
        reporter: user.name,
        technician: 'Ravi Technician',
      })
      setForm(initialForm)
      setMessage('Incident raised successfully')
      await reload()
    } catch {
      setMessage('Unable to create incident')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="space-y-6 pb-4">
      <div className="rounded-2xl border border-slate-200 bg-[#eef6ff] p-5 shadow-sm">
        <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-700 shadow-sm">
              <ShieldCheck className="h-5 w-5" />
            </div>
            <div>
              <div className="text-[10px] font-semibold uppercase tracking-[0.18em] text-indigo-600">Employee</div>
              <h2 className="text-2xl font-bold text-slate-800">{user.name}</h2>
            </div>
          </div>
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-4">
        <MetricCard label="Total" value={stats.total} accent="slate" />
        <MetricCard label="Open" value={stats.open} accent="amber" />
        <MetricCard label="Resolved" value={stats.resolved} accent="emerald" />
        <MetricCard label="Critical" value={stats.critical} accent="red" />
      </div>

      <Card title="Raise incident" className="min-h-[300px]">
        <form onSubmit={handleCreateIncident} className="grid gap-4 md:grid-cols-2">
          <div className="md:col-span-2">
            <label className="mb-2 block text-sm font-medium text-slate-700">Incident title</label>
            <input
              value={form.title}
              onChange={(e) => setForm((current) => ({ ...current, title: e.target.value }))}
              className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm text-slate-700 placeholder:text-slate-400 focus:border-indigo-300 focus:outline-none focus:ring-2 focus:ring-indigo-100"
              placeholder="Brief summary of the issue"
            />
          </div>

          <div>
            <label className="mb-2 block text-sm font-medium text-slate-700">Service</label>
            <input
              value={form.service}
              onChange={(e) => setForm((current) => ({ ...current, service: e.target.value }))}
              className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm text-slate-700 placeholder:text-slate-400 focus:border-indigo-300 focus:outline-none focus:ring-2 focus:ring-indigo-100"
              placeholder="Service affected"
            />
          </div>

          <div>
            <label className="mb-2 block text-sm font-medium text-slate-700">Priority</label>
            <select
              value={form.priority}
              onChange={(e) => setForm((current) => ({ ...current, priority: e.target.value }))}
              className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm text-slate-700 focus:border-indigo-300 focus:outline-none focus:ring-2 focus:ring-indigo-100"
            >
              <option value="P1">P1</option>
              <option value="P2">P2</option>
              <option value="P3">P3</option>
              <option value="P4">P4</option>
            </select>
          </div>

          <div className="md:col-span-2">
            <label className="mb-2 block text-sm font-medium text-slate-700">Category</label>
            <select
              value={form.category}
              onChange={(e) => setForm((current) => ({ ...current, category: e.target.value }))}
              className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm text-slate-700 focus:border-indigo-300 focus:outline-none focus:ring-2 focus:ring-indigo-100"
            >
              <option value="APPLICATION">Application</option>
              <option value="NETWORK">Network</option>
              <option value="ENDPOINT">Endpoint</option>
              <option value="ACCESS">Access</option>
              <option value="INFRASTRUCTURE">Infrastructure</option>
            </select>
          </div>

          <div className="md:col-span-2 flex items-center justify-between gap-3">
            {message && <p className="text-sm text-slate-600">{message}</p>}
            <button type="submit" disabled={submitting} className="ml-auto rounded-xl bg-gradient-to-r from-indigo-600 to-indigo-500 px-5 py-2.5 text-sm font-semibold text-white shadow-[0_8px_18px_rgba(79,70,229,0.25)] transition hover:brightness-105 disabled:opacity-70">
              {submitting ? 'Submitting...' : 'Submit incident'}
            </button>
          </div>
        </form>
      </Card>

      <Card title="My incidents" className="overflow-hidden">
        <div className="mb-4 flex flex-col gap-3 md:flex-row md:items-center">
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search by ID, keyword, or service..."
            className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm text-slate-700 placeholder:text-slate-400 focus:border-indigo-300 focus:outline-none focus:ring-2 focus:ring-indigo-100 md:max-w-sm"
          />
          <select value={status} onChange={(e) => setStatus(e.target.value)} className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm text-slate-700 focus:border-indigo-300 focus:outline-none focus:ring-2 focus:ring-indigo-100">
            <option value="ALL">All statuses</option>
            <option value="NEW">New</option>
            <option value="CLASSIFIED">Classified</option>
            <option value="ASSIGNED">Assigned</option>
            <option value="ACKNOWLEDGED">Acknowledged</option>
            <option value="IN_PROGRESS">In progress</option>
            <option value="RESOLVED">Resolved</option>
            <option value="VERIFIED">Verified</option>
            <option value="CLOSED">Closed</option>
          </select>
        </div>

        {loading && <p className="text-sm text-slate-500">Loading incidents...</p>}
        {error && <p className="text-sm text-red-600">Unable to load incidents.</p>}
        {!loading && !error && (
          <IncidentTable items={incidents} emptyMessage="You have no incidents yet." onSelect={setSelectedIncident} />
        )}
      </Card>

      <IncidentDetails incident={selectedIncident} onClose={() => setSelectedIncident(null)} />
    </div>
  )
}
