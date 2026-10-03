// Compact results table shared by NowNextBoard (idle-state "last completed" section) and
// ResultsBoard. Pure presentational component, no API calls.
import type { RaceResultRowDto } from '@/lib/api';

export interface ResultsTableProps {
  results: RaceResultRowDto[];
}

function fmtMs(ms: number | null): string {
  if (ms === null) return '—';
  return `${(ms / 1000).toFixed(3)}s`;
}

export default function ResultsTable({ results }: ResultsTableProps) {
  if (results.length === 0) {
    return <p className="text-sm text-slate-500">No results recorded for this race.</p>;
  }

  return (
    <table className="w-full text-sm">
      <thead>
        <tr className="border-b text-left text-slate-600">
          <th className="py-1 pr-2">Pos</th>
          <th className="py-1 pr-2">Racer</th>
          <th className="py-1 pr-2">Transponder</th>
          <th className="py-1 pr-2">Laps</th>
          <th className="py-1 pr-2">Best lap</th>
        </tr>
      </thead>
      <tbody>
        {results.map((row) => (
          <tr key={row.entryId} className="border-b">
            <td className="py-1 pr-2">{row.position}</td>
            <td className="py-1 pr-2">{row.racerName ?? '—'}</td>
            <td className="py-1 pr-2">{row.transponderNumber ?? '—'}</td>
            <td className="py-1 pr-2">{row.lapsCompleted}</td>
            <td className="py-1 pr-2">{fmtMs(row.bestLapMs)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
