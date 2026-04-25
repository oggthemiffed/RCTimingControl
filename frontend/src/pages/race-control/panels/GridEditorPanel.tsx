import { PreRaceReadinessPanel } from './PreRaceReadinessPanel';
import { Button } from '@/components/ui/button';

type Props = {
  raceId: number;
  onStart: () => void;
  isStarting: boolean;
};

export function GridEditorPanel({ raceId, onStart, isStarting }: Props) {
  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">Grid Called</h2>
        <Button onClick={onStart} disabled={isStarting} className="min-w-24">
          {isStarting ? 'Starting…' : 'Start Race'}
        </Button>
      </div>
      <PreRaceReadinessPanel raceId={raceId} />
    </div>
  );
}
