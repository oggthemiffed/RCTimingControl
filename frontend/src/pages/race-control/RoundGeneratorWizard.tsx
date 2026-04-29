import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { adminApi, type EventClassDto, type BumpUpConfig } from '@/lib/adminApi';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  eventId: number;
};

type ClassRow = {
  eventClassId: number;
  label: string;
  finalsCount: number;
  carsPerFinal: number;
  bumpCount: number;
};

function defaultsFromConfig(cls: EventClassDto): { finalsCount: number; carsPerFinal: number; bumpCount: number } {
  const cfg = cls.configSnapshot;
  if (cfg.type === 'BUMP_UP') {
    const bump = cfg as BumpUpConfig;
    return { finalsCount: 2, carsPerFinal: bump.gridSize ?? 10, bumpCount: bump.bumpSpots ?? 2 };
  }
  return { finalsCount: 2, carsPerFinal: 10, bumpCount: 2 };
}

export function RoundGeneratorWizard({ open, onOpenChange, eventId }: Props) {
  const queryClient = useQueryClient();

  const [practiceRounds, setPracticeRounds] = useState(2);
  const [qualifyingRounds, setQualifyingRounds] = useState(3);
  const [maxCarsPerHeat, setMaxCarsPerHeat] = useState(10);
  const [classRows, setClassRows] = useState<ClassRow[]>([]);
  const [initialised, setInitialised] = useState(false);

  const { data: eventClasses = [] } = useQuery({
    queryKey: ['adminApi', 'eventClasses', eventId],
    queryFn: () => adminApi.listEventClasses(eventId),
    enabled: open && eventId > 0,
  });

  const { data: racingClasses = [] } = useQuery({
    queryKey: ['adminApi', 'racingClasses'],
    queryFn: () => adminApi.listRacingClasses(),
    enabled: open,
  });

  // Initialise classRows once we have event classes
  if (open && eventClasses.length > 0 && !initialised) {
    setClassRows(
      eventClasses.map((cls) => {
        const racingClass = racingClasses.find((rc) => rc.id === cls.racingClassId);
        const defaults = defaultsFromConfig(cls);
        return {
          eventClassId: cls.id,
          label: racingClass?.name ?? `Class ${cls.id}`,
          ...defaults,
        };
      }),
    );
    setInitialised(true);
  }

  const generate = useMutation({
    mutationFn: () =>
      adminApi.generateRounds(eventId, {
        practiceRoundsCount: practiceRounds,
        qualifyingRoundsCount: qualifyingRounds,
        maxCarsPerHeat,
        classFinalsConfigs: classRows.map((r) => ({
          eventClassId: r.eventClassId,
          finalsCount: r.finalsCount,
          carsPerFinal: r.carsPerFinal,
          bumpCount: r.bumpCount,
        })),
      }),
    onSuccess: () => {
      toast.success('Rounds generated successfully');
      queryClient.invalidateQueries({ queryKey: ['runOrder'] });
      onOpenChange(false);
      setInitialised(false);
    },
    onError: () => toast.error('Failed to generate rounds'),
  });

  function updateClassRow(idx: number, field: keyof Omit<ClassRow, 'eventClassId' | 'label'>, value: number) {
    setClassRows((prev) => prev.map((r, i) => (i === idx ? { ...r, [field]: value } : r)));
  }

  return (
    <Dialog open={open} onOpenChange={(v) => { onOpenChange(v); if (!v) setInitialised(false); }}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Generate Rounds</DialogTitle>
          <DialogDescription>
            Configure practice, qualifying and finals structure for this event. Each class can have independent finals settings.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-2">
          {/* Global settings */}
          <div className="grid grid-cols-3 gap-3">
            <div className="space-y-1">
              <Label className="text-xs">Practice rounds</Label>
              <Input
                type="number"
                min={0}
                max={10}
                value={practiceRounds}
                onChange={(e) => setPracticeRounds(Number(e.target.value))}
              />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">Qualifying rounds</Label>
              <Input
                type="number"
                min={1}
                max={10}
                value={qualifyingRounds}
                onChange={(e) => setQualifyingRounds(Number(e.target.value))}
              />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">Max cars/heat</Label>
              <Input
                type="number"
                min={1}
                max={64}
                value={maxCarsPerHeat}
                onChange={(e) => setMaxCarsPerHeat(Number(e.target.value))}
              />
            </div>
          </div>

          {/* Per-class finals config */}
          {classRows.length > 0 && (
            <div className="space-y-2">
              <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Finals per class</p>
              {classRows.map((row, idx) => (
                <div key={row.eventClassId} className="rounded border p-3 space-y-2">
                  <p className="text-sm font-medium">{row.label}</p>
                  <div className="grid grid-cols-3 gap-2">
                    <div className="space-y-1">
                      <Label className="text-xs">Finals</Label>
                      <Input
                        type="number"
                        min={1}
                        max={3}
                        value={row.finalsCount}
                        onChange={(e) => updateClassRow(idx, 'finalsCount', Number(e.target.value))}
                      />
                    </div>
                    <div className="space-y-1">
                      <Label className="text-xs">Cars/final</Label>
                      <Input
                        type="number"
                        min={1}
                        max={64}
                        value={row.carsPerFinal}
                        onChange={(e) => updateClassRow(idx, 'carsPerFinal', Number(e.target.value))}
                      />
                    </div>
                    <div className="space-y-1">
                      <Label className="text-xs">Bump spots</Label>
                      <Input
                        type="number"
                        min={1}
                        max={10}
                        value={row.bumpCount}
                        onChange={(e) => updateClassRow(idx, 'bumpCount', Number(e.target.value))}
                      />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button onClick={() => generate.mutate()} disabled={generate.isPending}>
            {generate.isPending ? 'Generating…' : 'Generate Rounds'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
