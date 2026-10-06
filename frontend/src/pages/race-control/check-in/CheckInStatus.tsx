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

/**
 * The imported file's transponder numbers where they differ from a swap made on the day (#50). The swap is kept;
 * this only flags the difference.
 */
export function ImportedTransponderDifference({
  primary,
  secondary,
}: {
  primary: string | null;
  secondary: string | null;
}) {
  if (!primary && !secondary) return null;
  const numbers = [primary && `transponder ${primary}`, secondary && `secondary ${secondary}`].filter(Boolean);
  return (
    <span className="block text-xs text-amber-700 dark:text-amber-400" data-testid="imported-transponder-difference">
      Booking has {numbers.join(' and ')}. Keeping the number swapped on the day.
    </span>
  );
}
