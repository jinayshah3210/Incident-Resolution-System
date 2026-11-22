import { Activity, AlertTriangle, BarChart3, ClipboardList, FileText, Headphones, LayoutDashboard, Layers3, Plus, Settings, ShieldCheck, Ticket, Users } from 'lucide-react';

/** Navigation and accent per portal. */
export const roles = {
  EMPLOYEE: { label: 'Employee', color: '#3b5bdb', path: '/employee', items: [
    ['Overview', LayoutDashboard, '/employee'], ['Create ticket', Plus, '/employee/create'],
    ['Ongoing incidents', Activity, '/employee/ongoing'], ['Past incidents', ClipboardList, '/employee/past']] },
  TECHNICIAN: { label: 'Technician', color: '#0b8a7f', path: '/technician', items: [
    ['Overview', LayoutDashboard, '/technician'], ['Assignment requests', Ticket, '/technician/offers'],
    ['Current work', Activity, '/technician/ongoing'], ['Past incidents', ClipboardList, '/technician/past']] },
  ADMIN: { label: 'Administrator', color: '#5146c8', path: '/admin', items: [
    ['Overview', LayoutDashboard, '/admin'], ['Ongoing incidents', Activity, '/admin/ongoing'],
    ['Past incidents', ClipboardList, '/admin/past'], ['Employees', Users, '/admin/employees'],
    ['Technicians', Headphones, '/admin/technicians'], ['IT services', Layers3, '/admin/services'],
    ['System configuration', Settings, '/admin/config'], ['Edit incidents', FileText, '/admin/edit'],
    ['Analytics', BarChart3, '/admin/analytics'], ['SLA violations', AlertTriangle, '/admin/sla'],
    ['Audit log', ShieldCheck, '/admin/audit']] },
};
