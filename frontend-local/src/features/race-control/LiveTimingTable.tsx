// Read-only live position table, sourced from the STOMP `/timing` channel (with a
// getLiveSnapshot() fallback for the initial paint owned by the caller). `connected` surfaces
// the STOMP channel's connect/disconnect state so the official sees a "reconnecting" indicator
// on drop rather than a silently stale board.
import type { LiveTimingRowDto } from '@/lib/api';

export interface LiveTimingTableProps {
  rows: LiveTimingRowDto[];
  connected: boolean;
}

function fmtMs(ms: number | null): string {
  if (ms === null) return '—';
  return `${(ms / 1000).toFixed(3)}s`;
}

export default function LiveTimingTable({ rows, connected }: LiveTimingTableProps) {
  return (
    <div className="flex flex-col gap-2">
      <div className="flex items-center gap-2">
        <h3 className="text-sm font-medium text-slate-600">Live timing</h3>
        {!connected && (
          <span className="text-xs font-medium text-amber-700">Reconnecting to local server…</span>
        )}
      </div>

      {rows.length === 0 ? (
        <p className="text-sm text-slate-500">No live timing data yet.</p>
      ) : (
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b text-left text-slate-600">
              <th className="py-1 pr-2">Pos</th>
              <th className="py-1 pr-2">Driver</th>
              <th className="py-1 pr-2">Laps</th>
              <th className="py-1 pr-2">Last lap</th>
              <th className="py-1 pr-2">Best lap</th>
              <th className="py-1 pr-2">Gap</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.entryId} className="border-b">
                <td className="py-1 pr-2">{row.position}</td>
                <td className="py-1 pr-2">{row.driverName}</td>
                <td className="py-1 pr-2">{row.lapsCompleted}</td>
                <td className="py-1 pr-2">{fmtMs(row.lastLapMs)}</td>
                <td className="py-1 pr-2">{fmtMs(row.bestLapMs)}</td>
                <td className="py-1 pr-2">{fmtMs(row.gapToLeaderMs)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
