// Post-login shell for an authenticated official: a simple tab switcher between Race Control
// (default) and the Check-in Desk. Local useState toggle, same pattern as CheckInDesk's own
// `showReassign` — no router, no external state library.
//
// Also owns the "Close Day" action (U10): a destructive, hard-to-undo action, so it's visually
// separated from the tabs and gated behind a native confirm(). On a successful close the local
// session is cleared and `onDayClosed` is called so the parent (LoginPage) re-renders back to
// its logged-out state — R14 purges cached data including credentials on close, so there is
// nothing left to show here once it succeeds.
import { useEffect, useState } from 'react';
import RaceControl from '@/features/race-control/RaceControl';
import CheckInDesk from '@/features/checkin/CheckInDesk';
import { closeDay, getDayLifecycleStatus } from '@/lib/api';
import { clearSession } from '@/lib/auth';

type Tab = 'race-control' | 'checkin';

// U12: how often to check whether this instance's snapshot sync has been rejected as
// superseded (a device-loss declaration elsewhere has handed the day to a replacement). Slower
// than the boards' 5s live-data poll — this is a status check, not something officials are
// staring at, and superseded is a rare, one-time-per-session transition once it happens.
const SUPERSEDED_POLL_INTERVAL_MS = 30_000;

export interface OfficialShellProps {
  // True once this session has seen a splitBrainWarning from an offline day-open. Sticky for
  // the rest of the session by design (LoginPage never clears it back to false on its own) — it
  // is the entire v1 mitigation for split-brain, so it must stay visible, not dismissible.
  splitBrainWarning?: boolean;
  onDayClosed?: () => void;
}

export default function OfficialShell({
  splitBrainWarning = false,
  onDayClosed = () => {},
}: OfficialShellProps) {
  const [tab, setTab] = useState<Tab>('race-control');

  const [closing, setClosing] = useState(false);
  const [closeError, setCloseError] = useState<string | null>(null);
  const [pendingSyncCount, setPendingSyncCount] = useState<number | null>(null);
  const [superseded, setSuperseded] = useState(false);

  // U12: periodically check whether this instance's snapshot sync has been rejected as
  // superseded — this can happen mid-session (unlike splitBrainWarning, which is only ever known
  // at open time), so a one-time check at login can't catch it. Stops polling once true; there is
  // no un-supersede path to watch for.
  useEffect(() => {
    if (superseded) return;
    let cancelled = false;
    function poll() {
      getDayLifecycleStatus()
        .then((status) => {
          if (!cancelled && status.superseded) setSuperseded(true);
        })
        .catch(() => {
          // Best-effort — leave the last-known state and retry next tick.
        });
    }
    poll();
    const interval = setInterval(poll, SUPERSEDED_POLL_INTERVAL_MS);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, [superseded]);

  async function handleCloseDay() {
    if (closing) return;
    const confirmed = window.confirm(
      'Close the event day? Cached data on this device (including officials\' credentials) will be purged. Only do this once the event is finished. Continue?',
    );
    if (!confirmed) return;

    setClosing(true);
    setCloseError(null);
    setPendingSyncCount(null);
    try {
      const result = await closeDay();
      if (result.status === 'closed') {
        await clearSession();
        onDayClosed();
      } else {
        setPendingSyncCount(result.pendingSyncCount);
      }
    } catch {
      setCloseError('Could not close the day. Check the connection and try again.');
    } finally {
      setClosing(false);
    }
  }

  return (
    <div className="flex min-h-screen flex-col">
      {superseded && (
        <div
          role="alert"
          className="border-b-2 border-red-600 bg-red-50 p-3 text-sm font-semibold text-red-800"
        >
          This device has been superseded — a replacement device has taken over this event day
          (device loss was declared). Everything captured here so far is safe, but this device
          can no longer sync new results to the cloud. Stop using it for race control and confirm
          with an admin.
        </div>
      )}

      {splitBrainWarning && (
        <div
          role="alert"
          className="border-b-2 border-red-600 bg-red-50 p-3 text-sm font-semibold text-red-800"
        >
          Could not confirm exclusive control with the cloud (opened offline). If another device
          also has this event open, results may conflict — only proceed if you're certain no
          other device is running this event day.
        </div>
      )}

      <nav className="flex items-center justify-between gap-2 border-b bg-white p-3">
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => setTab('race-control')}
            aria-current={tab === 'race-control'}
            className={`rounded px-3 py-2 text-sm font-medium ${
              tab === 'race-control' ? 'bg-blue-600 text-white' : 'text-slate-600 hover:bg-slate-100'
            }`}
          >
            Race Control
          </button>
          <button
            type="button"
            onClick={() => setTab('checkin')}
            aria-current={tab === 'checkin'}
            className={`rounded px-3 py-2 text-sm font-medium ${
              tab === 'checkin' ? 'bg-blue-600 text-white' : 'text-slate-600 hover:bg-slate-100'
            }`}
          >
            Check-in Desk
          </button>
        </div>

        <button
          type="button"
          onClick={handleCloseDay}
          disabled={closing}
          className="rounded border border-red-600 px-3 py-2 text-sm font-medium text-red-700 hover:bg-red-50 disabled:opacity-50"
        >
          {closing ? 'Closing…' : 'Close Day'}
        </button>
      </nav>

      {pendingSyncCount !== null && (
        <div className="border-b bg-amber-50 p-3 text-sm text-amber-800">
          Sync is still catching up: {pendingSyncCount} item{pendingSyncCount === 1 ? '' : 's'}{' '}
          still pending. The day has not been closed — try again once sync has caught up.
        </div>
      )}
      {closeError && (
        <div className="border-b bg-red-50 p-3 text-sm text-red-700">{closeError}</div>
      )}

      <div className="flex-1">{tab === 'race-control' ? <RaceControl /> : <CheckInDesk />}</div>
    </div>
  );
}
