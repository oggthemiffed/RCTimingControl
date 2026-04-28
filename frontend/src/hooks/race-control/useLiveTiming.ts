import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useStomp } from './useStomp';
import { getLiveTimingSnapshot } from '@/lib/raceControlApi';
import type { LiveTimingRowDto } from '@/lib/raceControlApi';

/**
 * Subscribes to live timing for a race via STOMP and seeds from a REST snapshot
 * on mount so navigating away and back restores accumulated laps.
 *
 * The sorted array is memoised so its reference only changes when the underlying
 * data changes — prevents infinite loops in downstream hooks (useLappedBadge,
 * useLapFlash) that use the array as a useEffect dependency.
 */
export function useLiveTiming(raceId: number | null) {
  const topic = raceId ? `/topic/race/${raceId}/timing` : null;
  const { data: stompRows, status: wsStatus } = useStomp<LiveTimingRowDto[]>(topic);

  const { data: snapshot } = useQuery({
    queryKey: ['live-timing-snapshot', raceId],
    queryFn: () => getLiveTimingSnapshot(raceId!),
    enabled: (raceId ?? 0) > 0,
  });

  const rows = stompRows ?? snapshot ?? null;

  const sorted = useMemo(
    () => (rows ? [...rows].sort((a, b) => a.position - b.position) : []),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [rows],
  );

  return { rows: sorted, wsStatus };
}
