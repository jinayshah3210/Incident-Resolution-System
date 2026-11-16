
import StatusBadge from './StatusBadge'
import PriorityBadge from './PriorityBadge'
import { fmtDate } from '../utils/format'

const allowedTransitions = {
  ASSIGNED: ['ACKNOWLEDGED'],
  ACKNOWLEDGED: ['IN_PROGRESS'],
  IN_PROGRESS: ['ON_HOLD', 'RESOLVED'],
  ON_HOLD: ['IN_PROGRESS'],
  REOPENED: ['IN_PROGRESS'],
}

export default function IncidentTable({
  items = [],
  emptyMessage = 'No incidents found',
  onStatusChange,
  showAssignee = false,
  onSelect,
  onAssign,
  assigningId,
}) {
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
            {onAssign && <th className="px-3 py-2 font-medium">Assignment</th>}
            {onStatusChange && <th className="px-3 py-2 font-medium text-right">Action</th>}
            {onSelect && <th className="px-3 py-2 font-medium text-right">Details</th>}
          </tr>
        </thead>

        <tbody>
          {items.map((incident) => {
            const nextStatuses = allowedTransitions[incident.status] || []

            return (
              <tr key={incident.id} className="border-b border-slate-100 align-top">
                <td className="px-3 py-3 font-semibold text-slate-700">
                  {incident.id}
                </td>

                <td className="px-3 py-3">
                  <div className="font-medium text-slate-800">{incident.title}</div>
                  <div className="text-xs text-slate-500">{incident.category}</div>
                </td>

                <td className="px-3 py-3 text-slate-600">
                  {incident.service || incident.category}
                </td>

                <td className="px-3 py-3">
                  <PriorityBadge priority={incident.priority} />
                </td>

                <td className="px-3 py-3">
                  <StatusBadge status={incident.status} />
                </td>

                {showAssignee && (
                  <td className="px-3 py-3 text-slate-600">
                    {incident.technician || incident.assignedTechnicianName || 'Unassigned'}
                  </td>
                )}

                <td className="px-3 py-3 text-slate-600">
                  {incident.reporter}
                </td>

                <td className="px-3 py-3 text-slate-600">
                  {fmtDate(incident.createdAt)}
                </td>

                {onAssign && (
                  <td className="px-3 py-3">
                    {incident.status === 'NEW' && !incident.assignedTechnicianId ? (
                      <button
                        type="button"
                        onClick={() => onAssign(incident.id)}
                        disabled={assigningId === incident.id}
                        className="rounded-lg bg-indigo-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-indigo-700 disabled:opacity-50"
                      >
                        {assigningId === incident.id ? 'Assigning...' : 'Assign'}
                      </button>
                    ) : (
                      <span className="text-xs text-slate-500">
                        {incident.technician || incident.assignedTechnicianName || 'Not available'}
                      </span>
                    )}
                  </td>
                )}

                {onStatusChange && (
                  <td className="px-3 py-3 text-right">
                    {nextStatuses.length > 0 ? (
                      <select
                        value={incident.status}
                        onChange={(e) => onStatusChange(incident.id, e.target.value)}
                        className="rounded border border-slate-300 bg-white px-2 py-1 text-xs text-slate-700 focus:border-indigo-500 focus:outline-none"
                      >
                        <option value={incident.status}>{incident.status}</option>
                        {nextStatuses.map((nextStatus) => (
                          <option key={nextStatus} value={nextStatus}>
                            {nextStatus}
                          </option>
                        ))}
                      </select>
                    ) : (
                      <span className="text-xs text-slate-400">No actions</span>
                    )}
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
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
