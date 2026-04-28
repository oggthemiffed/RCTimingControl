import { useQuery } from '@tanstack/react-query';
import { useStomp } from './useStomp';
import { getLiveTimingSnapshot } from '@/lib/raceControlApi';
import type { LiveTimingRowDto } from '@/lib/raceControlApi';

/**
 * Subscribes to live timing for a race via STOMP and seeds from a REST snapshot
 * on mount so navigating away and back restores accumulated laps.
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
  const sorted = rows ? [...rows].sort((a, b) => a.position - b.position) : [];

  return { rows: sorted, wsStatus };
}
