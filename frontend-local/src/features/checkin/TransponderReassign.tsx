// Transponder reassignment for an existing entry (R8): find the entry via the shared roster
// search, then submit a new transponder number. 404 (entry not found) and 409 (transponder
// already assigned to someone else) are distinct, specific user-facing messages — never
// collapsed into one generic error.
import { useState, type FormEvent } from 'react';
import RosterSearch from './RosterSearch';
import {
  reassignTransponder,
  type CheckinEntry,
  type ReassignTransponderResponse,
} from '@/lib/api';

type ReassignResult =
  | { kind: 'success'; response: ReassignTransponderResponse }
  | { kind: 'not_found' }
  | { kind: 'conflict' }
  | { kind: 'generic' };

function statusFrom(err: unknown): number | undefined {
  return (err as { response?: { status?: number } })?.response?.status;
}

export default function TransponderReassign() {
  const [selectedEntry, setSelectedEntry] = useState<CheckinEntry | null>(null);
  const [newTransponderNumber, setNewTransponderNumber] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<ReassignResult | null>(null);

  function handleSelect(entry: CheckinEntry) {
    setSelectedEntry(entry);
    setResult(null);
    setNewTransponderNumber('');
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!selectedEntry) return;
    const trimmed = newTransponderNumber.trim();
    if (!trimmed) return;

    setSubmitting(true);
    setResult(null);
    try {
      const response = await reassignTransponder(selectedEntry.cachedEntryId, trimmed);
      setResult({ kind: 'success', response });
      setNewTransponderNumber('');
    } catch (err) {
      const status = statusFrom(err);
      if (status === 404) {
        setResult({ kind: 'not_found' });
      } else if (status === 409) {
        setResult({ kind: 'conflict' });
      } else {
        setResult({ kind: 'generic' });
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <h2 className="text-lg font-semibold">Reassign transponder</h2>

      {!selectedEntry ? (
        <RosterSearch onSelect={handleSelect} />
      ) : (
        <form onSubmit={handleSubmit} className="flex flex-col gap-3">
          <p className="text-sm text-slate-600">
            {selectedEntry.racerName} — currently #{selectedEntry.transponderNumber}
          </p>

          <label className="flex flex-col gap-1">
            <span className="text-sm font-medium">New transponder number</span>
            <input
              type="text"
              className="rounded border px-2 py-1"
              value={newTransponderNumber}
              onChange={(e) => setNewTransponderNumber(e.target.value)}
            />
          </label>

          <div className="flex gap-2">
            <button
              type="submit"
              disabled={submitting}
              className="rounded bg-blue-600 px-3 py-2 text-white disabled:opacity-50"
            >
              {submitting ? 'Reassigning…' : 'Reassign'}
            </button>
            <button
              type="button"
              className="rounded border px-3 py-2"
              onClick={() => {
                setSelectedEntry(null);
                setResult(null);
              }}
            >
              Choose different entry
            </button>
          </div>

          {result?.kind === 'success' && (
            <p className="text-sm font-medium text-green-700">
              Reassigned #{result.response.oldTransponderNumber} → #
              {result.response.newTransponderNumber}
            </p>
          )}
          {result?.kind === 'not_found' && <p className="text-sm text-red-600">Entry not found.</p>}
          {result?.kind === 'conflict' && (
            <p className="text-sm text-red-600">
              That transponder is already assigned to someone else.
            </p>
          )}
          {result?.kind === 'generic' && (
            <p className="text-sm text-red-600">Something went wrong. Please try again.</p>
          )}
        </form>
      )}
    </div>
  );
}
