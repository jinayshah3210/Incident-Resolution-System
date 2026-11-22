export const initials = (s = '') => s.split(' ').filter(Boolean).map((x) => x[0]).slice(0, 2).join('').toUpperCase();
export const dateTime = (v) => (v ? new Date(v).toLocaleString('en-GB', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '—');
export const shortDate = (v) => (v ? new Date(v).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }) : '—');
export const label = (v) => String(v || '').replaceAll('_', ' ').toLowerCase().replace(/^\w/, (c) => c.toUpperCase());
export const greeting = () => { const h = new Date().getHours(); return h < 12 ? 'Good morning' : h < 17 ? 'Good afternoon' : 'Good evening'; };
export function remaining(due) {
  if (!due) return '—';
  const ms = new Date(due) - Date.now();
  const m = Math.round(Math.abs(ms) / 60000);
  const text = m >= 60 ? `${Math.floor(m / 60)}h ${m % 60}m` : `${m}m`;
  return ms >= 0 ? `${text} left` : `${text} overdue`;
}
export const AFFECTED = { 1: 'Just me', 2: 'A few people', 3: 'A team or department', 4: 'Everyone / customers' };
