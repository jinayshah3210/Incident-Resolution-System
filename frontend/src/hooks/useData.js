import { useQuery } from '@tanstack/react-query';
import { API } from '../api/client';

/** GET helper with a query key equal to the path, so invalidation by path works everywhere. */
export function useData(path, options = {}) {
  return useQuery({ queryKey: [path], queryFn: async () => (await API.get(path)).data, ...options });
}
