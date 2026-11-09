export default function IncidentDetails({ incident, onClose, onStatusChange }) {
  if (!incident) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
      <div className="w-full max-w-2xl rounded-lg bg-white p-5 shadow-lg">
        <div className="mb-4 flex items-center justify-between gap-3">
          <div>
            <div className="text-xs uppercase tracking-wide text-slate-500">Incident</div>
            <h3 className="text-xl font-semibold text-slate-800">{incident.id}</h3>
          </div>
          <button onClick={onClose} className="rounded border border-slate-300 px-3 py-1 text-sm">
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
            {onStatusChange ? (
              <select
                value={incident.status}
                onChange={(e) => onStatusChange(incident.id, e.target.value)}
                className="mt-1 w-full rounded border border-slate-300 px-2 py-1 text-sm"
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
            <div className="text-slate-800">{incident.priority}</div>
          </div>

          <div>
            <div className="text-xs text-slate-500">Service</div>
            <div className="text-slate-800">{incident.service}</div>
          </div>
          <div>
            <div className="text-xs text-slate-500">Technician</div>
            <div className="text-slate-800">{incident.technician || 'Unassigned'}</div>
          </div>

          <div>
            <div className="text-xs text-slate-500">Reporter</div>
            <div className="text-slate-800">{incident.reporter}</div>
          </div>
          <div>
            <div className="text-xs text-slate-500">Created</div>
            <div className="text-slate-800">{new Date(incident.createdAt).toLocaleString()}</div>
          </div>
        </div>
      </div>
    </div>
  )
}
