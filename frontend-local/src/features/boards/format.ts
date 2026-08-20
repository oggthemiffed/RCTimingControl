// Shared race-identity label, matching the display convention already used in
// features/race-control/RaceDetail.tsx's heading.
import type { ScheduleEntryDto } from '@/lib/api';

export function formatRaceLabel(race: ScheduleEntryDto): string {
  return `Round ${race.roundNumber} · Heat ${race.heatNumber}${
    race.finalLetter ? ` ${race.finalLetter}` : ''
  } — ${race.className}`;
}
