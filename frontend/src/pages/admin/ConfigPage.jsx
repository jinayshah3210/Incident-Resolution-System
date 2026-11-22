import React, { useEffect, useState } from 'react';
import { Check } from 'lucide-react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { API } from '../../api/client';
import { useData } from '../../hooks/useData';
import { ErrorBanner, LoadingState, PageShell, PageTitle } from '../../components/ui';

const LEVELS = ['P1', 'P2', 'P3', 'P4'];

/** System configuration: strategies (Strategy pattern, switched at runtime), SLA policy and categories. */
export default function ConfigPage() {
  const q = useData('/admin/settings');
  const strategies = useData('/admin/strategies');
  const client = useQueryClient();
  const [vals, setVals] = useState(null);
  useEffect(() => { if (q.data) setVals({ ...q.data }); }, [q.data]);
  const change = (k, v) => setVals((old) => ({ ...old, [k]: v }));
  const save = useMutation({ mutationFn: () => API.put('/admin/settings', vals), onSuccess: () => { toast.success('Configuration saved'); client.invalidateQueries({ queryKey: ['/admin/settings'] }); } });
  const options = (kind) => Object.entries(strategies.data?.[kind] || {});
  if (q.isLoading || !vals) return <PageShell>{q.isError ? <ErrorBanner /> : <LoadingState />}</PageShell>;
  return <PageShell>
    <PageTitle eyebrow="ADMIN · CONFIGURATION" title="System configuration" description="Changes apply to new incidents immediately, without a restart, and are written to the audit log." />
    <form onSubmit={(e) => { e.preventDefault(); save.mutate(); }}>
      <div className="config-grid">
        <section className="panel config-card"><h3>Priority strategy</h3><p>How impact, urgency, affected users and service criticality become P1–P4.</p>
          <select value={vals['priority.strategy']} onChange={(e) => change('priority.strategy', e.target.value)}>{options('priority').map(([k, n]) => <option key={k} value={k}>{n}</option>)}</select></section>
        <section className="panel config-card"><h3>Assignment strategy</h3><p>Only active, service-linked technicians with capacity are considered.</p>
          <select value={vals['assignment.strategy']} onChange={(e) => change('assignment.strategy', e.target.value)}>{options('assignment').map(([k, n]) => <option key={k} value={k}>{n}</option>)}</select></section>
        <section className="panel config-card"><h3>SLA warning</h3><p>Warn the technician when this share of a target has elapsed.</p>
          <div className="suffix-input"><input type="number" min="50" max="99" value={vals['sla.warning.percent']} onChange={(e) => change('sla.warning.percent', e.target.value)} /><span>%</span></div></section>
        <section className="panel config-card"><h3>Automatic escalation</h3><p>Unacknowledged P1/P2 incidents escalate technician → team lead → admin → IT manager.</p>
          <select value={vals['escalation.enabled']} onChange={(e) => change('escalation.enabled', e.target.value)}><option value="true">Enabled</option><option value="false">Disabled</option></select></section>
      </div>
      <section className="panel sla-editor"><h3>SLA targets (minutes)</h3><p>Resolution must be at least the response target. Existing incidents keep their deadlines.</p>
        <table className="data-table"><thead><tr><th>Priority</th><th>Response</th><th>Resolution</th></tr></thead><tbody>
          {LEVELS.map((p) => <tr key={p}><td><b className={`priority ${p.toLowerCase()}`}>{p}</b></td>
            <td><input type="number" min="1" max="43200" aria-label={`${p} response minutes`} value={vals[`sla.${p}.response.minutes`] ?? ''} onChange={(e) => change(`sla.${p}.response.minutes`, e.target.value)} /></td>
            <td><input type="number" min="1" max="43200" aria-label={`${p} resolution minutes`} value={vals[`sla.${p}.resolution.minutes`] ?? ''} onChange={(e) => change(`sla.${p}.resolution.minutes`, e.target.value)} /></td></tr>)}
        </tbody></table></section>
      <div className="save-row"><button type="submit" className="btn primary" disabled={save.isPending}><Check size={15} />{save.isPending ? 'Saving…' : 'Save changes'}</button></div>
    </form>
    <CategoryEditor />
  </PageShell>;
}

function CategoryEditor() {
  const q = useData('/admin/categories');
  const client = useQueryClient();
  const [selected, setSelected] = useState(null);
  const [keywords, setKeywords] = useState('');
  const [name, setName] = useState('');
  const refresh = () => { client.invalidateQueries({ queryKey: ['/admin/categories'] }); client.invalidateQueries({ queryKey: ['/catalog/categories'] }); };
  const add = useMutation({ mutationFn: () => API.post('/admin/categories', { name, description: name }), onSuccess: () => { toast.success('Category added'); setName(''); refresh(); } });
  const saveWords = useMutation({
    mutationFn: () => API.put(`/admin/categories/${selected.id}/keywords`, { keywords: Object.fromEntries(keywords.split(',').map((w) => w.trim()).filter(Boolean).map((w) => { const [k, n] = w.split(':'); return [k.trim(), Number(n) || 1]; })) }),
    onSuccess: () => { toast.success('Keywords saved'); refresh(); },
  });
  const toggle = useMutation({ mutationFn: (c) => API.put(`/admin/categories/${c.id}`, { active: !c.active }), onSuccess: refresh });
  const pick = (c) => { setSelected(c); setKeywords(Object.entries(c.keywords).map(([k, w]) => `${k}:${w}`).join(', ')); };
  return <section className="panel"><h3>Categories and classifier keywords</h3>
    <p>New categories and keywords take effect for the next incident; no code change is needed. Format: keyword:weight, separated by commas.</p>
    {q.isLoading ? <LoadingState /> : q.isError ? <ErrorBanner /> : <table className="data-table"><thead><tr><th>Category</th><th>Keywords</th><th>Technicians</th><th>Open incidents</th><th>Status</th><th /></tr></thead>
      <tbody>{q.data.map((c) => <tr key={c.id} className={selected?.id === c.id ? 'row-selected' : ''}><td><b>{c.name}</b></td><td>{Object.keys(c.keywords).length}</td><td>{c.technicianCount}</td><td>{c.openIncidents}</td>
        <td>{c.active ? 'Active' : 'Inactive'}</td><td><button className="btn ghost small" onClick={() => pick(c)}>Keywords</button> <button className="btn ghost small" onClick={() => toggle.mutate(c)}>{c.active ? 'Deactivate' : 'Activate'}</button></td></tr>)}</tbody></table>}
    {selected && <div className="keyword-editor"><b>{selected.name} keywords</b><textarea rows={3} value={keywords} onChange={(e) => setKeywords(e.target.value)} />
      <div className="inline-note"><button className="btn primary" onClick={() => saveWords.mutate()}>Save keywords</button><button className="btn ghost" onClick={() => setSelected(null)}>Close</button></div></div>}
    <div className="inline-note"><input placeholder="New category name" value={name} onChange={(e) => setName(e.target.value)} /><button className="btn ghost" disabled={name.trim().length < 3} onClick={() => add.mutate()}>Add category</button></div>
  </section>;
}
