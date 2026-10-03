import { useEffect, useState } from 'react';
import {
  getDayLifecycleStatus,
  listOfficials,
  login,
  recover,
  type DayLifecycleStatus,
  type OfficialSummary,
} from '@/lib/api';
import { getStoredSession, storeSession, type StoredSession } from '@/lib/auth';
import OfficialShell from '@/features/shell/OfficialShell';
import DaySetupScreen from '@/features/day-lifecycle/DaySetupScreen';

type LoginError =
  | { kind: 'invalid_credential' }
  | { kind: 'locked'; retryAfterSeconds: number }
  | { kind: 'generic' };

type RecoveryResult = { kind: 'success' } | { kind: 'invalid' } | { kind: 'generic' };

export default function LoginPage() {
  const [checkingSession, setCheckingSession] = useState(true);
  const [session, setSession] = useState<StoredSession | null>(null);

  // Day-lifecycle status. null means "not yet known" while loading, or "status fetch itself
  // failed" (the local backend should always be reachable — same machine — so on failure we
  // fall back to today's behavior rather than blocking the page: attempt listOfficials() and
  // show the picker as if the day were open).
  const [dayStatus, setDayStatus] = useState<DayLifecycleStatus | null>(null);
  // Sticky once set — the split-brain warning (from an offline day-open) stays visible for the
  // rest of this session even after we move on to the officials picker / OfficialShell.
  const [splitBrainWarning, setSplitBrainWarning] = useState(false);

  const [officials, setOfficials] = useState<OfficialSummary[]>([]);
  const [officialsError, setOfficialsError] = useState(false);

  const [selectedCredentialId, setSelectedCredentialId] = useState<number | ''>('');
  const [secret, setSecret] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [loginError, setLoginError] = useState<LoginError | null>(null);

  const [showRecovery, setShowRecovery] = useState(false);
  const [recoveryCredentialId, setRecoveryCredentialId] = useState('');
  const [recoverySecret, setRecoverySecret] = useState('');
  const [targetCredentialId, setTargetCredentialId] = useState('');
  const [recoverySubmitting, setRecoverySubmitting] = useState(false);
  const [recoveryResult, setRecoveryResult] = useState<RecoveryResult | null>(null);

  useEffect(() => {
    let cancelled = false;

    function loadOfficials() {
      return listOfficials()
        .then((list) => {
          if (!cancelled) setOfficials(list);
        })
        .catch(() => {
          if (!cancelled) setOfficialsError(true);
        });
    }

    getStoredSession()
      .then((stored) => {
        if (cancelled) return;
        setSession(stored);
        setCheckingSession(false);
        if (stored) return;

        return getDayLifecycleStatus()
          .then((status) => {
            if (cancelled) return;
            setDayStatus(status);
            if (status.splitBrainWarning) {
              setSplitBrainWarning(true);
            }
            // Officials only become available to log in with once the day is OPEN — see
            // DaySetupScreen for NOT_SET_UP/PRE_CACHED, and the CLOSED message below.
            if (status.status === 'OPEN') {
              return loadOfficials();
            }
          })
          .catch(() => {
            if (cancelled) return;
            // Status fetch against our own local backend shouldn't normally fail. Don't block
            // the whole page on it — fall back to today's behavior.
            setDayStatus(null);
            return loadOfficials();
          });
      })
      .catch(() => {
        if (cancelled) return;
        setCheckingSession(false);
        setOfficialsError(true);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  function handleDayStatusChange(newStatus: DayLifecycleStatus) {
    setDayStatus(newStatus);
    if (newStatus.splitBrainWarning) {
      setSplitBrainWarning(true);
    }
    if (newStatus.status === 'OPEN') {
      listOfficials()
        .then((list) => setOfficials(list))
        .catch(() => setOfficialsError(true));
    }
  }

  function handleDayClosed() {
    // The session was already cleared by OfficialShell before calling this. Reset local state
    // so we fall through to the CLOSED message rather than the (now-empty) officials picker.
    setSession(null);
    setDayStatus({
      status: 'CLOSED',
      eventId: null,
      generation: null,
      splitBrainWarning: false,
      pendingSyncCount: 0,
      lastPreCachedAt: null,
      superseded: false,
    });
    setOfficials([]);
    setSplitBrainWarning(false);
  }

  async function handleLoginSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (selectedCredentialId === '') return;
    setSubmitting(true);
    setLoginError(null);
    try {
      const response = await login(selectedCredentialId, secret);
      await storeSession(response);
      setSession(response);
    } catch (err) {
      setLoginError(classifyLoginError(err));
    } finally {
      setSubmitting(false);
    }
  }

  async function handleRecoverySubmit(e: React.FormEvent) {
    e.preventDefault();
    setRecoverySubmitting(true);
    setRecoveryResult(null);
    try {
      const result = await recover(
        Number(recoveryCredentialId),
        recoverySecret,
        Number(targetCredentialId),
      );
      setRecoveryResult(result.unlocked ? { kind: 'success' } : { kind: 'generic' });
    } catch (err) {
      setRecoveryResult(classifyRecoveryError(err));
    } finally {
      setRecoverySubmitting(false);
    }
  }

  if (checkingSession) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p>Loading…</p>
      </div>
    );
  }

  if (session) {
    return <OfficialShell splitBrainWarning={splitBrainWarning} onDayClosed={handleDayClosed} />;
  }

  if (dayStatus && dayStatus.status === 'CLOSED') {
    return (
      <div className="mx-auto flex min-h-screen max-w-md flex-col items-center justify-center gap-3 p-6 text-center">
        <h1 className="text-xl font-semibold">This event day is closed</h1>
        <p className="text-sm text-slate-600">
          Cached data for this event has been cleared from this device. Pre-cache the next event
          when it's ready to prepare this device again.
        </p>
        <button
          type="button"
          onClick={() =>
            setDayStatus({
              status: 'NOT_SET_UP',
              eventId: null,
              generation: null,
              splitBrainWarning: false,
              pendingSyncCount: 0,
              lastPreCachedAt: null,
              superseded: false,
            })
          }
          className="mt-2 rounded bg-blue-600 px-3 py-2 text-sm text-white"
        >
          Prepare this device for the next event
        </button>
      </div>
    );
  }

  if (dayStatus && (dayStatus.status === 'NOT_SET_UP' || dayStatus.status === 'PRE_CACHED')) {
    return <DaySetupScreen status={dayStatus} onStatusChange={handleDayStatusChange} />;
  }

  return (
    <div className="mx-auto flex min-h-screen max-w-md flex-col justify-center gap-6 p-6">
      <h1 className="text-xl font-semibold">Local Race Day Login</h1>

      {officialsError && (
        <p className="text-sm text-red-600">Could not load officials. Check the connection and retry.</p>
      )}

      <form onSubmit={handleLoginSubmit} className="flex flex-col gap-3">
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">Official</span>
          <select
            className="rounded border px-2 py-1"
            value={selectedCredentialId}
            onChange={(e) =>
              setSelectedCredentialId(e.target.value === '' ? '' : Number(e.target.value))
            }
          >
            <option value="">Select an official…</option>
            {officials.map((o) => (
              <option key={o.credentialId} value={o.credentialId}>
                {o.officialName}
              </option>
            ))}
          </select>
        </label>

        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">PIN</span>
          <input
            type="password"
            inputMode="numeric"
            className="rounded border px-2 py-1"
            value={secret}
            onChange={(e) => setSecret(e.target.value)}
          />
        </label>

        <button
          type="submit"
          disabled={submitting || selectedCredentialId === ''}
          className="rounded bg-blue-600 px-3 py-2 text-white disabled:opacity-50"
        >
          {submitting ? 'Logging in…' : 'Log in'}
        </button>

        {loginError && <LoginErrorMessage error={loginError} />}
      </form>

      <div className="border-t pt-4">
        <button
          type="button"
          className="text-sm font-medium text-blue-700 underline"
          onClick={() => setShowRecovery((v) => !v)}
        >
          Use recovery credential
        </button>

        {showRecovery && (
          <form onSubmit={handleRecoverySubmit} className="mt-3 flex flex-col gap-3">
            <label className="flex flex-col gap-1">
              <span className="text-sm font-medium">Recovery official credential ID</span>
              <input
                type="number"
                className="rounded border px-2 py-1"
                value={recoveryCredentialId}
                onChange={(e) => setRecoveryCredentialId(e.target.value)}
              />
            </label>

            <label className="flex flex-col gap-1">
              <span className="text-sm font-medium">Recovery secret</span>
              <input
                type="password"
                className="rounded border px-2 py-1"
                value={recoverySecret}
                onChange={(e) => setRecoverySecret(e.target.value)}
              />
            </label>

            <label className="flex flex-col gap-1">
              <span className="text-sm font-medium">Official to unlock (credential ID)</span>
              <input
                type="number"
                className="rounded border px-2 py-1"
                value={targetCredentialId}
                onChange={(e) => setTargetCredentialId(e.target.value)}
              />
            </label>

            <button
              type="submit"
              disabled={recoverySubmitting}
              className="rounded bg-slate-700 px-3 py-2 text-white disabled:opacity-50"
            >
              {recoverySubmitting ? 'Submitting…' : 'Unlock'}
            </button>

            {recoveryResult && <RecoveryResultMessage result={recoveryResult} />}
          </form>
        )}
      </div>
    </div>
  );
}

function LoginErrorMessage({ error }: { error: LoginError }) {
  if (error.kind === 'invalid_credential') {
    return <p className="text-sm text-red-600">Incorrect PIN.</p>;
  }
  if (error.kind === 'locked') {
    return (
      <p className="text-sm text-red-600">
        Too many attempts. Try again in {error.retryAfterSeconds} seconds.
      </p>
    );
  }
  return <p className="text-sm text-red-600">Something went wrong. Please try again.</p>;
}

function RecoveryResultMessage({ result }: { result: RecoveryResult }) {
  if (result.kind === 'success') {
    return <p className="text-sm text-green-700">Credential unlocked.</p>;
  }
  if (result.kind === 'invalid') {
    return <p className="text-sm text-red-600">Invalid recovery credential.</p>;
  }
  return <p className="text-sm text-red-600">Something went wrong. Please try again.</p>;
}

function classifyLoginError(err: unknown): LoginError {
  const response = (err as { response?: { status?: number; data?: { error?: string; retryAfterSeconds?: number } } })
    .response;
  if (response?.status === 401) {
    return { kind: 'invalid_credential' };
  }
  if (response?.status === 423) {
    return { kind: 'locked', retryAfterSeconds: response.data?.retryAfterSeconds ?? 0 };
  }
  return { kind: 'generic' };
}

function classifyRecoveryError(err: unknown): RecoveryResult {
  const response = (err as { response?: { status?: number } }).response;
  if (response?.status === 401) {
    return { kind: 'invalid' };
  }
  return { kind: 'generic' };
}
