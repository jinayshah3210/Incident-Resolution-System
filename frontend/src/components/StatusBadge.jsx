const styles = {
  NEW: 'bg-slate-100 text-slate-700',
  CLASSIFIED: 'bg-sky-100 text-sky-700',
  ASSIGNED: 'bg-indigo-100 text-indigo-700',
  ACKNOWLEDGED: 'bg-violet-100 text-violet-700',
  IN_PROGRESS: 'bg-amber-100 text-amber-700',
  RESOLVED: 'bg-emerald-100 text-emerald-700',
  VERIFIED: 'bg-teal-100 text-teal-700',
  CLOSED: 'bg-slate-200 text-slate-600',
}

export default function StatusBadge({ status }) {
  return (
    <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${styles[status] || styles.NEW}`}>
      {status.replace('_', ' ')}
    </span>
  )
}
