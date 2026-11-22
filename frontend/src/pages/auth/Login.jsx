import React, { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, ArrowRight } from 'lucide-react';
import { API } from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import { roles } from '../../utils/roles';
import { Brand } from '../../components/ui';

/** Portal sign-in. Fields start empty and no credentials are displayed. */
export default function Login() {
  const { role = 'employee' } = useParams();
  const key = role.toUpperCase();
  const cfg = roles[key] || roles.EMPLOYEE;
  const nav = useNavigate();
  const { login } = useAuth();
  const [form, setForm] = useState({ login: '', password: '' });
  const [busy, setBusy] = useState(false);
  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    try {
      const { data } = await API.post('/auth/login', { ...form, role: key });
      login(data.user, data.token);
      nav(cfg.path);
    } catch { /* the interceptor shows the error */ } finally { setBusy(false); }
  }
  return <div className="auth-page simple">
    <form className="auth-form" onSubmit={submit} autoComplete="off">
      <Link to="/" className="back-link"><ArrowLeft size={15} /> Back</Link>
      <Brand />
      <div className="auth-heading"><span className="eyebrow muted">{cfg.label.toUpperCase()} PORTAL</span><h2>Sign in</h2><p>Use your work email or account ID.</p></div>
      <label>Work email or ID<input value={form.login} onChange={(e) => setForm({ ...form, login: e.target.value })} autoComplete="off" required /></label>
      <label>Password<input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} autoComplete="new-password" required /></label>
      <button className="btn primary full" disabled={busy}>{busy ? 'Signing in…' : `Continue as ${cfg.label}`}<ArrowRight size={16} /></button>
      <div className="portal-switch">{Object.entries(roles).map(([k, v]) => <Link key={k} className={key === k ? 'selected' : ''} to={`/login/${k.toLowerCase()}`}>{v.label}</Link>)}</div>
    </form>
  </div>;
}
