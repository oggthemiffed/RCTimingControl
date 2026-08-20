// +1 / -1 lap marshal-adjustment control for a single filled grid entry. Owns its own
// pending/error state (same guard pattern as CheckInDesk's `confirming`) so a rapid double-click
// can't fire two requests; the caller is only told the delta that succeeded so it can update the
// live table optimistically, and reconciles fully on the next STOMP `/timing` broadcast.
import { useState } from 'react';
import { recordMarshalAdjustment } from '@/lib/api';

export interface MarshalButtonsProps {
  raceId: number;
  cachedEntryId: number;
  onAdjusted: (lapDelta: 1 | -1) => void;
}

export default function MarshalButtons({ raceId, cachedEntryId, onAdjusted }: MarshalButtonsProps) {
  const [pending, setPending] = useState(false);
  const [error, setError] = useState(false);

  async function adjust(lapDelta: 1 | -1) {
    if (pending) return;
    setPending(true);
    setError(false);
    try {
      await recordMarshalAdjustment(raceId, cachedEntryId, lapDelta);
      onAdjusted(lapDelta);
    } catch {
      setError(true);
    } finally {
      setPending(false);
    }
  }

  return (
    <span className="flex items-center gap-1">
      <button
        type="button"
        disabled={pending}
        onClick={() => adjust(1)}
        aria-label="Add marshal lap"
        className="rounded border px-2 py-0.5 text-xs disabled:opacity-50"
      >
        +1 lap
      </button>
      <button
        type="button"
        disabled={pending}
        onClick={() => adjust(-1)}
        aria-label="Remove marshal lap"
        className="rounded border px-2 py-0.5 text-xs disabled:opacity-50"
      >
        −1 lap
      </button>
      {error && <span className="text-xs text-red-600">Failed</span>}
    </span>
  );
}
