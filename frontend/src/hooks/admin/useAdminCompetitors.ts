import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '@/lib/adminApi';
import { adminQueryKeys } from './adminQueryKeys';

export function useAdminCompetitorsList() {
  return useQuery({
    queryKey: adminQueryKeys.competitors.all(),
    queryFn: adminApi.competitors.list,
  });
}

/** Sets or clears how a competitor's name is said aloud, then refreshes the list (#119). */
export function useSetSpokenName() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, spokenName }: { id: number; spokenName: string }) =>
      adminApi.competitors.setSpokenName(id, spokenName),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: adminQueryKeys.competitors.all() }),
  });
}
