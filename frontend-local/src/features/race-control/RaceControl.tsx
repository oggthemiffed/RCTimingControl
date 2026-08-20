// Race-control root: race list on top, selected race's detail below. No router — race selection
// is local useState, matching this project's established no-router, hooks-only convention.
import { useEffect, useState } from 'react';
import { listRaces, type ScheduleEntryDto } from '@/lib/api';
import RaceList from './RaceList';
import RaceDetail from './RaceDetail';

type ListState =
  | { kind: 'loading' }
  | { kind: 'loaded'; races: ScheduleEntryDto[] }
  | { kind: 'error' };

export default function RaceControl() {
  const [state, setState] = useState<ListState>({ kind: 'loading' });
  const [selectedRaceId, setSelectedRaceId] = useState<number | null>(null);

  useEffect(() => {
    let cancelled = false;
    listRaces()
      .then((races) => {
        if (!cancelled) setState({ kind: 'loaded', races });
      })
      .catch(() => {
        if (!cancelled) setState({ kind: 'error' });
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="mx-auto flex min-h-screen max-w-4xl flex-col gap-6 p-6">
      <h1 className="text-xl font-semibold">Race Control</h1>

      {state.kind === 'loading' && <p className="text-sm text-slate-500">Loading races…</p>}
      {state.kind === 'error' && (
        <p className="text-sm text-red-600">
          Could not load races. Check the connection and retry.
        </p>
      )}

      {state.kind === 'loaded' && (
        <>
          <RaceList races={state.races} selectedRaceId={selectedRaceId} onSelect={setSelectedRaceId} />

          {selectedRaceId !== null && (
            // `key` forces a full remount on race switch, so RaceDetail's own state (grid, live
            // rows, transition/marshal-adjustment UI) starts fresh without needing any
            // reset-on-prop-change effect logic.
            <RaceDetail key={selectedRaceId} raceId={selectedRaceId} races={state.races} />
          )}
        </>
      )}
    </div>
  );
}
