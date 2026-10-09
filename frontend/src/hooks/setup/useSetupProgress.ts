import { useQuery } from '@tanstack/react-query';
import { getSetupStatus, getSetupProgress } from '@/lib/setupApi';
import { setupQueryKeys } from './setupQueryKeys';

export function useSetupStatus() {
  return useQuery({
    queryKey: setupQueryKeys.status(),
    queryFn: getSetupStatus,
    staleTime: 60_000,
    retry: false,
  });
}

export function useSetupProgress({ enabled = true }: { enabled?: boolean } = {}) {
  return useQuery({
    queryKey: setupQueryKeys.progress(),
    queryFn: getSetupProgress,
    staleTime: 0,
    enabled,
    retry: false,
  });
}
