import React, { createContext, useContext, useState } from 'react';
import { API, session } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(session.user);
  const login = (u, token) => { session.save(u, token); setUser(u); };
  const logout = async () => {
    try { await API.post('/auth/logout'); } catch { /* token may already be invalid */ }
    session.clear();
    setUser(null);
    window.location.href = '/';
  };
  return <AuthContext.Provider value={{ user, login, logout }}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
