import React, { useEffect, useRef } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Send } from 'lucide-react';
import { API } from '../api/client';
import { useData } from '../hooks/useData';

/** Minimal incident conversation: plain bubbles, muted sender/time line, input pinned to the bottom. */
export default function ChatThread({ incident, user, text, setText }) {
  const path = `/incidents/${incident.publicId}/chat`;
  const q = useData(path, { refetchInterval: 5000 });
  const client = useQueryClient();
  const list = useRef(null);
  const send = useMutation({ mutationFn: (content) => API.post(path, { content }), onSuccess: () => { setText(''); client.invalidateQueries({ queryKey: [path] }); } });
  useEffect(() => { if (list.current) list.current.scrollTop = list.current.scrollHeight; }, [q.data?.length]);
  const closed = incident.status === 'CLOSED';
  return <section className="chat">
    <header className="chat-header">Conversation</header>
    <div className="chat-list" ref={list}>
      {q.isLoading ? <p className="chat-hint">Loading…</p> : q.isError ? <p className="chat-hint">The conversation could not be loaded.</p>
        : !q.data?.length ? <p className="chat-hint">No messages yet.</p>
        : q.data.map((m) => {
          const mine = m.senderId === user.id;
          return <div key={m.id || m.at} className={`chat-row ${mine ? 'mine' : ''}`}>
            <div className="chat-bubble">{m.content}</div>
            <span className="chat-meta">{mine ? 'You' : m.senderName} · {new Date(m.at).toLocaleString('en-GB', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' })}</span>
          </div>;
        })}
    </div>
    {closed ? <p className="chat-hint chat-closed">This incident is closed; the conversation is read-only.</p>
      : <form className="chat-input" onSubmit={(e) => { e.preventDefault(); if (text.trim()) send.mutate(text.trim()); }}>
        <input id={`chat-input-${incident.publicId}`} value={text} maxLength={2000} onChange={(e) => setText(e.target.value)} placeholder="Write a message" aria-label="Message" />
        <button className="icon-send" disabled={!text.trim() || send.isPending} aria-label="Send message"><Send size={16} /></button>
      </form>}
  </section>;
}
