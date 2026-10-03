import { useQuery } from '@tanstack/react-query';
import { adminApi } from '@/lib/adminApi';
import { adminQueryKeys } from './adminQueryKeys';

export function useAdminCompetitorsList() {
  return useQuery({
    queryKey: adminQueryKeys.competitors.all(),
    queryFn: adminApi.competitors.list,
  });
}
