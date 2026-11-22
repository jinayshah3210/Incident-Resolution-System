import React, { useState } from 'react';
import { ChevronDown, X } from 'lucide-react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { API } from '../../api/client';
import { useData } from '../../hooks/useData';
import { initials, shortDate } from '../../utils/format';
import { EmptyState, ErrorBanner, LoadingState, PageShell, PageTitle } from '../../components/ui';

const BLANK = { name: '', email: '', password: '', department: '', skills: [], maxWorkload: 8, experienceYears: 1 };

/** Manage employees or technicians: search, expandable cards, add form, soft remove with confirmation. */
export default function PeoplePage({ role }) {
  const path = `/admin/users?role=${role}`;
  const q = useData(path);
  const categories = useData('/catalog/categories', { enabled: role === 'TECHNICIAN' });
  const client = useQueryClient();
  const [show, setShow] = useState(false);
  const [search, setSearch] = useState('');
  const [open, setOpen] = useState(null);
  const [form, setForm] = useState(BLANK);
  const refresh = () => client.invalidateQueries({ queryKey: [path] });
  const add = useMutation({ mutationFn: () => API.post('/admin/users', { ...form, role }), onSuccess: () => { toast.success('Account created'); setShow(false); setForm(BLANK); refresh(); } });
  const remove = useMutation({ mutationFn: (id) => API.delete(`/admin/users/${id}`), onSuccess: () => { toast.success('Account deactivated; history kept'); refresh(); client.invalidateQueries(); } });
  const restore = useMutation({ mutationFn: (id) => API.post(`/admin/users/${id}/reactivate`), onSuccess: () => { toast.success('Account reactivated'); refresh(); } });
  const tech = role === 'TECHNICIAN';
  const people = (q.data || []).filter((p) => `${p.id} ${p.name} ${p.email}`.toLowerCase().includes(search.toLowerCase()));
  const toggleSkill = (s) => setForm({ ...form, skills: form.skills.includes(s) ? form.skills.filter((x) => x !== s) : [...form.skills, s] });
  return <PageShell>
    <PageTitle eyebrow="ADMIN · DIRECTORY" title={tech ? 'Manage technicians' : 'Manage employees'} description="Removing an account disables sign-in and keeps its history."
      action={<button className="btn primary" onClick={() => setShow(!show)}>{show ? 'Cancel' : `Add ${tech ? 'technician' : 'employee'}`}</button>} />
    {show && <form className="panel people-form" onSubmit={(e) => { e.preventDefault(); add.mutate(); }}>
      <div className="edit-grid">
        <label>Full name<input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></label>
        <label>Work email<input required type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></label>
        <label>Initial password<input required type="password" minLength={8} value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} /></label>
        <label>Department<input value={form.department} onChange={(e) => setForm({ ...form, department: e.target.value })} /></label>
        {tech && <><label>Maximum workload<input type="number" min="1" max="50" value={form.maxWorkload} onChange={(e) => setForm({ ...form, maxWorkload: Number(e.target.value) })} /></label>
          <label>Experience (years)<input type="number" min="0" max="50" value={form.experienceYears} onChange={(e) => setForm({ ...form, experienceYears: Number(e.target.value) })} /></label>
          <fieldset className="span-2 skill-picker"><legend>Skills</legend>{(categories.data || []).map((c) => <label key={c}><input type="checkbox" checked={form.skills.includes(c)} onChange={() => toggleSkill(c)} /> {c}</label>)}</fieldset></>}
      </div>
      <button className="btn primary" disabled={add.isPending}>{add.isPending ? 'Creating…' : 'Create account'}</button></form>}
    <div className="filter-bar"><div className="search-box"><input placeholder="Search by name, ID or email" value={search} onChange={(e) => setSearch(e.target.value)} /></div>
      <span className="results-count">{(q.data || []).filter((x) => x.active).length} active · {(q.data || []).filter((x) => !x.active).length} inactive</span></div>
    <div className="panel">{q.isLoading ? <LoadingState /> : q.isError ? <ErrorBanner /> : !people.length ? <EmptyState title="No matching people" />
      : <div className="people-list">{people.map((p) => <div key={p.id} className={`person-row ${p.active ? '' : 'inactive'}`}>
        <button className="person-head" onClick={() => setOpen(open === p.id ? null : p.id)} aria-expanded={open === p.id}>
          <span className="avatar">{initials(p.name)}</span><span className="person-info"><b>{p.name}</b><small>{p.id} · {p.department || p.role}</small></span>
          {tech && <span className="person-load">{p.workload ?? 0}/{p.maxWorkload} · {p.availability}</span>}
          <span className={`account-status ${p.active ? 'active' : 'inactive'}`}>{p.active ? 'Active' : 'Inactive'}</span><ChevronDown size={16} /></button>
        {p.active ? <button className="notify-remove" aria-label={`Deactivate ${p.name}`} onClick={() => window.confirm(`Deactivate ${p.name}? ${tech ? 'Their open incidents will be reassigned.' : 'They will no longer be able to sign in.'}`) && remove.mutate(p.id)}><X size={16} /></button>
          : <button className="btn ghost small" onClick={() => restore.mutate(p.id)}>Reactivate</button>}
        {open === p.id && <div className="person-details">
          <span><small>Email</small>{p.email}</span><span><small>Phone</small>{p.phone || '—'}</span>
          {tech ? <><span><small>Skills</small>{(p.skills || []).join(', ')}</span><span><small>Experience</small>{p.experienceYears} years{p.teamLead ? ' · team lead' : ''}</span><span><small>Resolved</small>{p.incidentsResolved ?? 0}</span></>
            : <><span><small>Joined</small>{shortDate(p.joinDate)}</span><span><small>Incidents reported</small>{p.incidentsReported ?? 0}</span><span><small>Closed</small>{p.incidentsResolved ?? 0}</span></>}
        </div>}
      </div>)}</div>}</div>
  </PageShell>;
}
