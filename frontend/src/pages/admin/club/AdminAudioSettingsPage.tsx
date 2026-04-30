import { useState, useEffect } from 'react';
import { Loader2, Trash2, Plus } from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Switch } from '@/components/ui/switch';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  getAdminAudioSettings,
  saveAdminAudioSettings,
  getBlocklist,
  addBlocklistTerm,
  removeBlocklistTerm,
  listVoices,
  type AudioSettingsDto,
} from '@/lib/audioApi';

const TOGGLE_ITEMS: { key: keyof AudioSettingsDto; label: string }[] = [
  { key: 'announceCountdown', label: 'Countdown intervals' },
  { key: 'announceStagger', label: 'Stagger car calls' },
  { key: 'announceLapBeep', label: 'Lap improvement beeps' },
  { key: 'announceFinish', label: 'Finish announcements' },
  { key: 'announceRunningOrder', label: 'Running order' },
];

export default function AdminAudioSettingsPage() {
  const queryClient = useQueryClient();
  const [newWord, setNewWord] = useState('');
  const [localSettings, setLocalSettings] = useState<AudioSettingsDto | null>(null);

  // ── Fetch audio settings ───────────────────────────────────────────────────
  const { data: settings, isLoading: settingsLoading } = useQuery({
    queryKey: ['admin-audio-settings'],
    queryFn: () => getAdminAudioSettings().then((r) => r.data),
  });

  useEffect(() => {
    if (settings && !localSettings) {
      setLocalSettings(settings);
    }
  }, [settings, localSettings]);

  const saveSettingsMutation = useMutation({
    mutationFn: (s: AudioSettingsDto) => saveAdminAudioSettings(s).then((r) => r.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-audio-settings'] });
      toast.success('Audio settings saved.');
    },
    onError: () => toast.error('Failed to save settings.'),
  });

  // ── Fetch voices ───────────────────────────────────────────────────────────
  const { data: voices, isLoading: voicesLoading } = useQuery({
    queryKey: ['voices'],
    queryFn: () => listVoices().then((r) => r.data),
  });

  // ── Fetch blocklist ────────────────────────────────────────────────────────
  const { data: blocklist, isLoading: blocklistLoading } = useQuery({
    queryKey: ['admin-audio-blocklist'],
    queryFn: () => getBlocklist().then((r) => r.data),
  });

  const addMutation = useMutation({
    mutationFn: (word: string) => addBlocklistTerm(word).then((r) => r.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-audio-blocklist'] });
      setNewWord('');
      toast.success('Word added to blocklist.');
    },
    onError: () => toast.error('Failed to add word.'),
  });

  const removeMutation = useMutation({
    mutationFn: (id: number) => removeBlocklistTerm(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-audio-blocklist'] });
      toast.success('Word removed from blocklist.');
    },
    onError: () => toast.error('Failed to remove word.'),
  });

  const displaySettings = localSettings ?? settings;

  const handleToggle = (key: keyof AudioSettingsDto) => {
    if (!displaySettings) return;
    setLocalSettings({ ...displaySettings, [key]: !displaySettings[key] });
  };

  const handleVoiceChange = (voiceId: string) => {
    if (!displaySettings) return;
    setLocalSettings({ ...displaySettings, defaultVoiceId: voiceId });
  };

  const handleSave = () => {
    if (!displaySettings) return;
    saveSettingsMutation.mutate(displaySettings);
  };

  return (
    <div className="max-w-2xl mx-auto space-y-8">
      <h1 className="text-xl font-semibold">Audio Settings</h1>

      {/* Announcement toggles */}
      <Card>
        <CardHeader>
          <CardTitle>Announcement Types</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {settingsLoading ? (
            <div className="flex items-center gap-2 py-4">
              <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
              <span className="text-sm text-muted-foreground">Loading settings…</span>
            </div>
          ) : displaySettings ? (
            <>
              {TOGGLE_ITEMS.map(({ key, label }) => (
                <div key={key} className="flex items-center justify-between h-10">
                  <Label htmlFor={`admin-toggle-${key}`}>{label}</Label>
                  <Switch
                    id={`admin-toggle-${key}`}
                    checked={displaySettings[key] as boolean}
                    onCheckedChange={() => handleToggle(key)}
                    aria-label={label}
                  />
                </div>
              ))}

              {/* Default voice selector */}
              <div className="space-y-2 pt-2">
                <Label htmlFor="default-voice">Default voice</Label>
                {voicesLoading ? (
                  <p className="text-sm text-muted-foreground">Loading voices…</p>
                ) : (
                  <Select
                    value={displaySettings.defaultVoiceId ?? ''}
                    onValueChange={handleVoiceChange}
                  >
                    <SelectTrigger id="default-voice">
                      <SelectValue placeholder="Select default voice" />
                    </SelectTrigger>
                    <SelectContent>
                      {voices?.map((v) => (
                        <SelectItem key={v.voiceId} value={v.voiceId}>
                          {v.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              </div>

              <Button
                onClick={handleSave}
                disabled={saveSettingsMutation.isPending}
                className="mt-2"
              >
                {saveSettingsMutation.isPending ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" aria-hidden="true" />
                    Saving…
                  </>
                ) : (
                  'Save Settings'
                )}
              </Button>
            </>
          ) : null}
        </CardContent>
      </Card>

      {/* Profanity blocklist */}
      <Card>
        <CardHeader>
          <CardTitle>Profanity Blocklist</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <p className="text-sm text-muted-foreground">
            Words added here will be rejected when saving racer names or phonetic
            spellings.
          </p>

          {/* Add word form */}
          <form
            className="flex gap-2"
            onSubmit={(e) => {
              e.preventDefault();
              const w = newWord.trim();
              if (w) addMutation.mutate(w);
            }}
          >
            <Input
              placeholder="Add word…"
              value={newWord}
              onChange={(e) => setNewWord(e.target.value)}
              className="flex-1"
              aria-label="New blocklist word"
            />
            <Button
              type="submit"
              size="sm"
              disabled={!newWord.trim() || addMutation.isPending}
            >
              <Plus className="h-4 w-4 mr-1" aria-hidden="true" />
              Add
            </Button>
          </form>

          {/* Blocklist table */}
          {blocklistLoading ? (
            <div className="flex items-center gap-2 py-2">
              <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
              <span className="text-sm text-muted-foreground">Loading blocklist…</span>
            </div>
          ) : (blocklist?.length ?? 0) === 0 ? (
            <p className="text-sm text-muted-foreground py-2">
              No custom terms. The built-in word list is always active.
            </p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Word</TableHead>
                  <TableHead className="w-32">Added</TableHead>
                  <TableHead className="w-16" />
                </TableRow>
              </TableHeader>
              <TableBody>
                {blocklist?.map((term) => (
                  <TableRow key={term.id}>
                    <TableCell className="font-mono">{term.word}</TableCell>
                    <TableCell className="text-xs text-muted-foreground">
                      {new Date(term.addedAt).toLocaleDateString()}
                    </TableCell>
                    <TableCell>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => removeMutation.mutate(term.id)}
                        disabled={removeMutation.isPending}
                        aria-label={`Remove "${term.word}" from blocklist`}
                      >
                        <Trash2 className="h-4 w-4" aria-hidden="true" />
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
