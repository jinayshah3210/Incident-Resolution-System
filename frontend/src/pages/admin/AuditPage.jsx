import React, { useState } from 'react';
import { useData } from '../../hooks/useData';
import { dateTime } from '../../utils/format';
import { EmptyState, ErrorBanner, LoadingState, PageShell, PageTitle } from '../../components/ui';

/** Append-only audit log, newest first, filterable by incident, actor and action. */
export default function AuditPage() {
  const [offset, setOffset] = useState(0);
  const [f, setF] = useState({ incident: '', actor: '', action: '' });
  const params = new URLSearchParams({ offset, limit: 100, ...Object.fromEntries(Object.entries(f).filter(([, v]) => v)) });
  const q = useData(`/admin/audit?${params}`, { placeholderData: (p) => p });
  const set = (k, v) => { setOffset(0); setF({ ...f, [k]: v }); };
  return <PageShell>
    <PageTitle eyebrow="ADMIN · GOVERNANCE" title="Audit log" description="Every incident and administrative action, newest first. Entries cannot be edited or deleted." />
    <div className="filter-bar">
      <input className="filter-text" placeholder="Incident ID or SYSTEM" value={f.incident} onChange={(e) => set('incident', e.target.value)} />
      <input className="filter-text" placeholder="User ID" value={f.actor} onChange={(e) => set('actor', e.target.value)} />
      <input className="filter-text" placeholder="Action" value={f.action} onChange={(e) => set('action', e.target.value)} />
    </div>
    <div className="panel">{q.isLoading ? <LoadingState /> : q.isError ? <ErrorBanner /> : !q.data?.length ? <EmptyState title="No matching activity" />
      : <table className="data-table"><thead><tr><th>Time</th><th>Incident</th><th>User</th><th>Action</th><th>Detail</th><th>Status change</th></tr></thead>
        <tbody>{q.data.map((a) => <tr key={a.id}><td>{dateTime(a.at)}</td><td>{a.incidentId}</td><td>{a.actor}</td><td>{a.action.replaceAll('_', ' ').toLowerCase()}</td>
          <td>{a.detail}</td><td>{a.previousStatus ? `${a.previousStatus} → ${a.newStatus}` : ''}</td></tr>)}</tbody></table>}
      <div className="pager"><button className="btn ghost" disabled={offset === 0} onClick={() => setOffset(Math.max(0, offset - 100))}>Previous</button>
        <span>Entries {offset + 1}–{offset + (q.data || []).length}</span>
        <button className="btn ghost" disabled={(q.data || []).length < 100} onClick={() => setOffset(offset + 100)}>Next</button></div>
    </div>
  </PageShell>;
}
