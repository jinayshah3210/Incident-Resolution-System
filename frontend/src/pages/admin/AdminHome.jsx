import { useMemo, useState } from 'react'
import {
  BarChart,
  Bar,
  CartesianGrid,
  Tooltip,
  XAxis,
  YAxis,
  ResponsiveContainer,
} from 'recharts'
import Card from '../../components/Card'
import IncidentTable from '../../components/IncidentTable'
import IncidentDetails from '../../components/IncidentDetails'
import MetricCard from '../../components/MetricCard'
import client from '../../api/client'
import useFetch from '../../api/useFetch'

export default function AdminHome() {
  const { data, loading, error, reload } = useFetch('/api/incidents')
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState('ALL')
  const [priority, setPriority] = useState('ALL')
  const [selectedIncident, setSelectedIncident] = useState(null)
  const [updatingPriority, setUpdatingPriority] = useState(false)

  const filtered = useMemo(() => {
    return (data || []).filter((incident) => {
      const matchesQuery = !query || `${incident.id} ${incident.title} ${incident.service} ${incident.reporter} ${incident.technician}`.toLowerCase().includes(query.toLowerCase())
      const matchesStatus = status === 'ALL' || incident.status === status
      const matchesPriority = priority === 'ALL' || incident.priority === priority
      return matchesQuery && matchesStatus && matchesPriority
    })
  }, [data, priority, query, status])

  const counts = useMemo(() => {
    const incidents = data || []
    return {
      open: incidents.filter((incident) => !['RESOLVED', 'VERIFIED', 'CLOSED'].includes(incident.status)).length,
      inProgress: incidents.filter((incident) => incident.status === 'IN_PROGRESS').length,
      resolved: incidents.filter((incident) => incident.status === 'RESOLVED').length,
      p1: incidents.filter((incident) => incident.priority === 'P1').length,
      total: incidents.length,
      technicianLoad: incidents.reduce((acc, incident) => {
        const tech = incident.technician || 'Unassigned'
        acc[tech] = (acc[tech] || 0) + 1
        return acc
      }, {}),
    }
  }, [data])

  const chartData = [
    { name: 'NEW', count: (data || []).filter((incident) => incident.status === 'NEW').length },
    { name: 'ASSIGNED', count: (data || []).filter((incident) => incident.status === 'ASSIGNED').length },
    { name: 'IN_PROGRESS', count: (data || []).filter((incident) => incident.status === 'IN_PROGRESS').length },
    { name: 'RESOLVED', count: (data || []).filter((incident) => incident.status === 'RESOLVED').length },
    { name: 'CLOSED', count: (data || []).filter((incident) => incident.status === 'CLOSED').length },
  ]

  const topTechnicians = Object.entries(counts.technicianLoad)
    .map(([name, count]) => ({ name, count }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 3)

  async function handlePriorityChange(id, priority) {
    try {
      setUpdatingPriority(true)
      await client.patch(`/api/incidents/${id}/priority`, { priority })
      setSelectedIncident((current) => (current && current.id === id ? { ...current, priority } : current))
      await reload()
    } catch {
      alert('Unable to update incident priority')
    } finally {
      setUpdatingPriority(false)
    }
  }

  return (
    <div className="space-y-6 pb-4">
      <div className="grid gap-4 md:grid-cols-5">
        <MetricCard label="Total" value={counts.total} accent="slate" />
        <MetricCard label="Open" value={counts.open} accent="amber" />
        <MetricCard label="In progress" value={counts.inProgress} accent="sky" />
        <MetricCard label="Resolved" value={counts.resolved} accent="emerald" />
        <MetricCard label="Critical P1" value={counts.p1} accent="red" />
      </div>

      <div className="grid gap-6 lg:grid-cols-[1.5fr_1fr]">
        <Card title="Incident status overview">
          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                <XAxis dataKey="name" tickLine={false} axisLine={false} />
                <YAxis allowDecimals={false} tickLine={false} axisLine={false} />
                <Tooltip />
                <Bar dataKey="count" fill="#4f46e5" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>

        <Card title="Technician load">
          <div className="space-y-4">
            {topTechnicians.map((tech) => (
              <div key={tech.name}>
                <div className="mb-1 flex items-center justify-between text-sm text-slate-600">
                  <span>{tech.name}</span>
                  <span className="font-medium text-slate-800">{tech.count}</span>
                </div>
                <div className="h-2 rounded-full bg-slate-200">
                  <div
                    className="h-2 rounded-full bg-indigo-500"
                    style={{ width: `${Math.min((tech.count / Math.max(...topTechnicians.map((item) => item.count), 1)) * 100, 100)}%` }}
                  />
                </div>
              </div>
            ))}
            {!topTechnicians.length && <p className="text-sm text-slate-500">No technician assignments yet.</p>}
          </div>
        </Card>
      </div>

      <Card title="All incidents">
        <div className="mb-4 flex flex-col gap-3 lg:flex-row lg:items-center">
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search incidents"
            className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-200 lg:max-w-md"
          />
          <select
            value={status}
            onChange={(e) => setStatus(e.target.value)}
            className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-200"
          >
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
          <select
            value={priority}
            onChange={(e) => setPriority(e.target.value)}
            className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-200"
          >
            <option value="ALL">All priorities</option>
            <option value="P1">P1</option>
            <option value="P2">P2</option>
            <option value="P3">P3</option>
            <option value="P4">P4</option>
          </select>
        </div>

        {loading && <p className="text-sm text-slate-500">Loading dashboard...</p>}
        {error && <p className="text-sm text-red-600">Unable to load incident dashboard.</p>}
        {!loading && !error && (
          <IncidentTable
            items={filtered}
            emptyMessage="No incidents match your filters."
            onSelect={setSelectedIncident}
            showAssignee
          />
        )}
      </Card>

      <IncidentDetails
        incident={selectedIncident}
        onClose={() => setSelectedIncident(null)}
        onPriorityChange={handlePriorityChange}
        priorityUpdating={updatingPriority}
      />
    </div>
  )
}
