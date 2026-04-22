import { useQuery } from '@tanstack/react-query';
import { adminApi } from '@/lib/adminApi';
import { adminQueryKeys } from './adminQueryKeys';

export function useAdminUsersList() {
  return useQuery({
    queryKey: adminQueryKeys.users.all(),
    queryFn: adminApi.users.list,
  });
}
