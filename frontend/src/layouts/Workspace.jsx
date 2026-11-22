import React, { useState } from 'react';
import { Link, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { ChevronDown, Headphones, LogOut, Menu } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { roles } from '../utils/roles';
import { initials } from '../utils/format';
import { Brand } from '../components/ui';
import NotificationPanel from '../components/NotificationPanel';
import Dashboard from '../pages/shared/Dashboard';
import IncidentList from '../pages/shared/IncidentList';
import SupportPage from '../pages/shared/SupportPage';
import CreateTicket from '../pages/employee/CreateTicket';
import Analytics from '../pages/admin/Analytics';
import AuditPage from '../pages/admin/AuditPage';
import ConfigPage from '../pages/admin/ConfigPage';
import PeoplePage from '../pages/admin/PeoplePage';
import ServicesPage from '../pages/admin/ServicesPage';
import SlaViolationsPage from '../pages/admin/SlaViolationsPage';

/** Authenticated shell: scrollable sticky sidebar, top bar with bell and profile, and role routes. */
export default function Workspace() {
  const { user, logout } = useAuth();
  const cfg = roles[user.role] || roles.EMPLOYEE;
  const loc = useLocation();
  const [collapsed, setCollapsed] = useState(false);
  const [profile, setProfile] = useState(false);
  const title = cfg.items.find((i) => i[2] === loc.pathname)?.[0] || (loc.pathname.endsWith('/support') ? 'Support' : 'Overview');
  if (loc.pathname !== cfg.path && !loc.pathname.startsWith(`${cfg.path}/`)) return <Navigate to={cfg.path} replace />;
  return <div className={`app-shell role-${user.role.toLowerCase()} ${collapsed ? 'nav-collapsed' : ''}`} style={{ '--accent': cfg.color }}>
    <aside className="sidebar" aria-label="Main navigation">
      <div className="side-top"><Link to={cfg.path} className="side-brand"><Brand /></Link>
        <button className="side-toggle" onClick={() => setCollapsed(!collapsed)} aria-label="Toggle navigation"><Menu size={19} /></button></div>
      <nav className="side-nav">{cfg.items.map(([text, Icon, path]) => <Link key={path} to={path} className={loc.pathname === path ? 'active' : ''} title={text}><Icon size={18} /><span>{text}</span></Link>)}</nav>
      <div className="side-bottom">
        <Link className="side-link" to={`${cfg.path}/support`} title="Support"><Headphones size={17} /><span>Support</span></Link>
        <button onClick={logout} className="side-link side-logout" title="Sign out"><LogOut size={17} /><span>Sign out</span></button>
        <div className="side-user"><div className="avatar">{initials(user.name)}</div><div className="user-lines"><b>{user.name}</b><small>{user.id} · {cfg.label}</small></div></div>
      </div>
    </aside>
    <main className="main-area">
      <header className="topbar">
        <div className="crumb"><b>{user.name}</b><span>· {cfg.label} portal · {title}</span></div>
        <div className="topbar-right">
          <NotificationPanel />
          <div className="profile-wrap"><button className="profile-trigger" onClick={() => setProfile(!profile)} aria-expanded={profile} aria-label="Profile"><div className="avatar">{initials(user.name)}</div><ChevronDown size={14} /></button>
            {profile && <div className="profile-menu"><b>{user.name}</b><small>{user.id} · {cfg.label}</small><button onClick={logout}><LogOut size={15} /> Sign out</button></div>}</div>
        </div>
      </header>
      <Routes>
        <Route path="/employee" element={<Dashboard />} />
        <Route path="/technician" element={<Dashboard />} />
        <Route path="/admin" element={<Dashboard />} />
        <Route path="/employee/create" element={<CreateTicket />} />
        <Route path="/employee/ongoing" element={<IncidentList scope="ongoing" />} />
        <Route path="/employee/past" element={<IncidentList scope="past" />} />
        <Route path="/technician/offers" element={<IncidentList scope="offers" />} />
        <Route path="/technician/ongoing" element={<IncidentList scope="ongoing" />} />
        <Route path="/technician/past" element={<IncidentList scope="past" />} />
        <Route path="/admin/ongoing" element={<IncidentList scope="ongoing" />} />
        <Route path="/admin/past" element={<IncidentList scope="past" />} />
        <Route path="/admin/edit" element={<IncidentList scope="ongoing" editMode />} />
        <Route path="/admin/analytics" element={<Analytics />} />
        <Route path="/admin/audit" element={<AuditPage />} />
        <Route path="/admin/config" element={<ConfigPage />} />
        <Route path="/admin/sla" element={<SlaViolationsPage />} />
        <Route path="/admin/employees" element={<PeoplePage role="EMPLOYEE" />} />
        <Route path="/admin/technicians" element={<PeoplePage role="TECHNICIAN" />} />
        <Route path="/admin/services" element={<ServicesPage />} />
        <Route path="/:portal/support" element={<SupportPage />} />
        <Route path="*" element={<Dashboard />} />
      </Routes>
      <footer className="app-footer"><span>ResolveIT Service Desk</span><Link to={`${cfg.path}/support`}>Support / Contact</Link></footer>
    </main>
  </div>;
}
