import React, { useState } from 'react';
import { Activity, AlertTriangle, ArrowRight, Check, ChevronDown, Lightbulb, Link2, MessageSquare, Star } from 'lucide-react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { API } from '../api/client';
import { useData } from '../hooks/useData';
import { AFFECTED, dateTime, initials, remaining, shortDate } from '../utils/format';
import { Detail, PriorityChip, SlaBadge, StatusChip } from './ui';
import Lifecycle from './Lifecycle';
import ChatThread from './ChatThread';
import AdminIncidentControls from './AdminIncidentControls';

/** One incident in a list: a summary row that expands into details, actions, history and chat. */
export default function IncidentRow({ incident: i, expanded, onToggle, user, onAction, busy, adminControls }) {
  const [draft, setDraft] = useState('');
  const [note, setNote] = useState('');
  const [resolution, setResolution] = useState({ summary: '', rootCause: '', steps: '' });
  const tech = user.role === 'TECHNICIAN';
  const employee = user.role === 'EMPLOYEE';
  const staff = !employee;
  return <article className={`incident-row ${expanded ? 'is-open' : ''}`}>
    <button className="incident-summary" onClick={onToggle} aria-expanded={expanded}>
      <div className="incident-id"><span className="incident-avatar"><Activity size={17} /></span><span><b>{i.publicId}</b><small>{shortDate(i.createdAt)}</small></span></div>
      <div className="incident-main"><b>{i.title}</b><small>{i.serviceName || i.category} · {employee ? 'Reported by you' : i.reporterName}</small></div>
      <PriorityChip priority={i.priority} />
      <StatusChip status={i.status} />
      <span className="incident-owner"><span className="avatar small">{initials(i.technicianName || '—')}</span><span>{i.technicianName || 'Unassigned'}</span></span>
      <ChevronDown size={17} className="row-chevron" />
    </button>
    {expanded && <IncidentDetails i={i} user={user} staff={staff} tech={tech} employee={employee} onAction={onAction} busy={busy}
      draft={draft} setDraft={setDraft} note={note} setNote={setNote} resolution={resolution} setResolution={setResolution} adminControls={adminControls} />}
  </article>;
}

