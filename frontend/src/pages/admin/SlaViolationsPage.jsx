import React, { useState } from 'react';
import { X } from 'lucide-react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { API } from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import { useData } from '../../hooks/useData';
import { dateTime } from '../../utils/format';
import { EmptyState, ErrorBanner, LoadingState, PageShell, PageTitle, PriorityChip, StatusChip } from '../../components/ui';
import ChatThread from '../../components/ChatThread';

/** Open SLA violations grouped by incident; dismissing hides the alert but keeps the record. */
export default function SlaViolationsPage() {
  const { user } = useAuth();
  const q = useData('/admin/sla-violations', { refetchInterval: 10000 });
  const client = useQueryClient();
  const [open, setOpen] = useState(null);
  const [draft, setDraft] = useState('');
  const dismiss = useMutation({ mutationFn: (id) => API.post(`/admin/sla-violations/${id}/dismiss`), onSuccess: () => { toast.success('Alert dismissed; record kept'); client.invalidateQueries(); } });
  return <PageShell>
    <PageTitle eyebrow="ADMIN · SERVICE LEVELS" title="SLA violations" description="Incidents that missed a response or resolution target." />
    {q.isLoading ? <LoadingState /> : q.isError ? <ErrorBanner /> : !q.data?.length ? <EmptyState title="No open SLA violations" text="Breaches appear here as soon as the monitor detects them." />
      : <div className="incident-list">{q.data.map(({ incident: i, violations }) => <article key={i.publicId} className={`incident-row ${open === i.publicId ? 'is-open' : ''}`}>
        <div className="incident-summary violation">
          <button className="link-button" onClick={() => setOpen(open === i.publicId ? null : i.publicId)}><b>{i.publicId}</b> {i.title}</button>
          <PriorityChip priority={i.priority} /><StatusChip status={i.status} />
          <span>{violations.map((v) => `${v.type.toLowerCase()} +${v.delayMinutes} min`).join(', ')}</span>
          <button className="notify-remove" aria-label="Dismiss alert" onClick={() => window.confirm(`Dismiss the SLA alert for ${i.publicId}? The record is kept.`) && dismiss.mutate(i.publicId)}><X size={16} /></button>
        </div>
        {open === i.publicId && <div className="incident-details two-col"><div>
          <p><b>Technician:</b> {i.technicianName || 'Unassigned'}</p><p><b>Reporter:</b> {i.reporterName}</p>
          {violations.map((v) => <p key={v.id}><b>{v.type} deadline</b> {dateTime(v.dueAt)} · detected {dateTime(v.detectedAt)} · delay {v.delayMinutes} min</p>)}
          <p>{i.description}</p></div>
          <ChatThread incident={i} user={user} text={draft} setText={setDraft} /></div>}
      </article>)}</div>}
  </PageShell>;
}
