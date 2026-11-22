import React, { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { API } from '../api/client';
import { useData } from '../hooks/useData';
import { AFFECTED } from '../utils/format';

const NEXT = { CLASSIFIED: ['ASSIGNED', 'ACKNOWLEDGED'], ASSIGNED: ['ACKNOWLEDGED', 'IN_PROGRESS'], ACKNOWLEDGED: ['IN_PROGRESS'], NEW: ['CLASSIFIED', 'ASSIGNED'] };

/** Admin: smart technician recommendation, reassign/unassign, lifecycle override and field edits. */
export default function AdminIncidentControls({ incident }) {
  const client = useQueryClient();
  const recs = useData(`/admin/incidents/${incident.publicId}/recommendations`);
  const categories = useData('/catalog/categories');
  const [tech, setTech] = useState('');
  const [editing, setEditing] = useState(false);
  const [fields, setFields] = useState({ title: incident.title, description: incident.description, impact: incident.impact, urgency: incident.urgency, affectedUsers: incident.affectedUsers, category: incident.category });
  const done = (msg) => { toast.success(msg); client.invalidateQueries(); };
  const assign = useMutation({ mutationFn: () => API.post(`/admin/incidents/${incident.publicId}/assign`, { technicianId: tech }), onSuccess: () => done('Technician reassigned') });
  const unassign = useMutation({ mutationFn: () => API.post(`/admin/incidents/${incident.publicId}/unassign`), onSuccess: () => done('Technician removed; reassignment attempted') });
  const advance = useMutation({ mutationFn: (status) => API.post(`/admin/incidents/${incident.publicId}/advance`, { status }), onSuccess: () => done('Lifecycle updated') });
  const save = useMutation({ mutationFn: () => API.put(`/admin/incidents/${incident.publicId}`, fields), onSuccess: () => { setEditing(false); done('Incident saved; priority and SLA recalculated'); } });
  const open = !['CLOSED', 'RESOLVED', 'VERIFIED'].includes(incident.status);
  if (!open) return null;
  return <section className="panel admin-controls">
    <h3>Administrator controls</h3>
    <div className="recommend">
      <b>Recommended technicians</b>
      <p className="muted-text">Score = 0.4 × skill + 0.3 × (1 − load/max) + 0.2 × experience + 0.1 × past incidents on this service</p>
      {recs.isLoading ? <p>Loading…</p> : <table className="data-table"><thead><tr><th>Technician</th><th>Score</th><th>Skill</th><th>Load</th><th>Experience</th><th>History</th><th>Status</th><th /></tr></thead>
        <tbody>{(recs.data || []).slice(0, 6).map((r) => <tr key={r.technicianId} className={r.eligible ? '' : 'row-muted'}>
          <td>{r.name} <small>{r.technicianId}</small></td><td><b>{r.score.toFixed(2)}</b></td><td>{r.skillMatch}</td>
          <td>{r.load}/{r.maxWorkload}</td><td>{r.experienceYears} yrs</td><td>{r.pastIncidentsOnService}</td><td>{r.note}</td>
          <td>{r.eligible && <button className="btn ghost small" onClick={() => setTech(r.technicianId)}>Select</button>}</td></tr>)}</tbody></table>}
      <div className="inline-note">
        <select value={tech} onChange={(e) => setTech(e.target.value)} aria-label="Technician"><option value="">Choose a technician</option>{(recs.data || []).filter((r) => r.eligible).map((r) => <option key={r.technicianId} value={r.technicianId}>{r.name} · {r.technicianId}</option>)}</select>
        <button className="btn primary" disabled={!tech || assign.isPending} onClick={() => assign.mutate()}>Reassign</button>
        {incident.technicianId && <button className="btn ghost" disabled={unassign.isPending} onClick={() => window.confirm('Remove the technician? The system will reassign automatically.') && unassign.mutate()}>Remove technician</button>}
        {(NEXT[incident.status] || []).map((s) => <button key={s} className="btn ghost" disabled={advance.isPending} onClick={() => advance.mutate(s)}>Move to {s.replace('_', ' ').toLowerCase()}</button>)}
      </div>
    </div>
    {editing ? <div className="edit-grid">
      <label>Title<input value={fields.title} onChange={(e) => setFields({ ...fields, title: e.target.value })} /></label>
      <label>Category<select value={fields.category || ''} onChange={(e) => setFields({ ...fields, category: e.target.value })}>{(categories.data || []).map((c) => <option key={c}>{c}</option>)}</select></label>
      <label className="span-2">Description<textarea rows={3} value={fields.description} onChange={(e) => setFields({ ...fields, description: e.target.value })} /></label>
      <label>Impact (1–4)<input type="number" min="1" max="4" value={fields.impact} onChange={(e) => setFields({ ...fields, impact: Number(e.target.value) })} /></label>
      <label>Urgency (1–4)<input type="number" min="1" max="4" value={fields.urgency} onChange={(e) => setFields({ ...fields, urgency: Number(e.target.value) })} /></label>
      <label>Affected users<select value={fields.affectedUsers} onChange={(e) => setFields({ ...fields, affectedUsers: Number(e.target.value) })}>{[1, 2, 3, 4].map((n) => <option key={n} value={n}>{AFFECTED[n]}</option>)}</select></label>
      <div className="span-2 inline-note"><button className="btn primary" onClick={() => save.mutate()} disabled={save.isPending}>Save changes</button><button className="btn ghost" onClick={() => setEditing(false)}>Cancel</button></div>
    </div> : <button className="btn ghost" onClick={() => setEditing(true)}>Edit incident details</button>}
  </section>;
}
