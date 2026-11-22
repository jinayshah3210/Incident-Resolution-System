import React from 'react';
import { Search, X } from 'lucide-react';
import { useData } from '../hooks/useData';

const STATUSES = ['NEW', 'CLASSIFIED', 'ASSIGNED', 'ACKNOWLEDGED', 'IN_PROGRESS', 'RESOLVED', 'VERIFIED', 'CLOSED'];

/** Reusable filters for GET /api/incidents/search. Which filters appear depends on the role. */
export default function SearchFilterBar({ role, filters, setFilters, count }) {
  const services = useData('/catalog/services', { enabled: role !== 'EMPLOYEE' });
  const categories = useData('/catalog/categories', { enabled: role !== 'EMPLOYEE' });
  const set = (k, v) => setFilters({ ...filters, [k]: v });
  const staff = role !== 'EMPLOYEE';
  const active = Object.values(filters).some(Boolean);
  return <div className="filter-bar">
    <div className="search-box"><Search size={17} /><input placeholder="Search by ID, title or description" value={filters.q || ''} onChange={(e) => set('q', e.target.value)} aria-label="Search incidents" /></div>
    <select value={filters.status || ''} onChange={(e) => set('status', e.target.value)} aria-label="Status"><option value="">All statuses</option>{STATUSES.map((s) => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}</select>
    {staff && <select value={filters.priority || ''} onChange={(e) => set('priority', e.target.value)} aria-label="Priority"><option value="">All priorities</option>{['P1', 'P2', 'P3', 'P4'].map((p) => <option key={p}>{p}</option>)}</select>}
    {staff && <select value={filters.category || ''} onChange={(e) => set('category', e.target.value)} aria-label="Category"><option value="">All categories</option>{(categories.data || []).map((c) => <option key={c}>{c}</option>)}</select>}
    {staff && <select value={filters.service || ''} onChange={(e) => set('service', e.target.value)} aria-label="Service"><option value="">All services</option>{(services.data || []).map((s) => <option key={s.id}>{s.name}</option>)}</select>}
    {staff && <select value={filters.slaStatus || ''} onChange={(e) => set('slaStatus', e.target.value)} aria-label="SLA status"><option value="">Any SLA state</option><option value="ok">On track</option><option value="at-risk">At risk</option><option value="violated">Violated</option></select>}
    {role === 'ADMIN' && <input className="filter-text" placeholder="Technician" value={filters.technician || ''} onChange={(e) => set('technician', e.target.value)} aria-label="Technician" />}
    <label className="filter-date">From<input type="date" value={filters.from || ''} onChange={(e) => set('from', e.target.value)} /></label>
    <label className="filter-date">To<input type="date" value={filters.to || ''} onChange={(e) => set('to', e.target.value)} /></label>
    {active && <button className="btn ghost small" onClick={() => setFilters({})}><X size={14} /> Clear</button>}
    <span className="results-count">{count} result{count === 1 ? '' : 's'}</span>
  </div>;
}

/** Converts the filter state into search query parameters (dates to ISO instants). */
export function toSearchParams(filters, scope) {
  const p = new URLSearchParams();
  Object.entries(filters).forEach(([k, v]) => {
    if (!v) return;
    if (k === 'from') p.set('from', new Date(`${v}T00:00:00`).toISOString());
    else if (k === 'to') p.set('to', new Date(`${v}T23:59:59`).toISOString());
    else p.set(k, v);
  });
  if (scope) p.set('scope', scope);
  return p.toString();
}
