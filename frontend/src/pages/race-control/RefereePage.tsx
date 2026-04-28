import { useState, useRef, useEffect, useMemo } from 'react';
import { useParams } from 'react-router-dom';
import { toast } from 'sonner';
import { useRunOrder } from '@/hooks/race-control/useRunOrder';
import { useRaceStateMutations } from '@/hooks/race-control/useRaceStateMutations';
import { useLiveTiming } from '@/hooks/race-control/useLiveTiming';
import { LiveTimingPanel } from './panels/LiveTimingPanel';
import { IncidentDialog } from './dialogs/IncidentDialog';
import { PenaltyDialog } from './dialogs/PenaltyDialog';
import { RunOrderPanel } from './panels/RunOrderPanel';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { computeProximityAlerts } from './referee/alerts';
import type { LiveTimingRowDto, IncidentReportRequest, PenaltyRequest } from '@/lib/raceControlApi';

export default function RefereePage() {
  const { eventId: eventIdStr } = useParams<{ eventId: string }>();
  const eventId = Number(eventIdStr);

  const { data: runOrder = [] } = useRunOrder(eventId || null);
  const [selectedRaceId, setSelectedRaceId] = useState<number | null>(null);
  const [incidentOpen, setIncidentOpen] = useState(false);
  const [penaltyOpen, setPenaltyOpen] = useState(false);

  useEffect(() => {
    if (runOrder.length > 0 && selectedRaceId === null) {
      const active =
        runOrder.find((r) => r.status === 'RUNNING' || r.status === 'STOPPED') ??
        runOrder.find((r) => r.status !== 'FINISHED') ??
        runOrder[0];
      setSelectedRaceId(active.raceId);
    }
  }, [runOrder, selectedRaceId]);

  const { rows: current } = useLiveTiming(selectedRaceId);

  // Track previous rows for proximity alert calculation (closing gaps between updates)
  const previousRef = useRef<LiveTimingRowDto[]>([]);
  const highlightEntryIds = useMemo(
    () => computeProximityAlerts(current, previousRef.current.length > 0 ? previousRef.current : null),
    [current],
  );
  useEffect(() => {
    if (current.length > 0) previousRef.current = current;
  }, [current]);

  // Reset previous when race changes so stale highlights don't bleed across races
  useEffect(() => {
    previousRef.current = [];
  }, [selectedRaceId]);

  const selectedRace = runOrder.find((r) => r.raceId === selectedRaceId);
  const mutations = useRaceStateMutations(selectedRaceId ?? 0, eventId);

  function onIncident(req: IncidentReportRequest) {
    mutations.incident.mutate(req, {
      onSuccess: () => { toast.success('Incident report raised'); setIncidentOpen(false); },
      onError: (e) => toast.error(`Failed: ${(e as Error).message}`),
    });
  }

  function onPenalty(req: PenaltyRequest) {
    mutations.penalty.mutate(req, {
      onSuccess: () => { toast.success('Penalty applied'); setPenaltyOpen(false); },
      onError: (e) => toast.error(`Failed: ${(e as Error).message}`),
    });
  }

  return (
    <div className="flex h-full">
      {/* Run order sidebar */}
      <aside className="w-56 shrink-0 border-r overflow-y-auto">
        <div className="px-4 py-3 border-b">
          <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
            Run Order
          </p>
        </div>
        <RunOrderPanel
          items={runOrder}
          selectedRaceId={selectedRaceId}
          onSelect={setSelectedRaceId}
        />
      </aside>

      <Separator orientation="vertical" />

      {/* Main content */}
      <main className="flex-1 overflow-y-auto p-6 flex flex-col gap-4">
        <div className="flex items-center gap-3">
          <h1 className="text-lg font-semibold">Referee View</h1>
          <div className="ml-auto flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setIncidentOpen(true)}
              disabled={!selectedRaceId}
            >
              Raise Incident
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setPenaltyOpen(true)}
              disabled={!selectedRaceId}
            >
              Apply Penalty
            </Button>
          </div>
        </div>

        {!selectedRaceId ? (
          <p className="text-sm text-muted-foreground">Select a race from the run order.</p>
        ) : (
          <LiveTimingPanel
            raceId={selectedRaceId}
            status={selectedRace?.status ?? 'PENDING'}
            highlightEntryIds={highlightEntryIds}
          />
        )}
      </main>

      <IncidentDialog
        open={incidentOpen}
        onOpenChange={setIncidentOpen}
        onSubmit={onIncident}
        isPending={mutations.incident.isPending}
        drivers={current.map((r) => ({ entryId: r.entryId, driverName: r.driverName }))}
      />
      <PenaltyDialog
        open={penaltyOpen}
        onOpenChange={setPenaltyOpen}
        onSubmit={onPenalty}
        isPending={mutations.penalty.isPending}
        drivers={current.map((r) => ({ entryId: r.entryId, driverName: r.driverName }))}
      />
    </div>
  );
}
