import axios from 'axios';
import toast from 'react-hot-toast';

const TOKEN_KEY = 'resolveit.token';
const USER_KEY = 'resolveit.user';

export const session = {
  token: () => sessionStorage.getItem(TOKEN_KEY),
  user: () => { try { return JSON.parse(sessionStorage.getItem(USER_KEY)); } catch { return null; } },
  save: (user, token) => { sessionStorage.setItem(USER_KEY, JSON.stringify(user)); sessionStorage.setItem(TOKEN_KEY, token); },
  clear: () => { sessionStorage.removeItem(TOKEN_KEY); sessionStorage.removeItem(USER_KEY); },
};

/** One Axios instance: adds the bearer token and turns every ApiError into a toast. */
export const API = axios.create({ baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api' });

API.interceptors.request.use((config) => {
  const token = session.token();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

API.interceptors.response.use((response) => response, (error) => {
  const status = error.response?.status;
  const data = error.response?.data;
  const quiet = error.config?.quiet || (status === 409 && data?.code === 'DUPLICATE_INCIDENT');
  if (!error.response) {
    toast.error('Cannot reach the ResolveIT server. Check that the backend is running.', { id: 'network' });
  } else if (!quiet) {
    toast.error(data?.message || 'We could not complete that request.', { id: `${status}-${data?.code}` });
  }
  if (status === 401 && session.token() && !error.config?.url?.includes('/auth/login')) {
    session.clear();
    window.location.href = '/';
  }
  return Promise.reject(error);
});