function IncidentDetails({ i, user, staff, tech, employee, onAction, busy, draft, setDraft, note, setNote, resolution, setResolution, adminControls }) {
  const detail = useData(`/incidents/${i.publicId}`, { enabled: staff });
  const suggestions = useData(`/incidents/${i.publicId}/suggestions`, { enabled: tech && i.status !== 'CLOSED' });
  const audit = detail.data?.audit || [];
  const related = detail.data?.related || [];
  const open = i.status !== 'CLOSED';
  return <div className="incident-details">
    <div className="badges-row"><SlaBadge status={i.slaStatus} />{i.escalationLevel > 0 && <span className="sla-badge sla-at_risk"><AlertTriangle size={13} /> Escalated · level {i.escalationLevel}</span>}
      {i.correlationGroup && staff && <span className="sla-badge related"><Link2 size={13} /> Related incidents · group {i.correlationGroup}</span>}</div>
    <div className="detail-grid">
      <Detail label="Description" value={i.description} />
      <Detail label="Category / service" value={`${i.category || '—'} · ${i.serviceName || '—'}`} />
      <Detail label="Priority" value={`${i.priority} · score ${i.priorityScore}/${i.priorityMaxScore || 64}`} />
      {staff && <Detail label="Impact / urgency / criticality" value={`${i.impact} / ${i.urgency} / ${i.criticality} · ${AFFECTED[i.affectedUsers] || ''}`} />}
      <Detail label="Response due" value={open && !['ACKNOWLEDGED', 'IN_PROGRESS', 'RESOLVED', 'VERIFIED'].includes(i.status) ? `${dateTime(i.responseDueAt)} (${remaining(i.responseDueAt)})` : dateTime(i.responseDueAt)} />
      <Detail label="Resolution due" value={open && !i.resolvedAt ? `${dateTime(i.resolutionDueAt)} (${remaining(i.resolutionDueAt)})` : dateTime(i.resolutionDueAt)} />
      <Detail label="Technician" value={i.technicianName ? `${i.technicianName} (${i.technicianId})` : 'Not assigned yet'} />
      {i.closedAt && <Detail label="Closed" value={`${dateTime(i.closedAt)} · ${i.resolutionSlaViolated ? 'resolved late' : 'resolved on time'}`} />}
    </div>
    <Lifecycle status={i.status} detailed={staff} />
    {i.resolutionSummary && <div className="resolution-note"><Check size={16} /><span><b>Resolution</b>{i.resolutionSummary}{i.rootCause && <><br /><small>Root cause: {i.rootCause}</small></>}{i.resolutionSteps && <><br /><small>Steps: {i.resolutionSteps}</small></>}</span></div>}
    {!!i.noteEntries?.length && <div className="note-list"><b>Notes</b>{i.noteEntries.map((n) => <div key={n.id}><small>{dateTime(n.createdAt)} · {n.authorName} · {n.type.replaceAll('_', ' ').toLowerCase()}</small><p>{n.content}</p></div>)}</div>}

    {i.status === 'RESOLVED' && (employee || tech) && !(tech ? i.technicianApproved : i.employeeApproved) ? <div className="detail-actions">
      <p>Both you and the {employee ? 'technician' : 'employee'} need to approve before the incident closes.</p>
      <button className="btn primary" onClick={() => onAction('closure/approve')} disabled={busy}><Check size={15} /> Approve closure</button>
      <button className="btn ghost" onClick={() => onAction('closure/reject')} disabled={busy}>Reject closure</button>
    </div> : i.status === 'RESOLVED' && (employee || tech) ? <p className="waiting-note">You approved the closure. Waiting for the {employee ? 'technician' : 'employee'}.</p> : null}

    {i.status === 'ASSIGNED' && tech && <div className="detail-actions">
      <button className="btn primary" onClick={() => onAction('accept')} disabled={busy}><Check size={15} /> Accept request</button>
      <button className="btn ghost" onClick={() => onAction('reject', { reason: window.prompt('Reason for declining (optional)') || '' })} disabled={busy}>Decline</button>
    </div>}

    {employee && open && i.status !== 'RESOLVED' && <div className="inline-note">
      <input value={note} onChange={(e) => setNote(e.target.value)} placeholder="Add information for your technician" aria-label="Add information" />
      <button className="btn ghost" disabled={note.trim().length < 2} onClick={() => { onAction('info', { content: note }); setNote(''); }}>Add information</button>
    </div>}

    {tech && ['ACKNOWLEDGED', 'IN_PROGRESS'].includes(i.status) && <div className="work-actions">
      {i.status === 'ACKNOWLEDGED' && <button className="btn primary" onClick={() => onAction('start')}>Start work <ArrowRight size={15} /></button>}
      <div className="inline-note"><input value={note} onChange={(e) => setNote(e.target.value)} placeholder="Investigation note" aria-label="Investigation note" />
        <button className="btn ghost" disabled={note.trim().length < 2} onClick={() => { onAction('note', { content: note }); setNote(''); }}>Add note</button>
        <button className="btn soft" disabled={draft.trim().length < 2} onClick={() => { onAction('request-info', { content: draft }); setDraft(''); }}><MessageSquare size={15} /> Request information</button></div>
      {i.status === 'IN_PROGRESS' && <div className="resolve-form">
        <b>Resolve incident</b>
        <input placeholder="Resolution summary (required)" value={resolution.summary} onChange={(e) => setResolution({ ...resolution, summary: e.target.value })} />
        <input placeholder="Root cause" value={resolution.rootCause} onChange={(e) => setResolution({ ...resolution, rootCause: e.target.value })} />
        <input placeholder="Steps taken" value={resolution.steps} onChange={(e) => setResolution({ ...resolution, steps: e.target.value })} />
        <button className="btn primary" disabled={resolution.summary.trim().length < 5} onClick={() => onAction('resolve', resolution)}>Resolve</button>
      </div>}
      <small className="muted-text">"Request information" sends the text typed in the conversation box below to the employee.</small>
    </div>}

    {tech && !!suggestions.data?.length && <div className="suggestions"><b><Lightbulb size={15} /> Suggested resolutions from similar closed incidents</b>
      {suggestions.data.map((s) => <div key={s.publicId}><small>{s.publicId} · similarity {Math.round(s.similarity * 100)}%</small><p><b>{s.title}</b> — {s.resolutionSummary}</p></div>)}</div>}

    {employee && ['RESOLVED', 'CLOSED'].includes(i.status) && <FeedbackForm incident={i} />}
    {staff && i.feedbackRating && <p className="muted-text"><Star size={14} /> Feedback: {i.feedbackRating}/5 {i.feedbackComment && `— "${i.feedbackComment}"`}</p>}

    {staff && related.length > 0 && <div className="related"><b>Related incidents</b>{related.map((r) => <span key={r.publicId}>{r.publicId} · {r.title} · {r.reporterName} · {r.status.replace('_', ' ').toLowerCase()}</span>)}</div>}
    {adminControls && <AdminIncidentControls incident={i} />}
    <ChatThread incident={i} user={user} text={draft} setText={setDraft} />
    {staff && <details className="audit-inline"><summary>Audit trail ({audit.length})</summary>
      {audit.slice().reverse().map((a) => <div key={a.id}><time>{dateTime(a.at)}</time> — {a.action.replaceAll('_', ' ').toLowerCase()}: {a.detail}{a.previousStatus && ` (${a.previousStatus} → ${a.newStatus})`}</div>)}</details>}
  </div>;
}

function FeedbackForm({ incident }) {
  const client = useQueryClient();
  const [rating, setRating] = useState(0);
  const [comment, setComment] = useState('');
  const send = useMutation({ mutationFn: () => API.post(`/incidents/${incident.publicId}/feedback`, { rating, comment }), onSuccess: () => { toast.success('Thank you for your feedback'); client.invalidateQueries(); } });
  if (incident.feedbackRating) return <p className="muted-text"><Star size={14} /> You rated this {incident.feedbackRating}/5{incident.feedbackComment && ` — "${incident.feedbackComment}"`}</p>;
  return <div className="feedback-form"><b>How was the support you received?</b>
    <div className="stars" role="radiogroup" aria-label="Rating">{[1, 2, 3, 4, 5].map((n) => <button key={n} type="button" aria-label={`${n} star`} className={n <= rating ? 'on' : ''} onClick={() => setRating(n)}><Star size={20} /></button>)}</div>
    <input placeholder="Optional comment" value={comment} maxLength={1000} onChange={(e) => setComment(e.target.value)} />
    <button className="btn primary" disabled={!rating || send.isPending} onClick={() => send.mutate()}>Submit feedback</button>
  </div>;
}
