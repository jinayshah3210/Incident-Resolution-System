import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Plus } from 'lucide-react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { API } from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import { useData } from '../../hooks/useData';
import { roles } from '../../utils/roles';
import { EmptyState, ErrorBanner, LoadingState, PageShell, PageTitle } from '../../components/ui';
import IncidentRow from '../../components/IncidentRow';
import SearchFilterBar, { toSearchParams } from '../../components/SearchFilterBar';

const TITLES = { offers: 'Assignment requests', ongoing: 'Ongoing incidents', past: 'Past incidents' };

/** Searchable incident list for every role; the server scopes results to what the user may see. */
export default function IncidentList({ scope, editMode }) {
  const { user } = useAuth();
  const [filters, setFilters] = useState({});
  const [expanded, setExpanded] = useState(null);
  const client = useQueryClient();
  const path = scope === 'offers' ? '/technician/offers' : `/incidents/search?${toSearchParams(filters, scope)}`;
  const q = useData(path, { refetchInterval: 5000, placeholderData: (prev) => prev });
  const action = useMutation({
    mutationFn: ({ id, name, body = {} }) => API.post(`/incidents/${id}/${name}`, body),
    onSuccess: () => { toast.success('Incident updated'); client.invalidateQueries(); },
  });
  const rows = scope === 'offers' ? (q.data || []).filter((i) => !filters.q || `${i.publicId} ${i.title}`.toLowerCase().includes(filters.q.toLowerCase())) : q.data || [];
  const admin = user.role === 'ADMIN';
  return <PageShell>
    <PageTitle eyebrow={`${roles[user.role].label.toUpperCase()} · INCIDENTS`} title={editMode ? 'Edit ongoing incidents' : TITLES[scope]}
      description={scope === 'offers' ? 'Incidents the system has offered to you. Accept to start the response clock, or decline to pass it on.'
        : scope === 'past' ? 'Closed incidents with their resolution and history.' : 'Status, ownership and the next step for each open incident.'}
      action={user.role === 'EMPLOYEE' && <Link to="/employee/create" className="btn primary"><Plus size={16} /> New ticket</Link>} />
    {scope !== 'offers' ? <SearchFilterBar role={user.role} filters={filters} setFilters={setFilters} count={rows.length} />
      : <div className="filter-bar"><div className="search-box"><input placeholder="Search requests" value={filters.q || ''} onChange={(e) => setFilters({ q: e.target.value })} /></div><span className="results-count">{rows.length} results</span></div>}
    {q.isLoading ? <LoadingState text="Loading incidents…" /> : q.isError ? <ErrorBanner />
      : rows.length === 0 ? <EmptyState title={Object.values(filters).some(Boolean) ? 'No matching incidents' : 'Nothing here yet'}
        text={Object.values(filters).some(Boolean) ? 'Try different filters.' : 'Incidents will appear here when there are any.'} />
      : <div className="incident-list">{rows.map((i) => <IncidentRow key={i.publicId} incident={i} user={user} expanded={expanded === i.publicId}
        onToggle={() => setExpanded(expanded === i.publicId ? null : i.publicId)} busy={action.isPending}
        onAction={(name, body) => action.mutate({ id: i.publicId, name, body })} adminControls={admin && (editMode || scope === 'ongoing')} />)}</div>}
  </PageShell>;
}
