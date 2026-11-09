import { useMemo, useState } from 'react'
import Card from '../../components/Card'
import IncidentTable from '../../components/IncidentTable'
import IncidentDetails from '../../components/IncidentDetails'
import MetricCard from '../../components/MetricCard'
import { useAuth } from '../../auth/AuthContext'
import client from '../../api/client'
import useFetch from '../../api/useFetch'

export default function TechnicianHome() {
  const { user } = useAuth()
  const { data, loading, error, reload } = useFetch('/api/incidents')
  const [updating, setUpdating] = useState(false)
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState('ALL')
  const [priority, setPriority] = useState('ALL')
  const [selectedIncident, setSelectedIncident] = useState(null)

  const incidents = useMemo(() => {
    return (data || [])
      .filter((incident) => incident.technician === user.name)
      .filter((incident) => {
        const matchesQuery = !query || `${incident.id} ${incident.title} ${incident.service}`.toLowerCase().includes(query.toLowerCase())
        const matchesStatus = status === 'ALL' || incident.status === status
        const matchesPriority = priority === 'ALL' || incident.priority === priority
        return matchesQuery && matchesStatus && matchesPriority
      })
  }, [data, priority, query, status, user.name])

  const stats = useMemo(() => {
    const myIncidents = data ? data.filter((incident) => incident.technician === user.name) : []
    return {
      assigned: myIncidents.length,
      open: myIncidents.filter((incident) => !['RESOLVED', 'VERIFIED', 'CLOSED'].includes(incident.status)).length,
      critical: myIncidents.filter((incident) => incident.priority === 'P1').length,
      inProgress: myIncidents.filter((incident) => incident.status === 'IN_PROGRESS').length,
    }
  }, [data, user.name])

  async function handleStatusChange(id, status) {
    try {
      setUpdating(true)
      await client.patch(`/api/incidents/${id}/status`, { status })
      setSelectedIncident((current) => (current && current.id === id ? { ...current, status } : current))
      await reload()
    } catch {
      alert('Unable to update incident status')
    } finally {
      setUpdating(false)
    }
  }

  return (
    <div className="space-y-6 pb-4">
      <div className="grid gap-4 md:grid-cols-4">
        <MetricCard label="Assigned" value={stats.assigned} accent="indigo" />
        <MetricCard label="Open" value={stats.open} accent="amber" />
        <MetricCard label="Critical" value={stats.critical} accent="red" />
        <MetricCard label="In progress" value={stats.inProgress} accent="sky" />
      </div>

      <Card title="Assigned incidents">
        <div className="mb-4 flex flex-col gap-3 lg:flex-row lg:items-center">
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search by ID, title or service"
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

        {updating && <p className="mb-3 text-xs text-indigo-600">Updating status...</p>}
        {loading && <p className="text-sm text-slate-500">Loading assignments...</p>}
        {error && <p className="text-sm text-red-600">Unable to retrieve assigned incidents.</p>}
        {!loading && !error && (
          <IncidentTable
            items={incidents}
            emptyMessage="No incidents assigned to you."
            showAssignee={false}
            onStatusChange={handleStatusChange}
            onSelect={setSelectedIncident}
          />
        )}
      </Card>

      <IncidentDetails
        incident={selectedIncident}
        onClose={() => setSelectedIncident(null)}
        onStatusChange={handleStatusChange}
      />
    </div>
  )
}
