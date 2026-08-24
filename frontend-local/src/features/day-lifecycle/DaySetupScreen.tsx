// Shown by LoginPage before there is anything meaningful to log into: the local instance has
// either never been pre-cached for an event (NOT_SET_UP) or has been pre-cached but the day
// hasn't been opened yet (PRE_CACHED — officials/PINs only become available to log in with once
// the day is OPEN, see LoginPage). Three real-world moments this screen has to handle:
//   1. Never pre-cached, online: pre-cache ahead of time, or open straight away.
//   2. Already pre-cached, online: same form, "Open Day" is the primary action.
//   3. Already pre-cached, offline: a credential-free "open offline" action using whatever was
//      already pre-cached. We can't reliably detect "offline" client-side, so this is always
//      offered — the request itself is what succeeds or fails.
import { useState } from 'react';
import {
  openDay,
  preCacheDay,
  type DayLifecycleStatus,
} from '@/lib/api';

type SetupError =
  | { kind: 'invalid_credentials' }
  | { kind: 'unreachable' }
  | { kind: 'conflict'; message: string }
  | { kind: 'generic' };

function classifySetupError(err: unknown): SetupError {
  const response = (
    err as { response?: { status?: number; data?: { error?: string; message?: string } } }
  ).response;
  if (!response) {
    return { kind: 'unreachable' };
  }
  if (response.status === 401) {
    return { kind: 'invalid_credentials' };
  }
  if (response.status === 409) {
    return {
      kind: 'conflict',
      message:
        response.data?.message ??
        response.data?.error ??
        'This event day appears to already be open elsewhere.',
    };
  }
  return { kind: 'generic' };
}

function SetupErrorMessage({ error }: { error: SetupError }) {
  if (error.kind === 'invalid_credentials') {
    return (
      <p className="text-sm text-red-600">
        Incorrect cloud email or password. Double-check and try again.
      </p>
    );
  }
  if (error.kind === 'unreachable') {
    return (
      <p className="text-sm text-red-600">
        Could not reach the cloud service. Check the internet connection, or use "Open Day
        (offline)" below if this event was already pre-cached.
      </p>
    );
  }
  if (error.kind === 'conflict') {
    return <p className="text-sm text-red-600">{error.message}</p>;
  }
  return <p className="text-sm text-red-600">Something went wrong. Please try again.</p>;
}

export interface DaySetupScreenProps {
  status: DayLifecycleStatus;
  onStatusChange: (status: DayLifecycleStatus) => void;
}

type Action = 'precache' | 'open' | 'open-offline' | null;

export default function DaySetupScreen({ status, onStatusChange }: DaySetupScreenProps) {
  const [eventId, setEventId] = useState(status.eventId != null ? String(status.eventId) : '');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [action, setAction] = useState<Action>(null);
  const [error, setError] = useState<SetupError | null>(null);
  const [preCacheConfirmed, setPreCacheConfirmed] = useState(false);
  const [splitBrainWarning, setSplitBrainWarning] = useState(false);

  const parsedEventId = Number(eventId);
  const eventIdValid = eventId.trim() !== '' && Number.isFinite(parsedEventId);

  async function runAction(kind: Exclude<Action, null>, fn: () => Promise<DayLifecycleStatus>) {
    setAction(kind);
    setError(null);
    setPreCacheConfirmed(false);
    try {
      const result = await fn();
      if (kind === 'precache') {
        setPreCacheConfirmed(true);
      } else if (result.splitBrainWarning) {
        setSplitBrainWarning(true);
      }
      onStatusChange(result);
    } catch (err) {
      setError(classifySetupError(err));
    } finally {
      setAction(null);
    }
  }

  function handlePreCache(e: React.FormEvent) {
    e.preventDefault();
    if (!eventIdValid) return;
    void runAction('precache', () => preCacheDay(parsedEventId, email, password));
  }

  function handleOpen(e: React.FormEvent) {
    e.preventDefault();
    if (!eventIdValid) return;
    void runAction('open', () => openDay(parsedEventId, email, password));
  }

  function handleOpenOffline(e: React.FormEvent) {
    e.preventDefault();
    if (!eventIdValid) return;
    void runAction('open-offline', () => openDay(parsedEventId));
  }

  const busy = action !== null;
  const primaryIsOpen = status.status === 'PRE_CACHED';

  return (
    <div className="mx-auto flex min-h-screen max-w-md flex-col justify-center gap-6 p-6">
      <div>
        <h1 className="text-xl font-semibold">
          {primaryIsOpen ? 'Open this event day' : 'Set up this event day'}
        </h1>
        <p className="mt-1 text-sm text-slate-600">
          {primaryIsOpen
            ? 'This event has already been pre-cached. Open the day to start checking in officials.'
            : 'Nothing has been prepared on this device yet. Pre-cache to get ready ahead of time, or open the day now to start.'}
        </p>
        {status.lastPreCachedAt && (
          <p className="mt-1 text-xs text-slate-500">
            Last pre-cached: {new Date(status.lastPreCachedAt).toLocaleString()}
          </p>
        )}
      </div>

      {splitBrainWarning && (
        <div
          role="alert"
          className="rounded border-2 border-red-600 bg-red-50 p-3 text-sm font-semibold text-red-800"
        >
          Could not confirm exclusive control with the cloud (opened offline). If another device
          also has this event open, results may conflict — only proceed if you're certain no
          other device is running this event day.
        </div>
      )}

      {preCacheConfirmed && (
        <p className="rounded bg-green-50 p-2 text-sm text-green-700">
          Pre-cached successfully. This device is ready — open the day whenever the event starts.
        </p>
      )}

      <form className="flex flex-col gap-3">
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">Event ID</span>
          <input
            type="number"
            className="rounded border px-2 py-1"
            value={eventId}
            onChange={(e) => setEventId(e.target.value)}
          />
        </label>

        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">Cloud email</span>
          <input
            type="email"
            className="rounded border px-2 py-1"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </label>

        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">Cloud password</span>
          <input
            type="password"
            className="rounded border px-2 py-1"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>

        <div className="flex gap-2">
          <button
            type="button"
            onClick={handlePreCache}
            disabled={busy || !eventIdValid}
            className="flex-1 rounded bg-slate-700 px-3 py-2 text-sm text-white disabled:opacity-50"
            title="Prepare this device ahead of time — does not start the event day."
          >
            {action === 'precache' ? 'Pre-caching…' : 'Pre-cache now (prepare ahead of time)'}
          </button>
          <button
            type="button"
            onClick={handleOpen}
            disabled={busy || !eventIdValid}
            className="flex-1 rounded bg-blue-600 px-3 py-2 text-sm text-white disabled:opacity-50"
            title="Start the event day and lock the cloud so only this device can run it."
          >
            {action === 'open' ? 'Opening…' : 'Open Day (start the event day)'}
          </button>
        </div>

        {error && <SetupErrorMessage error={error} />}
      </form>

      <div className="border-t pt-4">
        <p className="mb-2 text-sm text-slate-600">
          No internet right now, but this event was already pre-cached on this device? Open
          offline using whatever was last pre-cached — no cloud credentials needed.
        </p>
        <button
          type="button"
          onClick={handleOpenOffline}
          disabled={busy || !eventIdValid}
          className="w-full rounded border border-slate-400 px-3 py-2 text-sm font-medium text-slate-700 disabled:opacity-50"
        >
          {action === 'open-offline' ? 'Opening…' : 'Open Day (offline — no connectivity)'}
        </button>
      </div>
    </div>
  );
}
