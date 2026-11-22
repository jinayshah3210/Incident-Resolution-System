import React from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, Headphones, ShieldCheck, Users } from 'lucide-react';
import { Brand } from '../../components/ui';

const PORTALS = [
  ['employee', Users, 'Employee', 'Report an IT incident, follow its progress and confirm the resolution.'],
  ['technician', Headphones, 'Technician', 'Review assignment requests, work your queue and record resolutions.'],
  ['admin', ShieldCheck, 'Administrator', 'Oversee incidents, service levels, the service catalog and the support team.'],
];

/** Public entry page: a restrained, formal introduction and the three portal entry points. */
export default function Landing() {
  return <main className="lp">
    <header className="lp-top"><Brand /><a href="#portals" className="lp-signin">Sign in</a></header>
    <section className="lp-hero">
      <p className="lp-kicker">IT Service Management</p>
      <h1>Incident management for your organisation's IT services</h1>
      <p className="lp-lead">ResolveIT records every IT incident, sets its priority from business impact, assigns a qualified technician
        and tracks it against service-level targets until both parties confirm the resolution.</p>
      <a className="btn primary" href="#portals">Choose a portal <ArrowRight size={16} /></a>
    </section>
    <section id="portals" className="lp-portals" aria-label="Portals">
      {PORTALS.map(([key, Icon, name, text]) => <Link key={key} to={`/login/${key}`} className="lp-portal">
        <Icon size={22} /><h2>{name}</h2><p>{text}</p><span>Sign in as {name.toLowerCase()} <ArrowRight size={15} /></span>
      </Link>)}
    </section>
    <section className="lp-facts">
      <div><h3>Prioritisation</h3><p>Priority is derived from impact, urgency, the number of affected users and the criticality of the service.</p></div>
      <div><h3>Service levels</h3><p>Response and resolution targets are monitored continuously, with warnings, recorded violations and escalation.</p></div>
      <div><h3>Accountability</h3><p>Every action is written to an append-only audit trail, and incidents close only when both sides approve.</p></div>
    </section>
    <footer className="lp-footer"><span>ResolveIT</span><span>Intelligent IT Service Management and Incident Resolution</span></footer>
  </main>;
}
