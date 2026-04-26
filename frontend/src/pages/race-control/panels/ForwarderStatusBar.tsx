import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useStomp } from '@/hooks/race-control/useStomp';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import api from '@/lib/api';

type ConnectionState = 'CONNECTED' | 'RECONNECTING' | 'DISCONNECTED';

type ForwarderStatusDto = {
  decoderState: ConnectionState;
  forwarderState: ConnectionState;
};

async function fetchForwarderStatus(): Promise<ForwarderStatusDto> {
  const { data } = await api.get<ForwarderStatusDto>('/api/v1/race-control/forwarder/status');
  return data;
}

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

  // Seed initial state from REST on mount — STOMP only delivers future changes
  const { data: initialStatus } = useQuery({
    queryKey: ['forwarder-status'],
    queryFn: fetchForwarderStatus,
    staleTime: 0,
  });

  useEffect(() => {
    if (initialStatus) {
      setDecoderState(initialStatus.decoderState);
      setForwarderState(initialStatus.forwarderState);
    }
  }, [initialStatus]);

  // Override with live STOMP pushes on state change
  const { data: stompData } = useStomp<ForwarderStatusDto>('/topic/system/forwarder-status');

  useEffect(() => {
    if (stompData) {
      setDecoderState(stompData.decoderState);
      setForwarderState(stompData.forwarderState);
    }
  }, [stompData]);

  return (
    <div className="flex h-8 items-center gap-3 px-4 bg-card border-b shrink-0">
      <StatusPill component="DECODER" state={decoderState} />
      <StatusPill component="FORWARDER" state={forwarderState} />
    </div>
  );
}
