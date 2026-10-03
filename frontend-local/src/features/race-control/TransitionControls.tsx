// Race-state transition controls, one set of buttons per current status (PENDING/GRID/RUNNING/
// STOPPED show controls; FINISHED is terminal and shows none). A single `pendingTarget` guards
// against a second click firing a second request while one is in flight — same pattern as
// CheckInDesk's `confirming` boolean. On rejection (typically 409 illegal_transition, if another
// official already moved the race) the parent is asked to re-fetch the race detail so the UI
// re-syncs to the server's actual state rather than trusting a stale local status.
import { useState } from 'react';
import { transitionRace, type RaceStatus, type ScheduleEntryDto } from '@/lib/api';

interface TransitionOption {
  target: RaceStatus;
  label: string;
  pendingLabel: string;
}

const TRANSITIONS: Record<RaceStatus, TransitionOption[]> = {
  PENDING: [{ target: 'GRID', label: 'Call to grid', pendingLabel: 'Calling to grid…' }],
  GRID: [{ target: 'RUNNING', label: 'Start race', pendingLabel: 'Starting…' }],
  RUNNING: [
    { target: 'STOPPED', label: 'Stop', pendingLabel: 'Stopping…' },
    { target: 'FINISHED', label: 'Finish', pendingLabel: 'Finishing…' },
  ],
  STOPPED: [
    { target: 'RUNNING', label: 'Resume', pendingLabel: 'Resuming…' },
    { target: 'FINISHED', label: 'Finish', pendingLabel: 'Finishing…' },
  ],
  FINISHED: [],
};

function classifyTransitionError(err: unknown): string {
  const status = (err as { response?: { status?: number } })?.response?.status;
  if (status === 409) {
    return 'That transition is no longer valid — refreshing race status.';
  }
  if (status === 404) {
    return 'Race not found — refreshing.';
  }
  if (status === 400) {
    return 'Invalid target state — refreshing.';
  }
  return 'Something went wrong. Please try again.';
}

export interface TransitionControlsProps {
  race: ScheduleEntryDto;
  onTransitioned: (updated: ScheduleEntryDto) => void;
  onTransitionFailed: () => void;
}

export default function TransitionControls({
  race,
  onTransitioned,
  onTransitionFailed,
}: TransitionControlsProps) {
  const [pendingTarget, setPendingTarget] = useState<RaceStatus | null>(null);
  const [error, setError] = useState<string | null>(null);

  const options = TRANSITIONS[race.status];

  async function handleClick(target: RaceStatus) {
    if (pendingTarget !== null) return;
    setPendingTarget(target);
    setError(null);
    try {
      const updated = await transitionRace(race.id, target);
      onTransitioned(updated);
    } catch (err) {
      setError(classifyTransitionError(err));
      onTransitionFailed();
    } finally {
      setPendingTarget(null);
    }
  }

  if (options.length === 0) {
    return null;
  }

  return (
    <div className="flex flex-col gap-2">
      <div className="flex gap-2">
        {options.map((option) => (
          <button
            key={option.target}
            type="button"
            disabled={pendingTarget !== null}
            onClick={() => handleClick(option.target)}
            className="rounded bg-blue-600 px-3 py-2 text-white disabled:opacity-50"
          >
            {pendingTarget === option.target ? option.pendingLabel : option.label}
          </button>
        ))}
      </div>
      {error && <p className="text-sm text-red-600">{error}</p>}
    </div>
  );
}
