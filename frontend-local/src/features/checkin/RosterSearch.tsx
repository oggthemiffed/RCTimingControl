// Manual roster search, shared by CheckInDesk (fallback for unmatched scans, and as a primary
// lookup path) and TransponderReassign (finding the entry to reassign).
import { useEffect, useState } from 'react';
import { checkinSearch, type CheckinEntry } from '@/lib/api';

const MIN_QUERY_LENGTH = 2;
const DEBOUNCE_MS = 300;

export interface RosterSearchProps {
  initialQuery?: string;
  onSelect: (entry: CheckinEntry) => void;
}

export default function RosterSearch({ initialQuery = '', onSelect }: RosterSearchProps) {
  // `initialQuery` only needs to seed the field on mount — CheckInDesk remounts this component
  // (via a `key`) whenever it wants to hand it a fresh pre-filled query, e.g. after an
  // unmatched scan, so there's no need to sync it back in with an effect.
  const [query, setQuery] = useState(initialQuery);
  const [results, setResults] = useState<CheckinEntry[]>([]);
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
      checkinSearch(trimmedQuery)
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
  }, [trimmedQuery, queryTooShort]);

  return (
    <div className="flex flex-col gap-2">
      <label className="flex flex-col gap-1">
        <span className="text-sm font-medium">Search roster by name</span>
        <input
          type="text"
          className="rounded border px-2 py-1"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Racer name…"
        />
      </label>

      {!queryTooShort && searching && <p className="text-sm text-slate-500">Searching…</p>}
      {!queryTooShort && error && (
        <p className="text-sm text-red-600">Search failed. Check the connection and retry.</p>
      )}

      {!queryTooShort && !searching && !error && results.length > 0 && (
        <ul className="flex flex-col gap-1">
          {results.map((entry) => (
            <li key={entry.cachedEntryId}>
              <button
                type="button"
                className="w-full rounded border px-2 py-1 text-left hover:bg-slate-50"
                onClick={() => onSelect(entry)}
              >
                {entry.racerName} — {entry.carName} ({entry.className}) — #
                {entry.transponderNumber}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
