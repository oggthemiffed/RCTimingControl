// Presentational race picker: round/heat/class/final-letter and current status per race,
// ordered as returned by listRaces() (already sequence-ascending from the backend).
import type { ScheduleEntryDto } from '@/lib/api';

export interface RaceListProps {
  races: ScheduleEntryDto[];
  selectedRaceId: number | null;
  onSelect: (raceId: number) => void;
}

export default function RaceList({ races, selectedRaceId, onSelect }: RaceListProps) {
  if (races.length === 0) {
    return <p className="text-sm text-slate-500">No races scheduled.</p>;
  }

  return (
    <ul className="flex flex-col gap-1">
      {races.map((race) => (
        <li key={race.id}>
          <button
            type="button"
            onClick={() => onSelect(race.id)}
            className={`flex w-full items-center justify-between rounded border px-3 py-2 text-left hover:bg-slate-50 ${
              race.id === selectedRaceId ? 'border-blue-600 bg-blue-50' : ''
            }`}
          >
            <span className="font-medium">
              Round {race.roundNumber} · Heat {race.heatNumber}
              {race.finalLetter ? ` ${race.finalLetter}` : ''} — {race.className}
            </span>
            <span className="text-xs font-medium uppercase text-slate-500">{race.status}</span>
          </button>
        </li>
      ))}
    </ul>
  );
}
