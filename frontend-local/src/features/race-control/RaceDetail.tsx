// Detail view for the currently selected race: its grid, live position table, state-transition
// controls, and (once STOPPED) the advance-to-next-round action. Live rows are sourced from the
// STOMP channel once connected, falling back to a one-off getLiveSnapshot() fetch for the initial
// paint before the first STOMP message arrives; a marshal adjustment applies an optimistic local
// update to lapsCompleted, which the next STOMP `/timing` broadcast then supersedes wholesale.
//
// The parent (RaceControl) mounts this with `key={raceId}`, so a race switch is a full remount —
// state here never needs to be reset in response to a changing `raceId` prop.
import { useCallback, useEffect, useRef, useState } from 'react';
import {
  getRaceDetail,
  getLiveSnapshot,
  type LiveTimingRowDto,
  type ScheduleEntryDetailDto,
  type ScheduleEntryDto,
} from '@/lib/api';
import { useRaceChannel } from '@/lib/stomp';
import GridTable from './GridTable';
import LiveTimingTable from './LiveTimingTable';
import TransitionControls from './TransitionControls';
import AdvanceRound from './AdvanceRound';

type DetailState =
  | { kind: 'loading' }
  | { kind: 'loaded'; detail: ScheduleEntryDetailDto }
  | { kind: 'not_found' }
  | { kind: 'error' };

export interface RaceDetailProps {
  raceId: number;
  races: ScheduleEntryDto[];
}

export default function RaceDetail({ raceId, races }: RaceDetailProps) {
  const [state, setState] = useState<DetailState>({ kind: 'loading' });
  const [liveRows, setLiveRows] = useState<LiveTimingRowDto[]>([]);
  const stompRowsArrived = useRef(false);

  const channel = useRaceChannel(raceId);

  useEffect(() => {
    let cancelled = false;
    getRaceDetail(raceId)
      .then((detail) => {
        if (!cancelled) setState({ kind: 'loaded', detail });
      })
      .catch((err) => {
        if (cancelled) return;
        const status = (err as { response?: { status?: number } })?.response?.status;
        setState(status === 404 ? { kind: 'not_found' } : { kind: 'error' });
      });
    return () => {
      cancelled = true;
    };
  }, [raceId]);

  // Used to re-sync after a rejected transition (see TransitionControls). Deliberately does not
  // flip `state` back to `{kind:'loading'}` first — this runs from a click handler, well after
  // the initial load, and dropping to the top-level loading screen would unmount
  // TransitionControls (and the inline error it's showing) out from under the official.
  const refetchAfterFailedTransition = useCallback(() => {
    getRaceDetail(raceId)
      .then((detail) => setState({ kind: 'loaded', detail }))
      .catch((err) => {
        const status = (err as { response?: { status?: number } })?.response?.status;
        setState(status === 404 ? { kind: 'not_found' } : { kind: 'error' });
      });
  }, [raceId]);

  // Initial paint fallback: only wins if no STOMP timing message has arrived yet for this race.
  useEffect(() => {
    let cancelled = false;
    getLiveSnapshot(raceId)
      .then((snapshot) => {
        if (!cancelled && !stompRowsArrived.current) setLiveRows(snapshot.rows);
      })
      .catch(() => {
        // Best-effort initial paint only — STOMP will catch up once connected.
      });
    return () => {
      cancelled = true;
    };
  }, [raceId]);

  // Sync local liveRows whenever a new STOMP timing broadcast arrives. Done synchronously during
  // render (React's documented "adjusting state when a prop changes" pattern) rather than in an
  // effect body, since an unconditional setState at the top of an effect trips the
  // set-state-in-effect lint rule and this is exactly the "derive state from a changed value"
  // case that pattern is for.
  const [syncedRows, setSyncedRows] = useState(channel.rows);
  if (channel.rows !== syncedRows) {
    setSyncedRows(channel.rows);
    if (channel.rows) {
      setLiveRows(channel.rows);
    }
  }

  // Refs may only be written from effects/handlers, not during render (see the block above) —
  // this effect exists solely to record "a STOMP message has arrived" for the initial-paint
  // fallback effect to check.
  useEffect(() => {
    if (channel.rows) {
      stompRowsArrived.current = true;
    }
  }, [channel.rows]);

  function handleTransitioned(updated: ScheduleEntryDto) {
    setState((prev) =>
      prev.kind === 'loaded' ? { kind: 'loaded', detail: { ...prev.detail, race: updated } } : prev,
    );
  }

  // Sync race status from a STOMP /state broadcast — another official's transition (Stop, Finish,
  // etc.) must be reflected here too, not just this browser's own transitionRace() calls. Same
  // render-time sync pattern as channel.rows above, for the same lint-rule reason.
  const [syncedStateEvent, setSyncedStateEvent] = useState(channel.stateEvent);
  if (channel.stateEvent !== syncedStateEvent) {
    setSyncedStateEvent(channel.stateEvent);
    if (channel.stateEvent) {
      const newStatus = channel.stateEvent.newStatus as ScheduleEntryDto['status'];
      setState((prev) =>
        prev.kind === 'loaded'
          ? { kind: 'loaded', detail: { ...prev.detail, race: { ...prev.detail.race, status: newStatus } } }
          : prev,
      );
    }
  }

  // Re-sorts by lapsCompleted DESC (mirroring the backend's LiveRaceState.calculatePositions
  // ordering) and renumbers `position` so array order stays the true finishing order — AdvanceRound
  // reads rows in array order to build entryIdsInFinishingOrder, so a stale order here would seed
  // the next round's grid backwards from the actual result until the next STOMP /timing broadcast
  // arrives to correct it.
  function handleMarshalAdjusted(cachedEntryId: number, lapDelta: 1 | -1) {
    setLiveRows((rows) => {
      const adjusted = rows.map((row) =>
        row.entryId === cachedEntryId ? { ...row, lapsCompleted: row.lapsCompleted + lapDelta } : row,
      );
      adjusted.sort((a, b) => b.lapsCompleted - a.lapsCompleted);
      return adjusted.map((row, i) => ({ ...row, position: i + 1 }));
    });
  }

  if (state.kind === 'loading') {
    return <p className="text-sm text-slate-500">Loading race…</p>;
  }
  if (state.kind === 'not_found') {
    return <p className="text-sm text-red-600">Race not found.</p>;
  }
  if (state.kind === 'error') {
    return <p className="text-sm text-red-600">Could not load race. Please try again.</p>;
  }

  const { race, grid } = state.detail;

  return (
    <div className="flex flex-col gap-4 border-t pt-4">
      <div>
        <h2 className="text-lg font-semibold">
          Round {race.roundNumber} · Heat {race.heatNumber}
          {race.finalLetter ? ` ${race.finalLetter}` : ''} — {race.className}
        </h2>
        <p className="text-sm text-slate-600">Status: {race.status}</p>
      </div>

      <TransitionControls
        race={race}
        onTransitioned={handleTransitioned}
        onTransitionFailed={refetchAfterFailedTransition}
      />

      <div className="flex flex-col gap-2">
        <h3 className="text-sm font-medium text-slate-600">Grid</h3>
        <GridTable grid={grid} raceId={race.id} onMarshalAdjusted={handleMarshalAdjusted} />
      </div>

      <LiveTimingTable rows={liveRows} connected={channel.connected} />

      {race.status === 'STOPPED' && (
        <AdvanceRound raceId={race.id} races={races} liveRows={liveRows} />
      )}
    </div>
  );
}
