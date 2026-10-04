// Anonymous spectator board for a venue TV (L12, ported from frontend-local): the race on track
// with live timing, what's next, and the last finished race's results when nothing is running.
// Polls the public board endpoints and, while a race is on track, follows its STOMP timing and
// state topics, which anonymous sessions may subscribe to.
import { useEffect, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useStomp } from '@/hooks/race-control/useStomp';
import type { LiveTimingRowDto, RaceStateChangeDto } from '@/lib/raceControlApi';
import { getBoardLiveTiming, getNowNext, getResultsBoard } from '@/lib/boardsApi';
import { BoardShell, BoardMessage } from './BoardShell';
import { BoardLiveTable } from './BoardLiveTable';
import { BoardResultsTable } from './BoardResultsTable';
import { BOARD_POLL_MS, parseEventParam } from './boardFormat';

export default function NowNextBoard() {
  const [searchParams] = useSearchParams();
  const eventId = parseEventParam(searchParams.get('event'));
  const queryClient = useQueryClient();

  const { data: nowNext, isPending } = useQuery({
    queryKey: ['boards', 'now-next', eventId],
    queryFn: () => getNowNext(eventId),
    refetchInterval: BOARD_POLL_MS,
  });

  const currentRace = nowNext?.currentRace ?? null;
  const nextRace = nowNext?.nextRace ?? null;
  const lastCompletedRace = nowNext?.lastCompletedRace ?? null;
  const raceId = currentRace?.raceId ?? null;

  const { data: liveRows } = useStomp<LiveTimingRowDto[]>(
    raceId ? `/topic/race/${raceId}/timing` : null,
  );
  const { data: stateChange } = useStomp<RaceStateChangeDto>(
    raceId ? `/topic/race/${raceId}/state` : null,
  );

  // Seed the table so a board opened mid-race shows the field before the next passing.
  const { data: seedRows } = useQuery({
    queryKey: ['boards', 'live-timing', raceId],
    queryFn: () => getBoardLiveTiming(raceId!),
    enabled: raceId !== null,
  });

  // A stop, resume or finish changes what the board should show, so don't wait for the next poll.
  useEffect(() => {
    if (stateChange) {
      void queryClient.invalidateQueries({ queryKey: ['boards'] });
    }
  }, [stateChange, queryClient]);

  const { data: results } = useQuery({
    queryKey: ['boards', 'results', eventId],
    queryFn: () => getResultsBoard(eventId),
    enabled: !currentRace && lastCompletedRace !== null,
    refetchInterval: BOARD_POLL_MS,
  });

  const rows = useMemo(() => {
    const source = liveRows ?? seedRows ?? [];
    return [...source].sort((a, b) => a.position - b.position);
  }, [liveRows, seedRows]);

  if (isPending) {
    return (
      <BoardShell>
        <BoardMessage>Loading…</BoardMessage>
      </BoardShell>
    );
  }

  if (currentRace) {
    return (
      <BoardShell eventName={nowNext?.eventName}>
        <div className="flex flex-wrap items-baseline justify-between gap-4">
          <h1 className="text-4xl font-bold lg:text-6xl">{currentRace.label}</h1>
          <span
            className={
              currentRace.status === 'STOPPED'
                ? 'rounded bg-red-600 px-4 py-1 text-2xl font-bold lg:text-3xl'
                : 'rounded bg-green-600 px-4 py-1 text-2xl font-bold lg:text-3xl'
            }
          >
            {currentRace.status === 'STOPPED' ? 'Stopped' : 'Racing'}
          </span>
        </div>
        {rows.length > 0 ? (
          <BoardLiveTable rows={rows} />
        ) : (
          <BoardMessage>Waiting for the first lap…</BoardMessage>
        )}
        {nextRace && (
          <div className="mt-auto border-t border-slate-800 pt-6">
            <h2 className="text-xl text-slate-400 lg:text-2xl">Next up</h2>
            <p className="text-3xl font-semibold lg:text-4xl">{nextRace.label}</p>
          </div>
        )}
      </BoardShell>
    );
  }

  if (!nextRace && !lastCompletedRace) {
    return (
      <BoardShell eventName={nowNext?.eventName}>
        <BoardMessage>No race data yet.</BoardMessage>
      </BoardShell>
    );
  }

  return (
    <BoardShell eventName={nowNext?.eventName}>
      {nextRace && (
        <div>
          <h2 className="text-xl text-slate-400 lg:text-2xl">Next up</h2>
          <h1 className="text-4xl font-bold lg:text-6xl">{nextRace.label}</h1>
        </div>
      )}
      {lastCompletedRace && (
        <div className={nextRace ? 'border-t border-slate-800 pt-6' : ''}>
          <h2 className="mb-4 text-2xl text-slate-400 lg:text-3xl">
            Last finished: {lastCompletedRace.label}
          </h2>
          {results?.race?.raceId === lastCompletedRace.raceId ? (
            <BoardResultsTable results={results.results} />
          ) : (
            <p className="text-2xl text-slate-400">Loading results…</p>
          )}
        </div>
      )}
    </BoardShell>
  );
}
