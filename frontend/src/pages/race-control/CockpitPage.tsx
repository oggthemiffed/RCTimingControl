import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { toast } from 'sonner';
import { useRunOrder } from '@/hooks/race-control/useRunOrder';
import { useRaceStateMutations } from '@/hooks/race-control/useRaceStateMutations';
import { RunOrderPanel } from './panels/RunOrderPanel';
import { GridEditorPanel } from './panels/GridEditorPanel';
import { LiveTimingPanel } from './panels/LiveTimingPanel';
import { FinishedPanel } from './panels/FinishedPanel';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import type { RunOrderItemDto } from '@/lib/raceControlApi';

function raceTitle(item: RunOrderItemDto | undefined) {
  if (!item) return 'No race selected';
  const type =
    item.roundType === 'FINAL'
      ? `Final${item.finalLetter ? ` ${item.finalLetter}` : ''}`
      : item.roundType === 'QUALIFIER'
      ? `Qualifier ${item.roundNumber}`
      : `Practice`;
  return `${type} — ${item.className} — Heat ${item.heatNumber}`;
}

export default function CockpitPage() {
  const { eventId: eventIdStr } = useParams<{ eventId: string }>();
  const eventId = Number(eventIdStr);

  const { data: runOrder = [], isLoading } = useRunOrder(eventId || null);
  const [selectedRaceId, setSelectedRaceId] = useState<number | null>(null);

  // Auto-select first non-FINISHED race on load
  useEffect(() => {
    if (runOrder.length > 0 && selectedRaceId === null) {
      const active = runOrder.find((r) => r.status !== 'FINISHED') ?? runOrder[0];
      setSelectedRaceId(active.raceId);
    }
  }, [runOrder, selectedRaceId]);

  const selectedRace = runOrder.find((r) => r.raceId === selectedRaceId);

  const mutations = useRaceStateMutations(selectedRaceId ?? 0, eventId);

  function onCallGrid() {
    mutations.callGrid.mutate(undefined, {
      onError: (e) => toast.error(`Call grid failed: ${(e as Error).message}`),
    });
  }

  function onStart() {
    mutations.start.mutate(undefined, {
      onError: (e) => toast.error(`Start failed: ${(e as Error).message}`),
    });
  }

  function onStop() {
    mutations.stop.mutate(undefined, {
      onError: (e) => toast.error(`Stop failed: ${(e as Error).message}`),
    });
  }

  function onAbandon() {
    if (!confirm('Abandon this race? This cannot be undone.')) return;
    mutations.abandon.mutate(undefined, {
      onError: (e) => toast.error(`Abandon failed: ${(e as Error).message}`),
    });
  }

  function onRestart() {
    if (!confirm('Restart this race from scratch? All timing data will be cleared.')) return;
    mutations.restart.mutate(undefined, {
      onError: (e) => toast.error(`Restart failed: ${(e as Error).message}`),
    });
  }

  function renderMainPanel() {
    if (!selectedRace || isLoading) {
      return (
        <div className="flex items-center justify-center h-48 text-muted-foreground text-sm">
          {isLoading ? 'Loading run order…' : 'Select a race from the list.'}
        </div>
      );
    }

    switch (selectedRace.status) {
      case 'PENDING':
        return (
          <div className="flex flex-col gap-4">
            <h2 className="text-lg font-semibold">{raceTitle(selectedRace)}</h2>
            <p className="text-sm text-muted-foreground">Race is pending. Call the grid when ready.</p>
            <Button
              onClick={onCallGrid}
              disabled={mutations.callGrid.isPending}
              className="w-fit"
            >
              {mutations.callGrid.isPending ? 'Calling grid…' : 'Call Grid'}
            </Button>
          </div>
        );

      case 'GRID':
        return (
          <GridEditorPanel
            raceId={selectedRace.raceId}
            onStart={onStart}
            isStarting={mutations.start.isPending}
          />
        );

      case 'RUNNING':
        return (
          <div className="flex flex-col gap-4">
            <div className="flex items-center gap-3">
              <h2 className="text-lg font-semibold">{raceTitle(selectedRace)}</h2>
              <div className="flex gap-2 ml-auto">
                <Button variant="outline" onClick={onStop} disabled={mutations.stop.isPending}>
                  {mutations.stop.isPending ? 'Stopping…' : 'Stop'}
                </Button>
                <Button variant="outline" onClick={onRestart} disabled={mutations.restart.isPending}>
                  Restart
                </Button>
                <Button variant="destructive" onClick={onAbandon} disabled={mutations.abandon.isPending}>
                  Abandon
                </Button>
              </div>
            </div>
            <LiveTimingPanel raceId={selectedRace.raceId} status={selectedRace.status} />
          </div>
        );

      case 'STOPPED':
        return (
          <div className="flex flex-col gap-4">
            <div className="flex items-center gap-3">
              <h2 className="text-lg font-semibold">{raceTitle(selectedRace)}</h2>
              <div className="flex gap-2 ml-auto">
                <Button onClick={onStart} disabled={mutations.start.isPending}>
                  {mutations.start.isPending ? 'Resuming…' : 'Resume Race'}
                </Button>
                <Button variant="outline" onClick={onRestart} disabled={mutations.restart.isPending}>
                  Restart
                </Button>
                <Button variant="destructive" onClick={onAbandon} disabled={mutations.abandon.isPending}>
                  Abandon
                </Button>
              </div>
            </div>
            <LiveTimingPanel raceId={selectedRace.raceId} status={selectedRace.status} />
          </div>
        );

      case 'FINISHED':
        return (
          <FinishedPanel raceId={selectedRace.raceId} eventId={eventId} onRestart={onRestart} isRestarting={mutations.restart.isPending} />
        );
    }
  }

  return (
    <div className="flex h-full gap-0">
      {/* Run order sidebar */}
      <aside className="w-56 shrink-0 border-r overflow-y-auto">
        <div className="px-4 py-3 border-b">
          <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
            Run Order
          </p>
        </div>
        {isLoading ? (
          <div className="p-4 space-y-2">
            {[...Array(6)].map((_, i) => (
              <div key={i} className="h-8 bg-muted animate-pulse rounded" />
            ))}
          </div>
        ) : (
          <RunOrderPanel
            items={runOrder}
            selectedRaceId={selectedRaceId}
            onSelect={setSelectedRaceId}
          />
        )}
      </aside>

      <Separator orientation="vertical" />

      {/* Main content */}
      <main className="flex-1 overflow-y-auto p-6">
        {renderMainPanel()}
      </main>
    </div>
  );
}
