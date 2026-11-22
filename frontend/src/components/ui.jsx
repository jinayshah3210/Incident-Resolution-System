import React from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, ClipboardList, Command, Loader2, WifiOff } from 'lucide-react';
import { roles } from '../utils/roles';
import { useAuth } from '../context/AuthContext';
import { label } from '../utils/format';

export function Brand() {
  return <div className="brand"><div className="brand-mark"><Command size={18} /></div><span>Resolve<span>IT</span></span></div>;
}

/** Every page opened from a dashboard: a back link that always returns to the role's dashboard. */
export function PageShell({ children }) {
  const { user } = useAuth();
  return <div className="page-content">
    <Link className="back-dashboard" to={roles[user?.role]?.path || '/'}><ArrowLeft size={16} /> Back to dashboard</Link>
    {children}
  </div>;
}

export function PageTitle({ eyebrow, title, description, action }) {
  return <div className="page-title-row"><div><span className="eyebrow muted">{eyebrow}</span><h1>{title}</h1>{description && <p>{description}</p>}</div>{action}</div>;
}

export function StatCard({ icon: Icon, label: text, value, change, tone }) {
  return <div className="stat-card"><div className="stat-head"><span className={`stat-icon ${tone}`}><Icon size={18} /></span></div>
    <span className="stat-label">{text}</span><div className="stat-value">{value}</div>{change && <span className="stat-caption">{change}</span>}</div>;
}

export function ErrorBanner({ message }) {
  return <div className="error-banner" role="alert"><WifiOff size={17} /><span>{message || 'This information could not be loaded. Check the connection and try again.'}</span></div>;
}

export function LoadingState({ text = 'Loading…' }) {
  return <div className="loading-panel" aria-busy="true"><Loader2 size={18} className="spin" /> {text}</div>;
}

export function EmptyState({ title, text, action, icon: Icon = ClipboardList }) {
  return <div className="empty-state"><div><Icon size={24} /></div><h3>{title}</h3>{text && <p>{text}</p>}{action}</div>;
}

/** Loading, error and empty handling for any list query. */
export function QueryState({ query, empty, emptyTitle, emptyText, children }) {
  if (query.isLoading) return <LoadingState />;
  if (query.isError) return <ErrorBanner />;
  if (empty) return <EmptyState title={emptyTitle} text={emptyText} />;
  return children;
}

export const StatusChip = ({ status }) => <span className={`status-chip ${String(status).toLowerCase()}`}>{label(status)}</span>;
export const PriorityChip = ({ priority }) => <span className={`priority ${String(priority || 'p3').toLowerCase()}`}>{priority || '—'}</span>;
export function SlaBadge({ status }) {
  if (!status) return null;
  const text = { OK: 'SLA on track', AT_RISK: 'SLA at risk', VIOLATED: 'SLA violated' }[status] || status;
  return <span className={`sla-badge sla-${status.toLowerCase()}`}>{text}</span>;
}

export function Detail({ label: name, value }) { return <div><small>{name}</small><p>{value || '—'}</p></div>; }
