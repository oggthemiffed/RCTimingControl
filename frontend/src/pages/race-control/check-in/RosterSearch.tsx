// Manual roster search (L11, ported from frontend-local), shared by CheckInDesk (fallback for
// unmatched scans, and a lookup path of its own) and TransponderSwap (finding the entry).
import { useEffect, useState } from 'react';
import { Input } from '@/components/ui/input';
import { checkInSearch, type CheckInEntry } from '@/lib/raceControlApi';
import { CheckInBadge } from './CheckInStatus';
import { transponderLabel } from './transponderLabel';

const MIN_QUERY_LENGTH = 2;
const DEBOUNCE_MS = 300;

export interface RosterSearchProps {
  eventId: number;
  initialQuery?: string;
  onSelect: (entry: CheckInEntry) => void;
}

export default function RosterSearch({ eventId, initialQuery = '', onSelect }: RosterSearchProps) {
  // `initialQuery` only seeds the field on mount. CheckInDesk remounts this component (via a
  // `key`) when it wants to hand it a fresh query, e.g. after an unmatched scan.
  const [query, setQuery] = useState(initialQuery);
  const [results, setResults] = useState<CheckInEntry[]>([]);
  const [searching, setSearching] = useState(false);
  const [error, setError] = useState(false);

  const trimmedQuery = query.trim();
  const queryTooShort = trimmedQuery.length < MIN_QUERY_LENGTH;

  useEffect(() => {
    if (queryTooShort) {
      return;
    }

    let cancelled = false;

    const timeoutId = setTimeout(() => {
      if (cancelled) return;
      setSearching(true);
      setError(false);
      checkInSearch(eventId, trimmedQuery)
        .then((data) => {
          if (!cancelled) setResults(data);
        })
        .catch(() => {
          if (!cancelled) setError(true);
        })
        .finally(() => {
          if (!cancelled) setSearching(false);
        });
    }, DEBOUNCE_MS);

    return () => {
      cancelled = true;
      clearTimeout(timeoutId);
    };
  }, [eventId, trimmedQuery, queryTooShort]);

  return (
    <div className="flex flex-col gap-2">
      <label className="flex flex-col gap-1">
        <span className="text-sm font-medium">Search entries by name</span>
        <Input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Competitor name…"
        />
      </label>

      {!queryTooShort && searching && <p className="text-sm text-muted-foreground">Searching…</p>}
      {!queryTooShort && error && (
        <p className="text-sm text-destructive">Search failed. Check the connection and retry.</p>
      )}

      {!queryTooShort && !searching && !error && results.length > 0 && (
        <ul className="flex flex-col gap-1">
          {results.map((entry) => (
            <li key={entry.entryId}>
              <button
                type="button"
                className="flex w-full items-center justify-between gap-2 rounded border px-2 py-1 text-left text-sm hover:bg-muted"
                onClick={() => onSelect(entry)}
              >
                <span>
                  {entry.competitorName} ({entry.className ?? 'no class'}) {transponderLabel(entry)}
                </span>
                <CheckInBadge checkedIn={entry.checkedIn} />
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
