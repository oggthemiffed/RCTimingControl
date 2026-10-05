import { useEffect } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { useStomp } from '@/hooks/race-control/useStomp';
import { useAuth } from '@/hooks/useAuth';
import { Badge } from '@/components/ui/badge';
import { Switch } from '@/components/ui/switch';
import { cn } from '@/lib/utils';
import {
  fetchLiveFeedStatus,
  getLiveFeedSetting,
  setLiveFeedSetting,
  type LiveFeedState,
  type LiveFeedStatusDto,
} from '@/lib/raceControlApi';

const LABEL: Record<Exclude<LiveFeedState, 'NOT_SET_UP'>, string> = {
  IDLE: 'LIVE FEED ready',
  CONNECTING: 'LIVE FEED connecting…',
  CONNECTED: 'LIVE FEED sending',
  RECONNECTING: 'LIVE FEED reconnecting…',
};

function textColor(state: LiveFeedState): string {
  if (state === 'CONNECTED') return 'text-[var(--flag-green)]';
  if (state === 'CONNECTING' || state === 'RECONNECTING') return 'text-[var(--flag-yellow)]';
  return 'text-muted-foreground';
}

function dotColor(state: LiveFeedState): string {
  if (state === 'CONNECTED') return 'bg-[var(--flag-green)]';
  if (state === 'CONNECTING' || state === 'RECONNECTING') return 'bg-[var(--flag-yellow)]';
  return 'bg-muted-foreground';
}

/**
 * The live feed for remote viewers (#28): whether the app is sending to the relay, and a switch for whether
 * this event's races are sent. Hidden when no relay is set up, since then there is nothing to show.
 */
export function LiveFeedStatus({ eventId }: { eventId: number }) {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const canChange = !!user?.roles.some(r => r === 'ADMIN' || r === 'RACE_DIRECTOR');

  const { data: status } = useQuery({
    queryKey: ['live-feed-status'],
    queryFn: fetchLiveFeedStatus,
    staleTime: 0,
    refetchInterval: 10000,
  });

  // A change pushed by the server replaces the polled status straight away
  const { data: pushed } = useStomp<LiveFeedStatusDto>('/topic/system/live-feed-status');
  useEffect(() => {
    if (pushed) queryClient.setQueryData(['live-feed-status'], pushed);
  }, [pushed, queryClient]);

  const setUp = !!status && status.state !== 'NOT_SET_UP';
  const settingKey = ['live-feed-setting', eventId];
  const { data: setting } = useQuery({
    queryKey: settingKey,
    queryFn: () => getLiveFeedSetting(eventId),
    enabled: setUp,
  });
  const change = useMutation({
    mutationFn: (enabled: boolean) => setLiveFeedSetting(eventId, enabled),
    onSuccess: saved => queryClient.setQueryData(settingKey, saved),
    onError: () => toast.error('Could not change the live feed for this event.'),
  });

  if (!status || status.state === 'NOT_SET_UP') return null;
  const state = status.state;

  return (
    <div className="flex items-center gap-3">
      <Badge
        variant="outline"
        className={cn('gap-2 h-6 px-3', textColor(state))}
        aria-label={`Live feed status: ${state}`}
        title={status.relayHost ? `Relay: ${status.relayHost}` : undefined}
      >
        <span
          className={cn('inline-block h-2 w-2 rounded-full', dotColor(state), state === 'CONNECTED' && 'animate-pulse')}
          aria-hidden="true"
        />
        <span className="text-xs font-normal">{LABEL[state]}</span>
      </Badge>
      <label className="flex items-center gap-2 text-xs text-muted-foreground">
        <Switch
          checked={setting?.enabled ?? false}
          onCheckedChange={enabled => change.mutate(enabled)}
          disabled={!canChange || !setting || change.isPending}
          aria-label="Send this event to the live feed"
        />
        Send this event
      </label>
    </div>
  );
}
