import React from 'react';
import { Check } from 'lucide-react';

const EMPLOYEE_STEPS = [['Created', ['NEW', 'CLASSIFIED']], ['Assigned technician', ['ASSIGNED', 'ACKNOWLEDGED']], ['In progress', ['IN_PROGRESS']], ['Resolved', ['RESOLVED', 'VERIFIED']], ['Closed', ['CLOSED']]];
const FULL_STEPS = ['NEW', 'CLASSIFIED', 'ASSIGNED', 'ACKNOWLEDGED', 'IN_PROGRESS', 'RESOLVED', 'VERIFIED', 'CLOSED'].map((s) => [s.replace('_', ' ').toLowerCase().replace(/^\w/, (c) => c.toUpperCase()), [s]]);

/** Employees see five friendly steps; technicians and admins see all eight states. */
export default function Lifecycle({ status, detailed }) {
  const steps = detailed ? FULL_STEPS : EMPLOYEE_STEPS;
  const index = steps.findIndex(([, states]) => states.includes(status));
  return <div className="lifecycle">{steps.map(([name], i) => <div key={name} className={`${i <= index ? 'done' : ''} ${i === index ? 'current' : ''}`}>
    <i>{i < index ? <Check size={11} /> : i + 1}</i><span>{name}</span></div>)}</div>;
}
