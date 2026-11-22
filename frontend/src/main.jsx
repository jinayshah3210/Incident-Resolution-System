import React from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';
import { AuthProvider, useAuth } from './context/AuthContext';
import ErrorBoundary from './components/ErrorBoundary';
import Landing from './pages/auth/Landing';
import Login from './pages/auth/Login';
import Workspace from './layouts/Workspace';
import { roles } from './utils/roles';
import './styles.css';
import './resolveit-overrides.css';
import './theme.css';

const queryClient = new QueryClient({ defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false } } });

function AppRoutes() {
  const { user } = useAuth();
  return <Routes>
    <Route path="/" element={user ? <Navigate to={roles[user.role]?.path || '/'} replace /> : <Landing />} />
    <Route path="/login/:role" element={<Login />} />
    <Route path="*" element={user ? <Workspace /> : <Navigate to="/" replace />} />
  </Routes>;
}

createRoot(document.getElementById('root')).render(
  <ErrorBoundary>
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Toaster position="top-right" toastOptions={{ duration: 4000 }} />
          <AppRoutes />
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  </ErrorBoundary>,
);
