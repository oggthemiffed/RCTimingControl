import { useEffect, useState } from 'react';
import { useStomp } from '@/hooks/race-control/useStomp';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';

type ConnectionState = 'CONNECTED' | 'RECONNECTING' | 'DISCONNECTED';

type ForwarderStatusDto = {
  decoderState: ConnectionState;
  forwarderState: ConnectionState;
};

function getStateColor(state: ConnectionState | null): string {
  if (state === 'CONNECTED') return 'bg-[var(--flag-green)]';
  if (state === 'RECONNECTING') return 'bg-[var(--flag-yellow)]';
  return 'bg-[var(--flag-red)]';
}

function getTextColor(state: ConnectionState | null): string {
  if (state === 'CONNECTED') return 'text-[var(--flag-green)]';
  if (state === 'RECONNECTING') return 'text-[var(--flag-yellow)]';
  return 'text-[var(--flag-red)]';
}

function getLabel(component: string, state: ConnectionState | null): string {
  if (state === null) return `${component} —`;
  if (state === 'CONNECTED') return `${component} connected`;
  if (state === 'RECONNECTING') return `${component} reconnecting…`;
  return `${component} disconnected`;
}

type StatusPillProps = {
  component: string;
  state: ConnectionState | null;
};

function StatusPill({ component, state }: StatusPillProps) {
  const dotColor = getStateColor(state);
  const textColor = getTextColor(state);
  const isConnected = state === 'CONNECTED';

  return (
    <Badge
      variant="outline"
      className={cn('gap-2 h-6 px-3', textColor)}
      aria-label={`${component} connection status: ${state ?? 'unknown'}`}
    >
      <span
        className={cn(
          'inline-block h-2 w-2 rounded-full',
          dotColor,
          isConnected && 'animate-pulse'
        )}
        aria-hidden="true"
      />
      <span className="text-xs font-normal">{getLabel(component, state)}</span>
    </Badge>
  );
}

export function ForwarderStatusBar() {
  const [decoderState, setDecoderState] = useState<ConnectionState | null>(null);
  const [forwarderState, setForwarderState] = useState<ConnectionState | null>(null);

  const { data } = useStomp<ForwarderStatusDto>('/topic/system/forwarder-status');

  useEffect(() => {
    if (data) {
      setDecoderState(data.decoderState);
      setForwarderState(data.forwarderState);
    }
  }, [data]);

  return (
    <div className="flex h-8 items-center gap-3 px-4 bg-card border-b shrink-0">
      <StatusPill component="DECODER" state={decoderState} />
      <StatusPill component="FORWARDER" state={forwarderState} />
    </div>
  );
}
