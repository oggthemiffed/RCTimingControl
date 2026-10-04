// Check-in desk (L11, ported from frontend-local): camera scan, keyboard-wedge scan and manual
// search all lead to the same confirmation step. Camera and keyboard-wedge are deliberately two
// separate entry points (see BarcodeScanner / KeyboardWedgeInput) even though both call
// checkInResolve, so a regression in one can't hide behind the other.
import { useCallback, useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { raceControlQueryKeys } from '@/hooks/race-control/raceControlQueryKeys';
import {
  checkInConfirm,
  checkInResolve,
  type CheckInConfirmResponse,
  type CheckInEntry,
} from '@/lib/raceControlApi';
import BarcodeScanner from './BarcodeScanner';
import { CheckInBadge, RaceHubArrival } from './CheckInStatus';
import { transponderLabel } from './transponderLabel';
import KeyboardWedgeInput from './KeyboardWedgeInput';
import RosterSearch from './RosterSearch';

type ResolveState =
  | { kind: 'idle' }
  | { kind: 'resolving' }
  | { kind: 'resolved'; entries: CheckInEntry[] }
  | { kind: 'not_found'; query: string }
  | { kind: 'error' };

function statusFrom(err: unknown): number | undefined {
  return (err as { response?: { status?: number } })?.response?.status;
}

export default function CheckInDesk({ eventId }: { eventId: number }) {
  const queryClient = useQueryClient();
  const [cameraAvailable, setCameraAvailable] = useState(true);
  const [resolveState, setResolveState] = useState<ResolveState>({
    kind: 'idle',
  });
  // Confirm results per entry, so a competitor with two entries can check in to each
  const [confirmed, setConfirmed] = useState<Record<number, CheckInConfirmResponse>>({});
  const [confirmingId, setConfirmingId] = useState<number | null>(null);
  const [confirmError, setConfirmError] = useState<string | null>(null);
  // Only the newest scan or search may update the desk: a slower earlier response is dropped
  const latestLookup = useRef(0);

  const resolveCode = useCallback(
    async (code: string) => {
      const lookup = ++latestLookup.current;
      // Clear the previous competitor at once so they can't be confirmed by mistake
      setResolveState({ kind: 'resolving' });
      setConfirmed({});
      setConfirmError(null);
      try {
        const entries = await checkInResolve(eventId, code);
        if (lookup !== latestLookup.current) return;
        setResolveState({ kind: 'resolved', entries });
      } catch (err) {
        if (lookup !== latestLookup.current) return;
        if (statusFrom(err) === 404) {
          setResolveState({ kind: 'not_found', query: code });
        } else {
          setResolveState({ kind: 'error' });
        }
      }
    },
    [eventId],
  );

  // Two separate callbacks, one per scan entry point, sharing resolveCode underneath.
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

  function handleSearchSelect(entry: CheckInEntry) {
    latestLookup.current++;
    setConfirmed({});
    setConfirmError(null);
    setResolveState({ kind: 'resolved', entries: [entry] });
  }

  async function handleConfirm(entryId: number) {
    setConfirmingId(entryId);
    setConfirmError(null);
    try {
      const result = await checkInConfirm(eventId, entryId);
      setConfirmed((prev) => ({ ...prev, [entryId]: result }));
      void queryClient.invalidateQueries({
        queryKey: raceControlQueryKeys.preRaceReadinessAll,
      });
    } catch (err) {
      setConfirmError(
        statusFrom(err) === 409
          ? 'That entry was withdrawn, so it cannot check in.'
          : 'Check-in failed. Please try again.',
      );
    } finally {
      setConfirmingId(null);
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <section className="flex flex-col gap-2">
        <h2 className="text-sm font-medium text-muted-foreground">Camera scan</h2>
        {cameraAvailable ? (
          <BarcodeScanner onDecode={handleCameraDecode} onUnavailable={handleCameraUnavailable} />
        ) : (
          <p className="text-sm text-amber-700">
            Camera unavailable. Use a keyboard scanner or search below.
          </p>
        )}
      </section>

      <section className="flex flex-col gap-2">
        <h2 className="text-sm font-medium text-muted-foreground">Keyboard-wedge scanner</h2>
        <KeyboardWedgeInput onScan={handleWedgeScan} />
      </section>

      {resolveState.kind === 'resolving' && (
        <p className="text-sm text-muted-foreground">Looking up…</p>
      )}

      {resolveState.kind === 'resolved' &&
        resolveState.entries.map((entry) => (
          <ResolvedEntryPanel
            key={entry.entryId}
            entry={entry}
            confirmResult={confirmed[entry.entryId] ?? null}
            confirming={confirmingId === entry.entryId}
            onConfirm={() => handleConfirm(entry.entryId)}
          />
        ))}

      {confirmError && <p className="text-sm text-destructive">{confirmError}</p>}

      {resolveState.kind === 'error' && (
        <p className="text-sm text-destructive">
          Something went wrong. Please try again or search below.
        </p>
      )}

      <section className="flex flex-col gap-2 border-t pt-4">
        <h2 className="text-sm font-medium text-muted-foreground">Manual search</h2>
        {resolveState.kind === 'not_found' && (
          <p className="text-sm text-amber-700">
            No match for “{resolveState.query}”. Search the entries below.
          </p>
        )}
        <RosterSearch
          key={resolveState.kind === 'not_found' ? resolveState.query : ''}
          eventId={eventId}
          initialQuery={resolveState.kind === 'not_found' ? resolveState.query : ''}
          onSelect={handleSearchSelect}
        />
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
  entry: CheckInEntry;
  confirmResult: CheckInConfirmResponse | null;
  confirming: boolean;
  onConfirm: () => void;
}) {
  const shown = confirmResult?.entry ?? entry;
  return (
    <Card className="flex flex-col gap-2 p-4">
      <div className="flex items-center justify-between gap-2">
        <p className="font-medium">{shown.competitorName}</p>
        <CheckInBadge checkedIn={shown.checkedIn} />
      </div>
      <p className="text-sm text-muted-foreground">
        {shown.className ?? 'No class'} {transponderLabel(shown)}
      </p>
      <RaceHubArrival arrival={shown.racehubArrival} />

      {confirmResult ? (
        confirmResult.alreadyCheckedIn ? (
          <p className="text-sm font-medium text-amber-700">
            Already checked in at {formatTime(confirmResult.entry.checkedInAt)}
          </p>
        ) : (
          <p className="text-sm font-medium text-green-700">Checked in!</p>
        )
      ) : (
        <Button className="self-start" disabled={confirming} onClick={onConfirm}>
          {confirming ? 'Confirming…' : 'Confirm check-in'}
        </Button>
      )}
    </Card>
  );
}

function formatTime(iso: string | null): string {
  if (!iso) return '';
  return new Date(iso).toLocaleTimeString([], {
    hour: '2-digit',
    minute: '2-digit',
  });
}
