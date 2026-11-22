import React, { useEffect, useRef, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Bell, X } from 'lucide-react';
import { API } from '../api/client';
import { useData } from '../hooks/useData';
import { dateTime } from '../utils/format';

/** Bell with unread badge and a dropdown panel: sort, unread-first, remove one (X) or clear all. */
export default function NotificationPanel() {
  const [open, setOpen] = useState(false);
  const [sort, setSort] = useState('newest');
  const [unreadFirst, setUnreadFirst] = useState(true);
  const ref = useRef(null);
  const client = useQueryClient();
  const listPath = `/notifications?sort=${sort}&unreadFirst=${unreadFirst}`;
  const list = useData(listPath, { refetchInterval: 5000 });
  const count = useData('/notifications/unread-count', { refetchInterval: 5000 });
  const refresh = () => client.invalidateQueries({ predicate: (q) => String(q.queryKey[0]).startsWith('/notifications') });
  const markRead = useMutation({ mutationFn: () => API.post('/notifications/mark-read'), onSuccess: refresh });
  const remove = useMutation({ mutationFn: (id) => API.delete(`/notifications/${id}`), onSuccess: refresh });
  const clear = useMutation({ mutationFn: () => API.delete('/notifications'), onSuccess: refresh });
  useEffect(() => {
    const close = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); };
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);
  const unread = count.data?.count || 0;
  const toggle = () => { const next = !open; setOpen(next); if (!next && unread) markRead.mutate(); };
  return <div className="notify-wrap" ref={ref}>
    <button className="icon-btn notification-btn" onClick={toggle} aria-label={`Notifications, ${unread} unread`} aria-expanded={open}>
      <Bell size={18} />{unread > 0 && <span className="notify-count">{unread > 99 ? '99+' : unread}</span>}
    </button>
    {open && <div className="notify-panel" role="dialog" aria-label="Notifications">
      <div className="notify-head"><b>Notifications</b>
        <div className="notify-tools">
          <select value={sort} onChange={(e) => setSort(e.target.value)} aria-label="Sort notifications"><option value="newest">Newest</option><option value="oldest">Oldest</option></select>
          <label><input type="checkbox" checked={unreadFirst} onChange={(e) => setUnreadFirst(e.target.checked)} /> Unread first</label>
        </div>
      </div>
      <div className="notify-list">
        {list.isLoading ? <p className="notify-empty">Loading…</p> : list.isError ? <p className="notify-empty">Notifications could not be loaded.</p>
          : !list.data?.length ? <p className="notify-empty">You are all caught up.</p>
          : list.data.map((n) => <div key={n.id} className={`notify-item ${n.read ? '' : 'unread'}`}>
            <div><b>{n.title}</b><p>{n.message}</p><small>{dateTime(n.createdAt)}{n.incidentId ? ` · ${n.incidentId}` : ''}</small></div>
            <button className="notify-remove" onClick={() => remove.mutate(n.id)} aria-label="Remove notification"><X size={14} /></button>
          </div>)}
      </div>
      {!!list.data?.length && <div className="notify-foot">
        <button onClick={() => markRead.mutate()}>Mark all read</button>
        <button onClick={() => window.confirm('Remove all notifications?') && clear.mutate()}>Clear all</button>
      </div>}
    </div>}
  </div>;
}
