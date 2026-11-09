export const STATUSES = ['NEW', 'CLASSIFIED', 'ASSIGNED', 'ACKNOWLEDGED', 'IN_PROGRESS', 'RESOLVED', 'VERIFIED', 'CLOSED']
export const PRIORITIES = ['P1', 'P2', 'P3', 'P4']
export const SLA_STATES = ['ON_TRACK', 'AT_RISK', 'VIOLATED', 'MET']
export const EMPTY_FILTERS = { q: '', status: '', priority: '', category: '', service: '', technician: '', slaStatus: '', from: '', to: '' }

export const fmtDate = (iso) =>
  iso ? new Date(iso).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : '-'

export function timeLeft(iso) {
  const ms = Date.parse(iso) - Date.now()
  const m = Math.floor(Math.abs(ms) / 60000)
  const h = Math.floor(m / 60)
  const d = Math.floor(h / 24)
  const txt = d >= 1 ? `${d}d ${h % 24}h` : h >= 1 ? `${h}h ${m % 60}m` : `${m}m`
  return ms >= 0 ? `${txt} left` : `${txt} overdue`
}

export const errMsg = (e) => e?.response?.data?.message || e?.message || 'Something went wrong'
