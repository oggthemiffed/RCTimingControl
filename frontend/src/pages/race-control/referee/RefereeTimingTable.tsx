import { useMemo } from 'react';
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
import {
  computeProximityAlerts,
  computeBackmarkers,
  type LiveRacePositionDto,
} from './alerts';

type Props = {
  current: LiveRacePositionDto[];
  previous: LiveRacePositionDto[] | null;
  driverNameByEntryId: Map<number, string>;
};

function formatMs(ms: number | null): string {
  if (ms === null) return '—';
  const secs = Math.floor(ms / 1000);
  const millis = ms % 1000;
  return `${secs}.${String(millis).padStart(3, '0')}`;
}

export function RefereeTimingTable({ current, previous, driverNameByEntryId }: Props) {
  const alerts = useMemo(() => computeProximityAlerts(current, previous), [current, previous]);
  const backmarkers = useMemo(() => computeBackmarkers(current), [current]);

  const sorted = [...current].sort((a, b) => a.position - b.position);

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Pos</TableHead>
          <TableHead>Driver</TableHead>
          <TableHead>Laps</TableHead>
          <TableHead>Last Lap</TableHead>
          <TableHead>Best Lap</TableHead>
          <TableHead>Gap</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {sorted.map((row) => (
          <TableRow
            key={row.entryId}
            className={cn(alerts.has(row.entryId) && 'bg-chart-3/20')}
          >
            <TableCell>{row.position}</TableCell>
            <TableCell>
              <span>{driverNameByEntryId.get(row.entryId) ?? `Entry ${row.entryId}`}</span>
              {backmarkers.has(row.entryId) && (
                <Badge variant="outline" className="border-chart-3 text-chart-3 ml-2">
                  LAPPED
                </Badge>
              )}
            </TableCell>
            <TableCell>{row.lapsCompleted}</TableCell>
            <TableCell>{formatMs(row.lastPassingTimeMs > 0 ? row.lastPassingTimeMs : null)}</TableCell>
            <TableCell>{formatMs(row.bestLapMs)}</TableCell>
            <TableCell>{formatMs(row.gapToLeaderMs)}</TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
