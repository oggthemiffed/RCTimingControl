import { useQuery } from '@tanstack/react-query';
import { getRunOrder, type RunOrderItemDto } from '@/lib/raceControlApi';
import { raceControlQueryKeys } from './raceControlQueryKeys';

export function useRunOrder(eventId: number | null) {
  return useQuery<RunOrderItemDto[]>({
    queryKey: raceControlQueryKeys.runOrder(eventId ?? -1),
    queryFn: () => getRunOrder(eventId!),
    enabled: eventId !== null,
    staleTime: 10_000,
    refetchInterval: 15_000,
  });
}
