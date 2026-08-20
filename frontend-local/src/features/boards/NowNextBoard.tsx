// Anonymous, read-only spectator board: current heat (live), next-up schedule, and last-completed
// results. No auth — polls the boards-only `/api/v1/boards/now-next` (and, in idle states,
// `/api/v1/boards/results`) endpoints and, while a race is running, subscribes to the same STOMP
// `/timing` channel the officials' live table uses via the shared `useRaceChannel` hook.
//
// Unlike the officials' RaceDetail, there is no REST fallback for the initial paint of a running
// race here (no getLiveSnapshot() equivalent is exposed anonymously) — a board's live table can be
// briefly empty right after a race starts, until the first STOMP message arrives. That's expected
// for a kiosk display, not a bug to work around.
import { useEffect, useState } from 'react';
import { getNowNext, getResults, type NowNextDto, type ResultsDto } from '@/lib/api';
import { useRaceChannel } from '@/lib/stomp';
import LiveTimingTable from '@/features/race-control/LiveTimingTable';
import ResultsTable from './ResultsTable';
import { formatRaceLabel } from './format';

const POLL_INTERVAL_MS = 5000;

export default function NowNextBoard() {
  const [nowNext, setNowNext] = useState<NowNextDto | null>(null);
  const [results, setResults] = useState<ResultsDto | null>(null);

  useEffect(() => {
    let cancelled = false;
    function poll() {
      getNowNext()
        .then((data) => {
          if (!cancelled) setNowNext(data);
        })
        .catch(() => {
          // Best-effort kiosk polling — leave whatever was last shown on screen and retry next tick.
        });
    }
    poll();
    const interval = setInterval(poll, POLL_INTERVAL_MS);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, []);

  const currentRace = nowNext?.currentRace ?? null;
  const nextRace = nowNext?.nextRace ?? null;
  const lastCompletedRace = nowNext?.lastCompletedRace ?? null;

  const channel = useRaceChannel(currentRace ? currentRace.id : null);

  // Only relevant in the idle states (no currentRace). Polled on the same cadence as now-next so a
  // board parked on "after last race" picks up a newly finished heat without a page reload.
  useEffect(() => {
    if (currentRace || !lastCompletedRace) {
      return;
    }
    let cancelled = false;
    function poll() {
      getResults()
        .then((data) => {
          if (!cancelled) setResults(data);
        })
        .catch(() => {
          // Best-effort — leave prior results showing.
        });
    }
    poll();
    const interval = setInterval(poll, POLL_INTERVAL_MS);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
    // currentRace/lastCompletedRace are plain DTOs re-fetched wholesale each poll tick; comparing
    // by id keeps this effect from tearing down/restarting its own interval every 5s just because
    // the outer poll produced a new (but equal) object.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentRace?.id, lastCompletedRace?.id]);

  if (currentRace) {
    return (
      <div className="flex min-h-screen flex-col gap-6 p-8">
        <div>
          <h1 className="text-3xl font-semibold">{formatRaceLabel(currentRace)}</h1>
          <p className="text-lg text-slate-600">Status: {currentRace.status}</p>
        </div>

        <LiveTimingTable rows={channel.rows ?? []} connected={channel.connected} />

        {nextRace && (
          <div className="border-t pt-4">
            <h2 className="text-sm font-medium text-slate-600">Next up</h2>
            <p className="text-base">{formatRaceLabel(nextRace)}</p>
          </div>
        )}
      </div>
    );
  }

  if (!nextRace && !lastCompletedRace) {
    return (
      <div className="flex min-h-screen items-center justify-center p-8">
        <p className="text-xl text-slate-500">No race data yet.</p>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen flex-col gap-6 p-8">
      {nextRace && (
        <div>
          <h2 className="text-sm font-medium text-slate-600">Next up</h2>
          <h1 className="text-3xl font-semibold">{formatRaceLabel(nextRace)}</h1>
        </div>
      )}

      {lastCompletedRace && (
        <div className={nextRace ? 'border-t pt-4' : ''}>
          <h2 className="text-sm font-medium text-slate-600">
            Last completed — {formatRaceLabel(lastCompletedRace)}
          </h2>
          {results && results.race ? (
            <ResultsTable results={results.results} />
          ) : (
            <p className="text-sm text-slate-500">Loading results…</p>
          )}
        </div>
      )}
    </div>
  );
}
