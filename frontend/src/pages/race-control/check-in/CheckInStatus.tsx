// Check-in state for one entry, with RaceHub's arrival mark shown read-only beside it (L11).
// Check-in here is authoritative on the day; the RaceHub mark is only a hint.
import { Badge } from '@/components/ui/badge';
import type { CheckInEntry } from '@/lib/raceControlApi';

export function CheckInBadge({ checkedIn }: { checkedIn: boolean }) {
  return checkedIn ? (
    <Badge variant="secondary">Checked in</Badge>
  ) : (
    <Badge variant="destructive">Not checked in</Badge>
  );
}

export function RaceHubArrival({ arrival }: { arrival: CheckInEntry['racehubArrival'] }) {
  if (!arrival) return null;
  return (
    <span className="text-xs text-muted-foreground">
      RaceHub: {arrival === 'ARRIVED' ? 'arrived' : 'not arrived'}
    </span>
  );
}
