import { useState, useEffect } from 'react';
import { ChevronDown, ChevronUp, Volume2, VolumeX, Loader2 } from 'lucide-react';
import { Switch } from '@/components/ui/switch';
import { Slider } from '@/components/ui/slider';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAnnouncements } from '@/hooks/race-control/useAnnouncements';
import {
  getAudioSettings,
  patchAudioSettings,
  type AudioSettingsDto,
} from '@/lib/audioApi';

interface AudioSettingsPanelProps {
  raceId: number | null;
}

const TOGGLE_ITEMS: { key: keyof AudioSettingsDto; label: string }[] = [
  { key: 'announceCountdown', label: 'Countdown intervals' },
  { key: 'announceStagger', label: 'Stagger car calls' },
  { key: 'announceLapBeep', label: 'Lap improvement beeps' },
  { key: 'announceFinish', label: 'Finish announcements' },
  { key: 'announceRunningOrder', label: 'Running order' },
];

const BOOLEAN_KEYS = TOGGLE_ITEMS.map((t) => t.key);

export function AudioSettingsPanel({ raceId }: AudioSettingsPanelProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [volume, setVolume] = useState<number>(() => {
    const stored = localStorage.getItem('rc-audio-volume');
    return stored ? parseInt(stored, 10) : 80;
  });
  const queryClient = useQueryClient();

  const { data: settings, isLoading } = useQuery({
    queryKey: ['audio-settings'],
    queryFn: () => getAudioSettings().then((r) => r.data),
  });

  const mutation = useMutation({
    mutationFn: (s: AudioSettingsDto) => patchAudioSettings(s).then((r) => r.data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['audio-settings'] }),
  });

  const { testAudio } = useAnnouncements({
    raceId,
    settings: settings ?? null,
    volume: volume / 100,
  });

  // Persist volume to localStorage whenever it changes
  useEffect(() => {
    localStorage.setItem('rc-audio-volume', String(volume));
  }, [volume]);

  const toggleSetting = (key: keyof AudioSettingsDto) => {
    if (!settings) return;
    const updated = { ...settings, [key]: !settings[key] };
    mutation.mutate(updated);
  };

  const updateDepth = (depth: number) => {
    if (!settings) return;
    mutation.mutate({ ...settings, runningOrderDepth: depth });
  };

  const allEnabled =
    settings !== undefined &&
    BOOLEAN_KEYS.every((k) => settings[k] as boolean);
  const someEnabled =
    settings !== undefined &&
    BOOLEAN_KEYS.some((k) => settings[k] as boolean);

  const statusDot = allEnabled
    ? 'bg-[var(--flag-green)]'
    : someEnabled
      ? 'bg-[var(--flag-yellow)]'
      : 'bg-[var(--flag-red)]';

  return (
    <div className="border-b" data-testid="audio-settings-panel">
      {/* Trigger row */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex h-10 w-full items-center gap-2 px-4 hover:bg-muted/50 select-none"
        aria-expanded={isOpen}
        aria-controls="audio-settings-panel-content"
      >
        <Volume2 className="h-4 w-4 shrink-0" aria-hidden="true" />
        <span className="text-sm font-normal">Audio Settings</span>
        <span
          className={`ml-auto h-2 w-2 rounded-full ${statusDot}`}
          aria-label="Audio status"
          data-testid="audio-status-dot"
        />
        {isOpen ? (
          <ChevronUp className="h-4 w-4" aria-hidden="true" />
        ) : (
          <ChevronDown className="h-4 w-4" aria-hidden="true" />
        )}
      </button>

      {/* Expanded panel */}
      {isOpen && (
        <div
          id="audio-settings-panel-content"
          className="px-4 pb-4 space-y-4"
        >
          {isLoading ? (
            <div className="flex items-center gap-2 py-4">
              <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
              <span className="text-sm text-muted-foreground">
                Loading settings…
              </span>
            </div>
          ) : settings ? (
            <>
              <p className="text-xs font-semibold text-muted-foreground pt-2">
                Announcement Types
              </p>

              {TOGGLE_ITEMS.map(({ key, label }) => (
                <div key={key} className="flex items-center justify-between h-10">
                  <label className="text-sm" htmlFor={`toggle-${key}`}>
                    {label}
                  </label>
                  <Switch
                    id={`toggle-${key}`}
                    checked={settings[key] as boolean}
                    onCheckedChange={() => toggleSetting(key)}
                    aria-label={label}
                  />
                </div>
              ))}

              <div className="flex items-center gap-2">
                <label className="text-sm shrink-0" htmlFor="running-order-depth">
                  Running order depth
                </label>
                <Input
                  id="running-order-depth"
                  type="number"
                  min={1}
                  max={20}
                  defaultValue={settings.runningOrderDepth}
                  onBlur={(e) => updateDepth(parseInt(e.target.value, 10) || 3)}
                  className="w-20 h-8 text-center"
                />
                <span className="text-sm text-muted-foreground">positions</span>
              </div>

              <p className="text-xs font-semibold text-muted-foreground pt-2">
                Volume
              </p>
              <div className="flex items-center gap-2">
                <Slider
                  value={[volume]}
                  onValueChange={([v]) => setVolume(v)}
                  min={0}
                  max={100}
                  step={5}
                  className="flex-1"
                  aria-label="Audio volume"
                />
                {volume > 0 ? (
                  <Volume2 className="h-4 w-4 shrink-0" aria-hidden="true" />
                ) : (
                  <VolumeX className="h-4 w-4 shrink-0" aria-hidden="true" />
                )}
                <span className="text-xs w-8 text-right">{volume}%</span>
              </div>

              <Button
                variant="outline"
                size="sm"
                onClick={testAudio}
                aria-label="Test audio"
              >
                ▶ Test Audio
              </Button>
              <p className="text-xs text-muted-foreground">
                Plays a sample announcement to verify speakers are working.
              </p>
            </>
          ) : null}
        </div>
      )}
    </div>
  );
}
