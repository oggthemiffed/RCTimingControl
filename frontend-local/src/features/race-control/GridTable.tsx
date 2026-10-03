// Starting-grid table for a race: gridPosition/racerName/carNumber/transponderNumber per entry.
// A bump slot that hasn't been filled yet comes back from the backend with cachedEntryId (and
// the other entry fields) as null — rendered as an empty slot, not a crash. Marshal +1/-1
// controls are only wired up when the caller supplies `raceId` + `onMarshalAdjusted` (the
// currently active race's own grid); a grid rendered elsewhere, e.g. the "next race" preview
// after Advance to next round, is read-only.
import type { GridEntryDto } from '@/lib/api';
import MarshalButtons from './MarshalButtons';

export interface GridTableProps {
  grid: GridEntryDto[];
  raceId?: number;
  onMarshalAdjusted?: (cachedEntryId: number, lapDelta: 1 | -1) => void;
}

export default function GridTable({ grid, raceId, onMarshalAdjusted }: GridTableProps) {
  if (grid.length === 0) {
    return <p className="text-sm text-slate-500">No grid set for this race yet.</p>;
  }

  const showMarshalColumn = raceId !== undefined && onMarshalAdjusted !== undefined;

  return (
    <table className="w-full text-sm">
      <thead>
        <tr className="border-b text-left text-slate-600">
          <th className="py-1 pr-2">Pos</th>
          <th className="py-1 pr-2">Racer</th>
          <th className="py-1 pr-2">Car</th>
          <th className="py-1 pr-2">Transponder</th>
          {showMarshalColumn && <th className="py-1 pr-2">Marshal</th>}
        </tr>
      </thead>
      <tbody>
        {grid.map((entry) => (
          <tr key={entry.gridPosition} className="border-b">
            <td className="py-1 pr-2">{entry.gridPosition}</td>
            <td className="py-1 pr-2">
              {entry.racerName ?? <span className="text-slate-400">— empty —</span>}
            </td>
            <td className="py-1 pr-2">{entry.carNumber ?? '—'}</td>
            <td className="py-1 pr-2">{entry.transponderNumber ?? '—'}</td>
            {showMarshalColumn && (
              <td className="py-1 pr-2">
                {entry.cachedEntryId !== null && (
                  <MarshalButtons
                    raceId={raceId as number}
                    cachedEntryId={entry.cachedEntryId}
                    onAdjusted={(lapDelta) =>
                      onMarshalAdjusted?.(entry.cachedEntryId as number, lapDelta)
                    }
                  />
                )}
              </td>
            )}
          </tr>
        ))}
      </tbody>
    </table>
  );
}
