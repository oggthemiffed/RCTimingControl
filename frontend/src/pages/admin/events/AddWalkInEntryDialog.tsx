import { useState } from 'react';
import axios from 'axios';
import { toast } from 'sonner';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';

import { useAdminCompetitorsList } from '@/hooks/admin/useAdminCompetitors';
import { useCreateWalkInEntry } from '@/hooks/admin/useAdminEntries';
import type { CompetitorSummaryDto } from '@/lib/adminApi';

interface AddWalkInEntryDialogProps {
  eventId: number;
  classId: number;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const MAX_MATCHES = 8;

/** Add a walk-in entry by hand: pick an existing driver or type a new name (L9, #17). */
export default function AddWalkInEntryDialog({ eventId, classId, open, onOpenChange }: AddWalkInEntryDialogProps) {
  const [driverText, setDriverText] = useState('');
  const [selected, setSelected] = useState<CompetitorSummaryDto | null>(null);
  const [primary, setPrimary] = useState('');
  const [secondary, setSecondary] = useState('');
  const [error, setError] = useState<string | null>(null);

  const { data: competitors = [] } = useAdminCompetitorsList();
  const createEntry = useCreateWalkInEntry(eventId, classId);

  function handleOpenChange(next: boolean) {
    if (!next) {
      setDriverText('');
      setSelected(null);
      setPrimary('');
      setSecondary('');
      setError(null);
    }
    onOpenChange(next);
  }

  const query = driverText.trim().toLowerCase();
  const matches = selected || !query
    ? []
    : competitors.filter(c => c.displayName.toLowerCase().includes(query)).slice(0, MAX_MATCHES);
  const exactMatch = competitors.find(c => c.displayName.trim().toLowerCase() === query);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!selected && !driverText.trim()) {
      setError('Choose a driver or enter a name.');
      return;
    }
    if (!primary.trim()) {
      setError('Enter the primary transponder number.');
      return;
    }
    if (secondary.trim() && secondary.trim() === primary.trim()) {
      setError('The secondary transponder must be different from the primary.');
      return;
    }
    setError(null);
    try {
      const result = await createEntry.mutateAsync({
        ...(selected ? { competitorId: selected.id } : { competitorName: driverText.trim() }),
        primaryTransponder: primary.trim(),
        ...(secondary.trim() ? { secondaryTransponder: secondary.trim() } : {}),
      });
      toast.success(`Entry added for ${selected?.displayName ?? driverText.trim()}.`);
      result.warnings.forEach(w => toast.warning(w));
      handleOpenChange(false);
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.status === 409) {
        setError(err.response.data?.detail ?? 'This driver already has an entry in this class.');
      } else if (axios.isAxiosError(err) && (err.response?.status === 400 || err.response?.status === 422)) {
        setError(err.response.data?.detail ?? 'Check the details and try again.');
      } else {
        setError('Could not add the entry. Check your connection and try again.');
      }
    }
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent>
        <form onSubmit={handleSubmit} className="space-y-4">
          <DialogHeader>
            <DialogTitle>Add entry</DialogTitle>
            <DialogDescription>
              Add a walk-in to this class. Pick a driver who has raced before, or type a new name.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-1.5">
            <Label htmlFor="walkin-driver">Driver</Label>
            {selected ? (
              <div className="flex items-center gap-2 rounded-md border px-3 py-2 text-sm">
                <span className="flex-1 font-medium">{selected.displayName}</span>
                {selected.homeClub && <span className="text-muted-foreground">{selected.homeClub}</span>}
                <Button type="button" size="sm" variant="ghost" onClick={() => setSelected(null)}>
                  Change
                </Button>
              </div>
            ) : (
              <>
                <Input
                  id="walkin-driver"
                  value={driverText}
                  onChange={e => setDriverText(e.target.value)}
                  placeholder="Search drivers or type a new name"
                  autoComplete="off"
                />
                {matches.length > 0 && (
                  <ul className="rounded-md border divide-y" aria-label="Matching drivers">
                    {matches.map(c => (
                      <li key={c.id}>
                        <button
                          type="button"
                          className="w-full px-3 py-1.5 text-left text-sm hover:bg-muted"
                          onClick={() => setSelected(c)}
                        >
                          {c.displayName}
                          {c.homeClub && <span className="text-muted-foreground"> · {c.homeClub}</span>}
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
                {query && !exactMatch && (
                  <p className="text-xs text-muted-foreground">
                    “{driverText.trim()}” will be added as a new driver.
                  </p>
                )}
                {exactMatch && (
                  <p className="text-xs text-amber-700 dark:text-amber-400">
                    {exactMatch.displayName} is already a driver. Pick them from the list to use their record, or
                    “{driverText.trim()}” will be added as a separate new driver.
                  </p>
                )}
              </>
            )}
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="walkin-primary">Primary transponder</Label>
              <Input id="walkin-primary" value={primary} maxLength={20} onChange={e => setPrimary(e.target.value)} />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="walkin-secondary">Secondary transponder (optional)</Label>
              <Input
                id="walkin-secondary"
                value={secondary}
                maxLength={20}
                onChange={e => setSecondary(e.target.value)}
              />
            </div>
          </div>

          {error && <p className="text-sm text-destructive">{error}</p>}

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={createEntry.isPending}>
              {createEntry.isPending ? 'Adding…' : 'Add entry'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
