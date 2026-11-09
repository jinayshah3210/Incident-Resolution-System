import StatusBadge from './StatusBadge'
import PriorityBadge from './PriorityBadge'
import { fmtDate } from '../utils/format'

export default function IncidentTable({ items = [], emptyMessage = 'No incidents found', onStatusChange, showAssignee = false, onSelect }) {
  if (!items.length) {
    return (
      <div className="rounded-lg border border-dashed border-slate-200 bg-slate-50 p-8 text-center text-sm text-slate-500">
        {emptyMessage}
      </div>
    )
  }

  return (
    <div className="overflow-x-auto">
      <table className="min-w-full text-left text-sm">
        <thead>
          <tr className="border-b border-slate-200 text-slate-500">
            <th className="px-3 py-2 font-medium">ID</th>
            <th className="px-3 py-2 font-medium">Incident</th>
            <th className="px-3 py-2 font-medium">Service</th>
            <th className="px-3 py-2 font-medium">Priority</th>
            <th className="px-3 py-2 font-medium">Status</th>
            {showAssignee && <th className="px-3 py-2 font-medium">Technician</th>}
            <th className="px-3 py-2 font-medium">Reported by</th>
            <th className="px-3 py-2 font-medium">Created</th>
            {onStatusChange && <th className="px-3 py-2 font-medium text-right">Action</th>}
            {onSelect && <th className="px-3 py-2 font-medium text-right">Details</th>}
          </tr>
        </thead>
        <tbody>
          {items.map((incident) => (
            <tr key={incident.id} className="border-b border-slate-100 align-top">
              <td className="px-3 py-3 font-semibold text-slate-700">{incident.id}</td>
              <td className="px-3 py-3">
                <div className="font-medium text-slate-800">{incident.title}</div>
                <div className="text-xs text-slate-500">{incident.category}</div>
              </td>
              <td className="px-3 py-3 text-slate-600">{incident.service}</td>
              <td className="px-3 py-3"><PriorityBadge priority={incident.priority} /></td>
              <td className="px-3 py-3"><StatusBadge status={incident.status} /></td>
              {showAssignee && (
                <td className="px-3 py-3 text-slate-600">{incident.technician || 'Unassigned'}</td>
              )}
              <td className="px-3 py-3 text-slate-600">{incident.reporter}</td>
              <td className="px-3 py-3 text-slate-600">{fmtDate(incident.createdAt)}</td>
              {onStatusChange && (
                <td className="px-3 py-3 text-right">
                  <select
                    value={incident.status}
                    onChange={(e) => onStatusChange(incident.id, e.target.value)}
                    className="rounded border border-slate-300 bg-white px-2 py-1 text-xs text-slate-700 focus:border-indigo-500 focus:outline-none"
                  >
                    <option value="NEW">NEW</option>
                    <option value="CLASSIFIED">CLASSIFIED</option>
                    <option value="ASSIGNED">ASSIGNED</option>
                    <option value="ACKNOWLEDGED">ACKNOWLEDGED</option>
                    <option value="IN_PROGRESS">IN_PROGRESS</option>
                    <option value="RESOLVED">RESOLVED</option>
                    <option value="VERIFIED">VERIFIED</option>
                    <option value="CLOSED">CLOSED</option>
                  </select>
                </td>
              )}
              {onSelect && (
                <td className="px-3 py-3 text-right">
                  <button
                    type="button"
                    onClick={() => onSelect(incident)}
                    className="rounded border border-slate-300 px-2 py-1 text-xs text-slate-700"
                  >
                    View
                  </button>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
