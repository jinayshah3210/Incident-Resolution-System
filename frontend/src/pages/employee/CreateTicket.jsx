import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { API } from '../../api/client';
import { useData } from '../../hooks/useData';
import { AFFECTED } from '../../utils/format';
import { PageShell, PageTitle } from '../../components/ui';

/** New incident form with field errors and the duplicate-incident dialog. */
export default function CreateTicket() {
  const nav = useNavigate();
  const client = useQueryClient();
  const services = useData('/catalog/services');
  const [form, setForm] = useState({ title: '', description: '', service: '', affectedUsers: '' });
  const [errors, setErrors] = useState({});
  const [duplicate, setDuplicate] = useState(null);
  const create = useMutation({
    mutationFn: (force) => API.post('/incidents', { ...form, service: form.service || null, affectedUsers: form.affectedUsers ? Number(form.affectedUsers) : null, force }).then((r) => r.data),
    onSuccess: (d) => {
      toast.success(`${d.publicId} created · ${d.category} · ${d.priority}${d.technicianName ? ` · offered to ${d.technicianName}` : ' · awaiting a technician'}`, { duration: 6000 });
      client.invalidateQueries();
      nav('/employee/ongoing');
    },
    onError: (e) => {
      const data = e.response?.data;
      if (data?.code === 'DUPLICATE_INCIDENT') setDuplicate(data.details?.existingIncidentId);
      setErrors(data?.fieldErrors || {});
    },
  });
  const set = (k, v) => setForm({ ...form, [k]: v });
  return <PageShell>
    <PageTitle eyebrow="EMPLOYEE · NEW INCIDENT" title="Create a ticket" description="Describe the problem. Category, priority and technician are assigned automatically." />
    <form className="panel ticket-form" onSubmit={(e) => { e.preventDefault(); create.mutate(false); }} noValidate>
      <label>Short summary *<input required minLength="5" maxLength="120" placeholder="e.g. Payment portal will not load" value={form.title} onChange={(e) => set('title', e.target.value)} />
        {errors.title && <span className="field-error">{errors.title}</span>}</label>
      <label>What happened? *<textarea required minLength="10" maxLength="2000" rows="6" placeholder="What were you doing, what did you see, and who is affected?" value={form.description} onChange={(e) => set('description', e.target.value)} />
        <small>{form.description.length}/2,000 characters</small>{errors.description && <span className="field-error">{errors.description}</span>}</label>
      <div className="form-row">
        <label>Affected service<select value={form.service} onChange={(e) => set('service', e.target.value)}><option value="">Identify it for me</option>{(services.data || []).map((s) => <option key={s.id}>{s.name}</option>)}</select></label>
        <label>Who is affected?<select value={form.affectedUsers} onChange={(e) => set('affectedUsers', e.target.value)}><option value="">Work it out from my description</option>{[1, 2, 3, 4].map((n) => <option key={n} value={n}>{AFFECTED[n]}</option>)}</select></label>
      </div>
      <div className="form-actions"><button type="button" className="btn ghost" onClick={() => nav('/employee')}>Cancel</button>
        <button className="btn primary" disabled={create.isPending}>{create.isPending ? 'Submitting…' : 'Submit ticket'}<ArrowRight size={16} /></button></div>
    </form>
    {duplicate && <div className="modal-backdrop" role="dialog" aria-modal="true"><div className="modal">
      <h3>A similar incident is already open</h3>
      <p>You already reported a very similar problem as <b>{duplicate}</b>. You can follow that incident, or submit this one anyway.</p>
      <div className="form-actions"><button className="btn ghost" onClick={() => nav('/employee/ongoing')}>View existing incident</button>
        <button className="btn primary" onClick={() => { setDuplicate(null); create.mutate(true); }}>Submit anyway</button></div>
    </div></div>}
  </PageShell>;
}
