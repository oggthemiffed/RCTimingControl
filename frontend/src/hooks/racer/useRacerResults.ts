import { useQuery } from '@tanstack/react-query';
import { getMyResults, type RacerResultHistoryDto } from '@/lib/racerApi';

export function useRacerResults() {
  return useQuery<RacerResultHistoryDto[]>({
    queryKey: ['racer', 'results'],
    queryFn: getMyResults,
    staleTime: 30_000,
  });
}
