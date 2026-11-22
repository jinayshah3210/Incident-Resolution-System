import React from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, Headphones, MessageSquare } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useData } from '../../hooks/useData';
import { roles } from '../../utils/roles';
import { ErrorBanner, LoadingState, PageShell, PageTitle } from '../../components/ui';

export default function SupportPage() {
  const { user } = useAuth();
  const q = useData('/support/contact');
  return <PageShell>
    <PageTitle eyebrow="HELP DESK" title="Support and contact" description="Contact the ResolveIT service desk for help with an incident or with the portal." />
    <div className="support-grid">
      <section className="panel support-card"><Headphones size={20} /><h3>Service desk</h3>
        {q.isLoading ? <LoadingState /> : q.isError ? <ErrorBanner /> : <>
          <div className="support-contact"><b>Email</b><a href={`mailto:${q.data.email}`}>{q.data.email}</a></div>
          <div className="support-contact"><b>Phone</b><span>{q.data.phone}</span></div>
          <div className="support-contact"><b>Availability</b><span>{q.data.hours}</span></div></>}
      </section>
      <section className="panel support-card"><MessageSquare size={20} /><h3>Existing incident</h3>
        <p>For updates on an existing incident, open it and use its conversation.</p>
        <Link className="btn primary" to={`${roles[user.role].path}/ongoing`}>Open incidents <ArrowRight size={16} /></Link></section>
    </div>
  </PageShell>;
}
