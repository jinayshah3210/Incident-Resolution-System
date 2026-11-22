import React from 'react';
import { Link } from 'react-router-dom';
import { Activity, AlertTriangle, ArrowRight, BarChart3, Check, ClipboardList, FileText, Headphones, Layers3, Plus, Settings, ShieldCheck, Ticket, Users } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useData } from '../../hooks/useData';
import { roles } from '../../utils/roles';
import { greeting } from '../../utils/format';
import { ErrorBanner, StatCard } from '../../components/ui';

const OPTIONS = {
  ADMIN: [['Ongoing incidents', 'Monitor open work and service levels.', Activity, '/admin/ongoing'], ['Past incidents', 'Review closed incidents and outcomes.', ClipboardList, '/admin/past'],
    ['Manage employees', 'Maintain employee accounts.', Users, '/admin/employees'], ['Manage technicians', 'Skills, availability and workload.', Headphones, '/admin/technicians'],
    ['Manage IT services', 'Service catalog and criticality.', Layers3, '/admin/services'], ['System configuration', 'Strategies, SLA policy and categories.', Settings, '/admin/config'],
    ['Edit ongoing incidents', 'Reassign, re-prioritise or advance incidents.', FileText, '/admin/edit'], ['Analytics', 'Monthly reports and charts.', BarChart3, '/admin/analytics'],
    ['SLA violations', 'Follow up on missed targets.', AlertTriangle, '/admin/sla']],
  TECHNICIAN: [['Assignment requests', 'Accept or decline incidents offered to you.', Ticket, '/technician/offers'], ['Current work', 'Progress, notes and resolutions.', Activity, '/technician/ongoing'],
    ['Past incidents', 'Completed work with its audit trail.', ClipboardList, '/technician/past']],
  EMPLOYEE: [['Create a ticket', 'Report a new IT incident.', Plus, '/employee/create'], ['Track ongoing incidents', 'Status, technician and conversation.', Activity, '/employee/ongoing'],
    ['Past incidents', 'Closed incidents and feedback.', ClipboardList, '/employee/past']],
};

/** Role dashboard (no back button): summary figures and the role's main options. */
export default function Dashboard() {
  const { user } = useAuth();
  const role = user.role;
  const cfg = roles[role];
  const path = role === 'ADMIN' ? '/admin/incidents?scope=all' : role === 'TECHNICIAN' ? '/technician/incidents?scope=all' : '/incidents/mine?scope=all';
  const q = useData(path, { refetchInterval: 10000 });
  const workload = useData('/technician/workload', { enabled: role === 'TECHNICIAN' });
  const offers = useData('/technician/offers', { enabled: role === 'TECHNICIAN', refetchInterval: 10000 });
  const all = q.data || [];
  const open = all.filter((i) => i.status !== 'CLOSED');
  const dash = q.isLoading ? '—' : undefined;
  return <div className="page-content">
    <div className="welcome-strip plain"><div>
      <div className="welcome-kicker">{new Date().toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}</div>
      <h1>{greeting()}, {user.name.split(' ')[0]}</h1>
      <p>{role === 'ADMIN' ? 'Service desk overview.' : role === 'TECHNICIAN' ? 'Your queue and current workload.' : 'Your reported incidents at a glance.'}</p>
    </div></div>
    {q.isError && <ErrorBanner />}
    <div className="stat-grid">
      <StatCard icon={Ticket} label={role === 'EMPLOYEE' ? 'Total reported' : 'Total incidents'} value={dash ?? all.length} tone="blue" />
      <StatCard icon={Activity} label="Ongoing" value={dash ?? open.length} tone="teal" />
      <StatCard icon={Check} label="Closed" value={dash ?? all.length - open.length} tone="violet" />
      {role === 'TECHNICIAN' && <StatCard icon={Layers3} label="Workload" value={`${workload.data?.current ?? 0} / ${workload.data?.maximum ?? '—'}`} change={`${offers.data?.length ?? 0} pending request(s)`} tone="orange" />}
      {role === 'ADMIN' && <StatCard icon={AlertTriangle} label="SLA violated" value={dash ?? all.filter((i) => i.slaViolated).length} change={`${all.filter((i) => i.slaStatus === 'AT_RISK').length} at risk`} tone="red" />}
    </div>
    <div className="section-heading"><h2>{role === 'ADMIN' ? 'Administration' : 'What would you like to do?'}</h2></div>
    <div className={`action-grid ${role === 'ADMIN' ? 'admin-grid' : ''}`}>{OPTIONS[role].map(([t, d, Icon, p]) => <Link to={p} key={p} className="action-card">
      <span className="action-icon"><Icon size={20} /></span><h3>{t}</h3><p>{d}</p><span className="action-link">Open <ArrowRight size={14} /></span></Link>)}</div>
    {role === 'ADMIN' && <div className="bottom-insight"><ShieldCheck size={18} /><span>The audit log records every incident and administrative action.</span><Link to="/admin/audit">Open audit log <ArrowRight size={15} /></Link></div>}
    <span className="sr-only">{cfg.label}</span>
  </div>;
}
