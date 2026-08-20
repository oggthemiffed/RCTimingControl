// Advance-to-next-round action (available once the selected race is STOPPED — see RaceDetail).
// The finishing order must be captured from the *current* live snapshot's row order before the
// race transitions to FINISHED, because the backend releases live position data at that point.
// If the live snapshot is already empty (e.g. the official finished this race in a prior
// session), submitting an empty order would silently corrupt the next race's grid — so that case
// is refused client-side with a clear message instead.
import { useState } from 'react';
import {
  advanceRound,
  type LiveTimingRowDto,
  type ScheduleEntryDetailDto,
  type ScheduleEntryDto,
} from '@/lib/api';
import GridTable from './GridTable';

type AdvanceState =
  | { kind: 'idle' }
  | { kind: 'pending' }
  | { kind: 'success'; detail: ScheduleEntryDetailDto }
  | { kind: 'error' };

export interface AdvanceRoundProps {
  raceId: number;
  races: ScheduleEntryDto[];
  liveRows: LiveTimingRowDto[];
}

export default function AdvanceRound({ raceId, races, liveRows }: AdvanceRoundProps) {
  const candidates = races.filter((r) => r.id !== raceId);
  const [nextScheduleId, setNextScheduleId] = useState<number | ''>('');
  const [state, setState] = useState<AdvanceState>({ kind: 'idle' });

  const noLiveData = liveRows.length === 0;

  async function handleAdvance() {
    if (state.kind === 'pending') return;
    if (nextScheduleId === '') return;
    if (noLiveData) return;

    setState({ kind: 'pending' });
    try {
      const entryIdsInFinishingOrder = liveRows.map((row) => row.entryId);
      const detail = await advanceRound(raceId, nextScheduleId, entryIdsInFinishingOrder);
      setState({ kind: 'success', detail });
    } catch {
      setState({ kind: 'error' });
    }
  }

  return (
    <section className="flex flex-col gap-2 border-t pt-4">
      <h3 className="text-sm font-medium text-slate-600">Advance to next round</h3>

      <label className="flex flex-col gap-1">
        <span className="text-sm font-medium">Next race</span>
        <select
          className="rounded border px-2 py-1"
          value={nextScheduleId}
          onChange={(e) => setNextScheduleId(e.target.value === '' ? '' : Number(e.target.value))}
        >
          <option value="">Select next race…</option>
          {candidates.map((r) => (
            <option key={r.id} value={r.id}>
              Round {r.roundNumber} · Heat {r.heatNumber}
              {r.finalLetter ? ` ${r.finalLetter}` : ''} — {r.className}
            </option>
          ))}
        </select>
      </label>

      <button
        type="button"
        disabled={state.kind === 'pending' || nextScheduleId === '' || noLiveData}
        onClick={handleAdvance}
        className="self-start rounded bg-blue-600 px-3 py-2 text-white disabled:opacity-50"
      >
        {state.kind === 'pending' ? 'Advancing…' : 'Advance to next round'}
      </button>

      {noLiveData && (
        <p className="text-sm text-amber-700">
          No live data available for this race — cannot capture finishing order.
        </p>
      )}

      {state.kind === 'error' && (
        <p className="text-sm text-red-600">Could not advance round. Please try again.</p>
      )}

      {state.kind === 'success' && (
        <div className="flex flex-col gap-2">
          <p className="text-sm font-medium text-green-700">Advanced — next race grid set.</p>
          <GridTable grid={state.detail.grid} />
        </div>
      )}
    </section>
  );
}
