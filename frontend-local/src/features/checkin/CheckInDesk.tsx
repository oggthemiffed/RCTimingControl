// Check-in desk (R5, R8): camera scan, keyboard-wedge scan, and manual roster search all
// resolve to the same entry-confirmation flow. Camera and keyboard-wedge are deliberately two
// distinct entry points (see BarcodeScanner / KeyboardWedgeInput) even though both end up
// calling checkinResolve — a regression in one must not be able to hide behind the other.
import { useCallback, useState } from 'react';
import BarcodeScanner from './BarcodeScanner';
import KeyboardWedgeInput from './KeyboardWedgeInput';
import RosterSearch from './RosterSearch';
import TransponderReassign from './TransponderReassign';
import {
  checkinConfirm,
  checkinResolve,
  type CheckinConfirmResponse,
  type CheckinEntry,
} from '@/lib/api';

type ResolveState =
  | { kind: 'idle' }
  | { kind: 'resolved'; entry: CheckinEntry }
  | { kind: 'not_found'; query: string }
  | { kind: 'error' };

function statusFrom(err: unknown): number | undefined {
  return (err as { response?: { status?: number } })?.response?.status;
}

export default function CheckInDesk() {
  const [cameraAvailable, setCameraAvailable] = useState(true);
  const [resolveState, setResolveState] = useState<ResolveState>({ kind: 'idle' });
  const [confirmResult, setConfirmResult] = useState<CheckinConfirmResponse | null>(null);
  const [confirming, setConfirming] = useState(false);
  const [showReassign, setShowReassign] = useState(false);

  const resolveCode = useCallback(async (code: string) => {
    setConfirmResult(null);
    try {
      const entry = await checkinResolve(code);
      setResolveState({ kind: 'resolved', entry });
    } catch (err) {
      if (statusFrom(err) === 404) {
        setResolveState({ kind: 'not_found', query: code });
      } else {
        setResolveState({ kind: 'error' });
      }
    }
  }, []);

  // Two separate wired callbacks — one per scan entry point — even though they share the
  // resolveCode implementation underneath.
  const handleCameraDecode = useCallback(
    (code: string) => {
      void resolveCode(code);
    },
    [resolveCode],
  );
  const handleWedgeScan = useCallback(
    (code: string) => {
      void resolveCode(code);
    },
    [resolveCode],
  );
  const handleCameraUnavailable = useCallback(() => setCameraAvailable(false), []);

  function handleSearchSelect(entry: CheckinEntry) {
    setConfirmResult(null);
    setResolveState({ kind: 'resolved', entry });
  }

  async function handleConfirm(cachedEntryId: number) {
    setConfirming(true);
    try {
      const result = await checkinConfirm(cachedEntryId);
      setConfirmResult(result);
    } catch {
      setResolveState({ kind: 'error' });
    } finally {
      setConfirming(false);
    }
  }

  return (
    <div className="mx-auto flex min-h-screen max-w-2xl flex-col gap-6 p-6">
      <h1 className="text-xl font-semibold">Check-in Desk</h1>

      <section className="flex flex-col gap-2">
        <h2 className="text-sm font-medium text-slate-600">Camera scan</h2>
        {cameraAvailable ? (
          <BarcodeScanner onDecode={handleCameraDecode} onUnavailable={handleCameraUnavailable} />
        ) : (
          <p className="text-sm text-amber-700">
            Camera unavailable — use keyboard scanner or search below.
          </p>
        )}
      </section>

      <section className="flex flex-col gap-2">
        <h2 className="text-sm font-medium text-slate-600">Keyboard-wedge scanner</h2>
        <KeyboardWedgeInput onScan={handleWedgeScan} />
      </section>

      {resolveState.kind === 'resolved' && (
        <ResolvedEntryPanel
          entry={resolveState.entry}
          confirmResult={confirmResult}
          confirming={confirming}
          onConfirm={() => handleConfirm(resolveState.entry.cachedEntryId)}
        />
      )}

      {resolveState.kind === 'error' && (
        <p className="text-sm text-red-600">
          Something went wrong. Please try again or search below.
        </p>
      )}

      <section className="flex flex-col gap-2 border-t pt-4">
        <h2 className="text-sm font-medium text-slate-600">Manual roster search</h2>
        {resolveState.kind === 'not_found' && (
          <p className="text-sm text-amber-700">
            No match for “{resolveState.query}”. Search the roster below.
          </p>
        )}
        <RosterSearch
          key={resolveState.kind === 'not_found' ? resolveState.query : ''}
          initialQuery={resolveState.kind === 'not_found' ? resolveState.query : ''}
          onSelect={handleSearchSelect}
        />
      </section>

      <section className="border-t pt-4">
        <button
          type="button"
          className="text-sm font-medium text-blue-700 underline"
          onClick={() => setShowReassign((v) => !v)}
        >
          {showReassign ? 'Hide transponder reassignment' : 'Reassign a transponder'}
        </button>
        {showReassign && (
          <div className="mt-3">
            <TransponderReassign />
          </div>
        )}
      </section>
    </div>
  );
}

function ResolvedEntryPanel({
  entry,
  confirmResult,
  confirming,
  onConfirm,
}: {
  entry: CheckinEntry;
  confirmResult: CheckinConfirmResponse | null;
  confirming: boolean;
  onConfirm: () => void;
}) {
  return (
    <section className="flex flex-col gap-2 rounded border p-4">
      <p className="font-medium">{entry.racerName}</p>
      <p className="text-sm text-slate-600">
        {entry.carName} — {entry.className} — #{entry.transponderNumber}
      </p>

      {confirmResult ? (
        confirmResult.alreadyCheckedIn ? (
          <p className="text-sm font-medium text-amber-700">
            Already checked in at {confirmResult.checkedInAt}
          </p>
        ) : (
          <p className="text-sm font-medium text-green-700">Checked in!</p>
        )
      ) : (
        <button
          type="button"
          disabled={confirming}
          onClick={onConfirm}
          className="self-start rounded bg-blue-600 px-3 py-2 text-white disabled:opacity-50"
        >
          {confirming ? 'Confirming…' : 'Confirm check-in'}
        </button>
      )}
    </section>
  );
}
