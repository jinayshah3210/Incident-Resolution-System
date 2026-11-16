
const allowedTransitions = {
  ASSIGNED: ['ACKNOWLEDGED'],
  ACKNOWLEDGED: ['IN_PROGRESS'],
  IN_PROGRESS: ['ON_HOLD', 'RESOLVED'],
  ON_HOLD: ['IN_PROGRESS'],
  REOPENED: ['IN_PROGRESS'],
}

export default function IncidentDetails({
  incident,
  onClose,
  onStatusChange,
  onPriorityChange,
  priorityUpdating = false,
}) {
  if (!incident) return null

  const nextStatuses = allowedTransitions[incident.status] || []

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
      <div className="w-full max-w-2xl rounded-lg bg-white p-5 shadow-lg">
        <div className="mb-4 flex items-center justify-between gap-3">
          <div>
            <div className="text-xs uppercase tracking-wide text-slate-500">
              Incident
            </div>
            <h3 className="text-xl font-semibold text-slate-800">
              {incident.id}
            </h3>
          </div>

          <button
            onClick={onClose}
            className="rounded border border-slate-300 px-3 py-1 text-sm"
          >
            Close
          </button>
        </div>

        <div className="grid gap-4 md:grid-cols-2">
          <div>
            <div className="text-xs text-slate-500">Title</div>
            <div className="font-medium text-slate-800">{incident.title}</div>
          </div>

          <div>
            <div className="text-xs text-slate-500">Status</div>
            {onStatusChange && nextStatuses.length > 0 ? (
              <select
                value={incident.status}
                onChange={(e) => onStatusChange(incident.id, e.target.value)}
                className="mt-1 w-full rounded border border-slate-300 px-2 py-1 text-sm"
              >
                <option value={incident.status}>{incident.status}</option>
                {nextStatuses.map((nextStatus) => (
                  <option key={nextStatus} value={nextStatus}>
                    {nextStatus}
                  </option>
                ))}
              </select>
            ) : (
              <div className="mt-1 text-slate-800">{incident.status}</div>
            )}
          </div>

          <div>
            <div className="text-xs text-slate-500">Category</div>
            <div className="text-slate-800">{incident.category}</div>
          </div>

          <div>
            <div className="text-xs text-slate-500">Priority</div>
            {onPriorityChange ? (
              <select
                value={incident.priority}
                onChange={(e) => onPriorityChange(incident.id, e.target.value)}
                disabled={priorityUpdating}
                className="mt-1 w-full rounded border border-slate-300 px-2 py-1 text-sm"
              >
                <option value="P1">P1</option>
                <option value="P2">P2</option>
                <option value="P3">P3</option>
                <option value="P4">P4</option>
              </select>
            ) : (
              <div className="mt-1 text-slate-800">{incident.priority}</div>
            )}

            {onPriorityChange && priorityUpdating && (
              <div className="mt-1 text-xs text-slate-500" role="status">
                Updating priority...
              </div>
            )}
          </div>

          <div>
            <div className="text-xs text-slate-500">Service</div>
            <div className="text-slate-800">
              {incident.service || incident.category}
            </div>
          </div>

          <div>
            <div className="text-xs text-slate-500">Technician</div>
            <div className="text-slate-800">
              {incident.technician || incident.assignedTechnicianName || 'Unassigned'}
            </div>
          </div>

          <div>
            <div className="text-xs text-slate-500">Reporter</div>
            <div className="text-slate-800">
              {incident.reporter || 'Not available'}
            </div>
          </div>

          <div>
            <div className="text-xs text-slate-500">Created</div>
            <div className="text-slate-800">
              {incident.createdAt
                ? new Date(incident.createdAt).toLocaleString()
                : 'Not available'}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
