import { useQuery } from '@tanstack/react-query';
import { useStomp } from '@/hooks/race-control/useStomp';
import { getLiveTimingSnapshot } from '@/lib/raceControlApi';
import type { LiveTimingRowDto, RunOrderItemDto } from '@/lib/raceControlApi';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';

type Props = {
  raceId: number;
  status: RunOrderItemDto['status'];
};

function fmtMs(ms: number | null): string {
  if (ms === null || ms <= 0) return '—';
  const s = Math.floor(ms / 1000);
  const m = Math.floor(s / 60);
  const rem = ms % 60000;
  if (m > 0) return `${m}:${String(Math.floor(rem / 1000)).padStart(2, '0')}.${String(rem % 1000).padStart(3, '0')}`;
  return `${Math.floor(ms / 1000)}.${String(ms % 1000).padStart(3, '0')}`;
}

export function LiveTimingPanel({ raceId, status }: Props) {
  const topic = raceId ? `/topic/race/${raceId}/timing` : null;
  const { data: stompRows, status: wsStatus } = useStomp<LiveTimingRowDto[]>(topic);

  // Fetch snapshot on mount so navigating away and back restores accumulated laps.
  // staleTime:0 (default) ensures a remount always refetches the latest server-side positions.
  const { data: snapshot } = useQuery({
    queryKey: ['live-timing-snapshot', raceId],
    queryFn: () => getLiveTimingSnapshot(raceId),
    enabled: raceId > 0,
  });

  // STOMP push is authoritative once connected; snapshot fills the gap on remount
  const rows = stompRows ?? snapshot ?? null;

  const sorted = rows ? [...rows].sort((a, b) => a.position - b.position) : [];

  const leaderLaps = sorted.length > 0 ? sorted[0].lapsCompleted : 0;

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center gap-3">
        <h2 className="text-lg font-semibold">
          {status === 'STOPPED' ? 'Race Stopped' : 'Live Timing'}
        </h2>
        <div
          className={cn(
            'h-2 w-2 rounded-full',
            wsStatus === 'connected' ? 'bg-green-500 animate-pulse' : 'bg-muted-foreground/40',
          )}
          title={wsStatus}
        />
        <span className="text-xs text-muted-foreground capitalize">{wsStatus}</span>
      </div>

      {sorted.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          {wsStatus === 'connected' ? 'Waiting for first passing…' : 'Connecting to timing…'}
        </p>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-10">Pos</TableHead>
              <TableHead>Driver</TableHead>
              <TableHead className="text-right">Laps</TableHead>
              <TableHead className="text-right">Last Lap</TableHead>
              <TableHead className="text-right">Best Lap</TableHead>
              <TableHead className="text-right">Gap</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {sorted.map((row) => {
              const isLapped = row.lapsCompleted < leaderLaps;
              return (
                <TableRow key={row.entryId}>
                  <TableCell className="font-mono font-semibold">{row.position}</TableCell>
                  <TableCell>
                    <span>{row.driverName}</span>
                    {isLapped && (
                      <Badge variant="outline" className="ml-2 text-[10px] px-1 py-0 border-chart-3 text-chart-3">
                        LAPPED
                      </Badge>
                    )}
                  </TableCell>
                  <TableCell className="text-right font-mono">{row.lapsCompleted}</TableCell>
                  <TableCell className="text-right font-mono">{fmtMs(row.lastPassingTimeMs)}</TableCell>
                  <TableCell className="text-right font-mono">{fmtMs(row.bestLapMs)}</TableCell>
                  <TableCell className="text-right font-mono text-muted-foreground">
                    {row.position === 1 ? '—' : fmtMs(row.gapToLeaderMs)}
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      )}
    </div>
  );
}
