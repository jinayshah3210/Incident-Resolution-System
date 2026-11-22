import React, { useEffect, useState } from 'react';
import { Check, Plus } from 'lucide-react';
import { ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid } from 'recharts';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { API } from '../../api/client';
import { useData } from '../../hooks/useData';
import { ErrorBanner, LoadingState, PageShell, PageTitle } from '../../components/ui';

const BLANK = { name: '', description: '', category: 'Application', criticality: 3, keywords: '', active: true };

/** IT service catalog: service table, technician links, category statistics and the service histogram. */
export default function ServicesPage() {
  const client = useQueryClient();
  const q = useData('/admin/services');
  const techs = useData('/admin/users?role=TECHNICIAN');
  const categories = useData('/admin/categories');
  const [selected, setSelected] = useState(null);
  const [mode, setMode] = useState('');
  const [techId, setTechId] = useState('');
  const [form, setForm] = useState(BLANK);
  const details = useData(`/admin/services/${selected}`, { enabled: !!selected });
  useEffect(() => { if (details.data && mode !== 'add') setForm({ ...details.data, keywords: details.data.keywords || '' }); }, [details.data]);
  const refresh = () => client.invalidateQueries({ predicate: (x) => String(x.queryKey[0]).includes('/services') });
  const create = useMutation({ mutationFn: () => API.post('/admin/services', form), onSuccess: (r) => { toast.success('Service created'); refresh(); setMode(''); setSelected(r.data.id); } });
  const update = useMutation({ mutationFn: () => API.put(`/admin/services/${selected}`, form), onSuccess: () => { toast.success('Service saved'); setMode(''); refresh(); } });
  const remove = useMutation({ mutationFn: () => API.delete(`/admin/services/${selected}`), onSuccess: (r) => { toast.success(r.data.archived ? 'Service archived to preserve incident history' : 'Service deleted'); setSelected(null); refresh(); } });
  const link = useMutation({ mutationFn: () => API.post(`/admin/services/${selected}/technicians`, { technicianId: techId }), onSuccess: () => { toast.success('Technician linked'); setTechId(''); refresh(); } });
  const unlink = useMutation({ mutationFn: (id) => API.delete(`/admin/services/${selected}/technicians/${id}`), onSuccess: () => { toast.success('Technician unlinked'); refresh(); } });
  const linked = new Set((details.data?.technicians || []).map((t) => t.id));
  return <PageShell>
    <PageTitle eyebrow="ADMIN · SERVICE CATALOG" title="Manage IT services" description="Business criticality feeds directly into incident priority."
      action={<button className="btn primary" onClick={() => { setMode('add'); setSelected(null); setForm(BLANK); }}><Plus size={16} /> Add service</button>} />
    {q.isLoading ? <LoadingState /> : q.isError ? <ErrorBanner /> : <div className="services-layout">
      <section className="panel"><h3>Services</h3><table className="data-table"><thead><tr><th>Service</th><th>Category</th><th>Criticality</th><th>Technicians</th><th>Incidents</th><th>Status</th></tr></thead>
        <tbody>{q.data.map((s) => <tr key={s.id} className={`clickable ${selected === s.id ? 'row-selected' : ''}`} onClick={() => { setSelected(s.id); setMode(''); }}>
          <td><b>{s.name}</b><br /><small>{s.description}</small></td><td>{s.category}</td><td>{s.criticality}/4</td><td>{s.technicianCount}</td><td>{s.incidentCount}</td><td>{s.active ? 'Active' : 'Archived'}</td></tr>)}</tbody></table></section>
      <section className="panel chart-card"><h3>Incidents per service</h3><div className="chart-holder"><ResponsiveContainer width="100%" height="100%">
        <BarChart data={q.data.map((s) => ({ name: s.name, value: s.incidentCount }))}><CartesianGrid strokeDasharray="3 3" vertical={false} /><XAxis dataKey="name" interval={0} angle={-25} textAnchor="end" height={70} fontSize={12} /><YAxis allowDecimals={false} /><Tooltip /><Bar dataKey="value" name="Incidents" fill="#5146c8" /></BarChart>
      </ResponsiveContainer></div></section>
      {categories.data && <section className="panel"><h3>Categories</h3><table className="data-table"><thead><tr><th>Category</th><th>Technicians</th><th>Open incidents</th></tr></thead>
        <tbody>{categories.data.map((c) => <tr key={c.id}><td>{c.name}</td><td>{c.technicianCount}</td><td>{c.openIncidents}</td></tr>)}</tbody></table></section>}
    </div>}
    {(mode === 'add' || selected) && <section className="panel service-detail">
      <h3>{mode === 'add' ? 'New service' : details.data?.name}</h3>
      {mode === 'add' || mode === 'edit' ? <form className="edit-grid" onSubmit={(e) => { e.preventDefault(); (mode === 'add' ? create : update).mutate(); }}>
        <label>Name<input required minLength={3} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></label>
        <label>Category<select value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>{(categories.data || []).filter((c) => c.active).map((c) => <option key={c.id}>{c.name}</option>)}</select></label>
        <label className="span-2">Description<textarea required minLength={8} rows={2} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></label>
        <label>Criticality<select value={form.criticality} onChange={(e) => setForm({ ...form, criticality: Number(e.target.value) })}>{[1, 2, 3, 4].map((n) => <option key={n}>{n}</option>)}</select></label>
        <label>Detection keywords<input value={form.keywords} onChange={(e) => setForm({ ...form, keywords: e.target.value })} placeholder="comma separated" /></label>
        {mode === 'edit' && <label className="inline-check"><input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} /> Active</label>}
        <div className="span-2 inline-note"><button className="btn primary"><Check size={16} /> Save</button><button type="button" className="btn ghost" onClick={() => setMode('')}>Cancel</button></div>
      </form> : details.isLoading ? <LoadingState /> : <>
        <div className="inline-note"><button className="btn ghost" onClick={() => setMode('edit')}>Edit service</button>
          <button className="btn ghost" onClick={() => window.confirm('Remove this service? Services with incidents are archived instead.') && remove.mutate()}>Remove / archive</button></div>
        <h4>Linked technicians</h4>
        <table className="data-table"><tbody>{(details.data?.technicians || []).map((t) => <tr key={t.id}><td>{t.name} ({t.id})</td><td>{(t.skills || []).join(', ')}</td><td>{t.workload}/{t.maxWorkload}</td>
          <td><button className="btn ghost small" onClick={() => unlink.mutate(t.id)}>Unlink</button></td></tr>)}</tbody></table>
        <div className="inline-note"><select value={techId} onChange={(e) => setTechId(e.target.value)}><option value="">Link a technician…</option>
          {(techs.data || []).filter((t) => t.active && !linked.has(t.id)).map((t) => <option key={t.id} value={t.id}>{t.name} ({t.id})</option>)}</select>
          <button className="btn primary" disabled={!techId} onClick={() => link.mutate()}>Link</button></div>
      </>}
    </section>}
  </PageShell>;
}
