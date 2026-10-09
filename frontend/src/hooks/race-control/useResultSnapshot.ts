import { useQuery } from '@tanstack/react-query';
import { getPublicResultSnapshot, type ResultSnapshotDto } from '@/lib/raceControlApi';
import { raceControlQueryKeys } from './raceControlQueryKeys';

export function useResultSnapshot(raceId: number | null) {
  return useQuery<ResultSnapshotDto>({
    queryKey: raceControlQueryKeys.resultSnapshot(raceId ?? -1),
    queryFn: () => getPublicResultSnapshot(raceId!),
    enabled: raceId !== null,
    staleTime: 30_000,
  });
}
