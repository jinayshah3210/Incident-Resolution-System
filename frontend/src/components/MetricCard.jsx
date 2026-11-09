export default function MetricCard({ label, value, accent = 'slate' }) {
  const tones = {
    slate: { wrap: 'border-slate-200 bg-slate-50 text-slate-700', dot: 'bg-slate-400', icon: 'bg-slate-100 text-slate-600' },
    amber: { wrap: 'border-amber-200 bg-amber-50 text-amber-700', dot: 'bg-amber-500', icon: 'bg-amber-100 text-amber-700' },
    emerald: { wrap: 'border-emerald-200 bg-emerald-50 text-emerald-700', dot: 'bg-emerald-500', icon: 'bg-emerald-100 text-emerald-700' },
    red: { wrap: 'border-red-200 bg-red-50 text-red-700', dot: 'bg-red-500', icon: 'bg-red-100 text-red-700' },
    indigo: { wrap: 'border-indigo-200 bg-indigo-50 text-indigo-700', dot: 'bg-indigo-500', icon: 'bg-indigo-100 text-indigo-700' },
    sky: { wrap: 'border-sky-200 bg-sky-50 text-sky-700', dot: 'bg-sky-500', icon: 'bg-sky-100 text-sky-700' },
  }

  const current = tones[accent] || tones.slate

  return (
    <div className={`flex items-center gap-3 rounded-2xl border p-4 shadow-sm ${current.wrap}`}>
      <div className={`flex h-10 w-10 items-center justify-center rounded-xl ${current.icon}`}>
        <span className={`h-2.5 w-2.5 rounded-full ${current.dot}`} />
      </div>
      <div>
        <div className="text-sm font-medium text-slate-500">{label}</div>
        <div className="mt-1 text-3xl font-bold tracking-tight text-slate-800">{value}</div>
      </div>
    </div>
  )
}
