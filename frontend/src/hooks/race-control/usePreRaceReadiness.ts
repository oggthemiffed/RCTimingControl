import { useQuery } from '@tanstack/react-query';
import { getPreRaceReadiness, type PreRaceReadinessDto } from '@/lib/raceControlApi';
import { raceControlQueryKeys } from './raceControlQueryKeys';

export function usePreRaceReadiness(raceId: number | null) {
  return useQuery<PreRaceReadinessDto>({
    queryKey: raceControlQueryKeys.preRaceReadiness(raceId ?? -1),
    queryFn: () => getPreRaceReadiness(raceId!),
    enabled: raceId !== null,
    staleTime: 5_000,
  });
}
