import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Loader2, ChevronLeft, RefreshCw } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import api from '@/lib/api';

interface PhoneticDto {
  displayName: string;
  phoneticName: string | null;
}

const getRacerPhonetic = (userId: number): Promise<PhoneticDto> =>
  api.get<PhoneticDto>(`/api/v1/admin/audio/racer/${userId}/phonetic`).then((r) => r.data);

const updateRacerPhonetic = (userId: number, phoneticName: string | null): Promise<PhoneticDto> =>
  api
    .put<PhoneticDto>(`/api/v1/admin/audio/racer/${userId}/phonetic`, { phoneticName })
    .then((r) => r.data);

const regenerateNameClip = (userId: number): Promise<void> =>
  api.delete(`/api/v1/admin/audio/racer/${userId}/name-clip`).then(() => undefined);

export default function AdminRacerDetailPage() {
  const { userId } = useParams<{ userId: string }>();
  const id = Number(userId);
  const queryClient = useQueryClient();
  const [editedPhonetic, setEditedPhonetic] = useState('');
  const [confirmClear, setConfirmClear] = useState(false);

  const { data: phonetic, isLoading } = useQuery({
    queryKey: ['racer-phonetic', id],
    queryFn: () => getRacerPhonetic(id),
    enabled: id > 0,
    select: (data) => {
      // Seed local input if not yet edited
      setEditedPhonetic((prev) => prev || data.phoneticName || '');
      return data;
    },
  });

  const saveMutation = useMutation({
    mutationFn: (phoneticName: string | null) => updateRacerPhonetic(id, phoneticName),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['racer-phonetic', id] });
      toast.success('Phonetic name saved.');
    },
    onError: () => toast.error('Failed to save phonetic name.'),
  });

  const clearAndRegenerateMutation = useMutation({
    mutationFn: async () => {
      await updateRacerPhonetic(id, null);
      await regenerateNameClip(id);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['racer-phonetic', id] });
      setEditedPhonetic('');
      setConfirmClear(false);
      toast.success('Phonetic name cleared. Clip will regenerate.');
    },
    onError: () => {
      toast.error('Failed to clear phonetic name.');
      setConfirmClear(false);
    },
  });

  return (
    <div className="max-w-xl mx-auto space-y-6">
      <div className="flex items-center gap-2">
        <Link
          to="/admin/racers"
          className="flex items-center gap-1 text-muted-foreground hover:text-foreground transition-colors text-sm"
        >
          <ChevronLeft className="h-4 w-4" />
          Back
        </Link>
        <h1 className="text-xl font-semibold">Racer Detail</h1>
      </div>

      {isLoading && (
        <div className="flex items-center gap-2 py-4">
          <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
          <span className="text-sm text-muted-foreground">Loading…</span>
        </div>
      )}

      {phonetic && (
        <Card>
          <CardHeader>
            <CardTitle>Announcement Settings</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-1">
              <p className="text-sm text-muted-foreground">
                Display name: <span className="font-medium text-foreground">{phonetic.displayName}</span>
              </p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="phonetic-input">Phonetic name</Label>
              <Input
                id="phonetic-input"
                placeholder={`e.g. "Tom-is JONES"`}
                value={editedPhonetic}
                onChange={(e) => setEditedPhonetic(e.target.value)}
              />
              <p className="text-xs text-muted-foreground">
                If set, this text is used instead of the display name for TTS announcements.
              </p>
            </div>

            <div className="flex gap-2">
              <Button
                onClick={() => saveMutation.mutate(editedPhonetic || null)}
                disabled={saveMutation.isPending}
              >
                {saveMutation.isPending ? (
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" aria-hidden="true" />
                ) : null}
                Save
              </Button>

              {!confirmClear ? (
                <Button
                  variant="outline"
                  onClick={() => setConfirmClear(true)}
                  disabled={clearAndRegenerateMutation.isPending}
                >
                  <RefreshCw className="mr-2 h-4 w-4" aria-hidden="true" />
                  Clear &amp; Regenerate Clip
                </Button>
              ) : (
                <div className="flex items-center gap-2 border rounded-md px-3 py-2 bg-destructive/10">
                  <span className="text-sm">Clear phonetic name?</span>
                  <Button
                    variant="destructive"
                    size="sm"
                    onClick={() => clearAndRegenerateMutation.mutate()}
                    disabled={clearAndRegenerateMutation.isPending}
                  >
                    {clearAndRegenerateMutation.isPending ? (
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" aria-hidden="true" />
                    ) : null}
                    Clear
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => setConfirmClear(false)}
                  >
                    Cancel
                  </Button>
                </div>
              )}
            </div>

            <p className="text-xs text-muted-foreground">
              Clearing regenerates the name clip from the display name.
            </p>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
