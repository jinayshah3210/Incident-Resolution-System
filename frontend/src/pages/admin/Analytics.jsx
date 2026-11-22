import React, { useState } from 'react';
import { ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid, PieChart, Pie, Cell, LineChart, Line, Legend } from 'recharts';
import { ChevronDown } from 'lucide-react';
import { useData } from '../../hooks/useData';
import { EmptyState, ErrorBanner, LoadingState, PageShell, PageTitle } from '../../components/ui';

const PALETTE = ['#3b5bdb', '#0b8a7f', '#e8590c', '#5146c8', '#2f9e44', '#c92a2a', '#868e96', '#1098ad', '#d6336c'];
const toRows = (m = {}) => Object.entries(m).map(([name, value]) => ({ name: name.replaceAll('_', ' '), value }));

/** Monthly analytics accordion: overview pie, service histogram, technician load and trends. */
export default function Analytics() {
  const months = useData('/admin/reports/months');
  const [open, setOpen] = useState(null);
  const current = open ?? months.data?.[0]?.month;
  return <PageShell>
    <PageTitle eyebrow="ADMIN · ANALYTICS" title="Service desk analytics" description="Reports are computed from live data. Select a month to expand it." />
    {months.isLoading ? <LoadingState /> : months.isError ? <ErrorBanner /> : !months.data?.length ? <EmptyState title="No data yet" />
      : months.data.map(({ month, report }) => <section key={month} className="panel month-report">
        <button className="month-head" onClick={() => setOpen(current === month ? '' : month)} aria-expanded={current === month}>
          <b>{new Date(`${month}-01T00:00:00`).toLocaleDateString('en-GB', { month: 'long', year: 'numeric' })}</b>
          <span>{report.total} incidents · {report.slaViolations} SLA violations · {report.slaCompliance}% compliance</span><ChevronDown size={18} /></button>
        {current === month && <MonthReport r={report} />}
      </section>)}
  </PageShell>;
}

function MonthReport({ r }) {
  const overview = [{ name: 'Open', value: r.open }, { name: 'Resolved / closed', value: r.resolved }, { name: 'Critical (P1)', value: r.critical }, { name: 'SLA violated', value: r.slaViolations }];
  return <div className="report-body">
    <div className="kpi-row">
      {[['Total', r.total], ['Open', r.open], ['Closed', r.closed], ['Critical', r.critical], ['SLA compliance', `${r.slaCompliance}%`],
        ['Avg response', `${r.averageResponseMinutes} min`], ['Avg resolution', `${r.averageResolutionMinutes} min`], ['Avg rating', r.feedbackCount ? `${r.averageRating} / 5` : '—'], ['Escalated', r.escalations]]
        .map(([k, v]) => <div key={k} className="kpi"><small>{k}</small><b>{v}</b></div>)}
    </div>
    <div className="chart-grid">
      <Chart title="Incident overview"><PieChart><Pie data={overview} dataKey="value" nameKey="name" innerRadius={55} outerRadius={90}>{overview.map((_, i) => <Cell key={i} fill={PALETTE[i]} />)}</Pie><Tooltip /><Legend /></PieChart></Chart>
      <Chart title="Incidents per service"><BarChart data={toRows(r.services)}><CartesianGrid strokeDasharray="3 3" vertical={false} /><XAxis dataKey="name" interval={0} angle={-20} textAnchor="end" height={60} fontSize={12} /><YAxis allowDecimals={false} /><Tooltip /><Bar dataKey="value" name="Incidents" fill={PALETTE[0]} /></BarChart></Chart>
      <Chart title="Active incidents per technician"><BarChart data={toRows(r.technicians)} layout="vertical"><CartesianGrid strokeDasharray="3 3" horizontal={false} /><XAxis type="number" allowDecimals={false} /><YAxis type="category" dataKey="name" width={120} fontSize={12} /><Tooltip /><Bar dataKey="value" name="Active" fill={PALETTE[1]} /></BarChart></Chart>
      <Chart title="By priority"><BarChart data={toRows(r.priorities)}><CartesianGrid strokeDasharray="3 3" vertical={false} /><XAxis dataKey="name" /><YAxis allowDecimals={false} /><Tooltip /><Bar dataKey="value" name="Incidents">{toRows(r.priorities).map((_, i) => <Cell key={i} fill={['#c92a2a', '#e8590c', '#f59f00', '#2f9e44'][i]} />)}</Bar></BarChart></Chart>
      <Chart title="By category"><BarChart data={toRows(r.categories)}><CartesianGrid strokeDasharray="3 3" vertical={false} /><XAxis dataKey="name" interval={0} angle={-20} textAnchor="end" height={60} fontSize={12} /><YAxis allowDecimals={false} /><Tooltip /><Bar dataKey="value" name="Incidents" fill={PALETTE[3]} /></BarChart></Chart>
      <Chart title="Daily trend"><LineChart data={r.trend}><CartesianGrid strokeDasharray="3 3" vertical={false} /><XAxis dataKey="date" fontSize={12} /><YAxis allowDecimals={false} /><Tooltip /><Legend /><Line dataKey="created" name="Created" stroke={PALETTE[0]} /><Line dataKey="closed" name="Closed" stroke={PALETTE[4]} /></LineChart></Chart>
    </div>
  </div>;
}

function Chart({ title, children }) {
  return <section className="chart-card"><h3>{title}</h3><div className="chart-holder"><ResponsiveContainer width="100%" height="100%">{children}</ResponsiveContainer></div></section>;
}
