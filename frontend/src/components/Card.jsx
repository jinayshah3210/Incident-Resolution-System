export default function Card({ title, children, className = '' }) {
  return (
    <div className={`rounded-2xl border border-slate-200 bg-white p-5 shadow-sm ${className}`}>
      {title && <h3 className="mb-4 text-sm font-semibold uppercase tracking-[0.12em] text-slate-500">{title}</h3>}
      {children}
    </div>
  )
}
