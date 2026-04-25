import { useState, useRef, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { toast } from 'sonner';
import { useRunOrder } from '@/hooks/race-control/useRunOrder';
import { useRaceStateMutations } from '@/hooks/race-control/useRaceStateMutations';
import { useStomp } from '@/hooks/race-control/useStomp';
import { RefereeTimingTable } from './referee/RefereeTimingTable';
import { IncidentDialog } from './dialogs/IncidentDialog';
import { PenaltyDialog } from './dialogs/PenaltyDialog';
import { RunOrderPanel } from './panels/RunOrderPanel';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import type { LiveTimingRowDto, IncidentReportRequest, PenaltyRequest } from '@/lib/raceControlApi';
import type { LiveRacePositionDto } from './referee/alerts';

function toPositionDto(row: LiveTimingRowDto): LiveRacePositionDto {
  return {
    entryId: row.entryId,
    position: row.position,
    lapsCompleted: row.lapsCompleted,
    lastPassingTimeMs: row.lastPassingTimeMs,
    bestLapMs: row.bestLapMs,
    gapToLeaderMs: row.gapToLeaderMs,
    gapToAheadMs: row.gapToAheadMs,
  };
}

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

  const topic = selectedRaceId ? `/topic/race/${selectedRaceId}/timing` : null;
  const { data: timingRows } = useStomp<LiveTimingRowDto[]>(topic);

  const current = (timingRows ?? []).map(toPositionDto);
  const previousRef = useRef<LiveRacePositionDto[] | null>(null);
  const previous = previousRef.current;
  if (current.length > 0) previousRef.current = current;

  const driverNameByEntryId = new Map<number, string>();

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

        {current.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            {selectedRaceId ? 'Waiting for timing data…' : 'Select a race from the run order.'}
          </p>
        ) : (
          <RefereeTimingTable
            current={current}
            previous={previous}
            driverNameByEntryId={driverNameByEntryId}
          />
        )}
      </main>

      <IncidentDialog
        open={incidentOpen}
        onOpenChange={setIncidentOpen}
        onSubmit={onIncident}
        isPending={mutations.incident.isPending}
      />
      <PenaltyDialog
        open={penaltyOpen}
        onOpenChange={setPenaltyOpen}
        onSubmit={onPenalty}
        isPending={mutations.penalty.isPending}
      />
    </div>
  );
}
