import { useQuery } from '@tanstack/react-query';
import { getSetupStatus, getSetupProgress } from '@/lib/setupApi';

export function useSetupStatus() {
  return useQuery({
    queryKey: ['setup-status'],
    queryFn: getSetupStatus,
    staleTime: 60_000,
    retry: false,
  });
}

export function useSetupProgress({ enabled = true }: { enabled?: boolean } = {}) {
  return useQuery({
    queryKey: ['setup-progress'],
    queryFn: getSetupProgress,
    staleTime: 0,
    enabled,
    retry: false,
  });
}
