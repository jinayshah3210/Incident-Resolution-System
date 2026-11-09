export default function Card({ title, children, className = '' }) {
  return (
    <div className={`rounded-xl border border-slate-200 bg-white p-5 shadow-sm ${className}`}>
      {title && <h3 className="mb-3 text-sm font-semibold text-slate-500 uppercase tracking-wide">{title}</h3>}
      {children}
    </div>
  )
}
