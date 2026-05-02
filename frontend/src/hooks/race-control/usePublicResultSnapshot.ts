import { useQuery } from '@tanstack/react-query';
import { getPublicResultSnapshot } from '@/lib/raceControlApi';
import type { ResultSnapshotDto } from '@/lib/raceControlApi';

export function usePublicResultSnapshot(raceId: number | null) {
  return useQuery<ResultSnapshotDto>({
    queryKey: ['public', 'results', raceId ?? -1],
    queryFn: () => getPublicResultSnapshot(raceId!),
    enabled: raceId !== null,
    staleTime: 60_000,
  });
}
