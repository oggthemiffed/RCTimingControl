import { useState } from 'react';
import { toast } from 'sonner';
import { Loader2 } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';

import { useRacingClasses } from '@/hooks/admin/useAdminEventClasses';
import {
  useRaceHubClassMappings,
  useRaceHubImport,
  useReplaceRaceHubClassMappings,
} from '@/hooks/admin/useRaceHubImport';
import type { EventClassDto, RaceHubImportResult } from '@/lib/adminApi';

interface RaceHubImportDialogProps {
  eventId: number;
  classes: EventClassDto[];
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const SUMMARY_LABELS: { key: keyof RaceHubImportResult['summary']; label: string }[] = [
  { key: 'created', label: 'New' },
  { key: 'updated', label: 'Updated' },
  { key: 'withdrawn', label: 'Withdrawn' },
  { key: 'unchanged', label: 'Unchanged' },
  { key: 'stale', label: 'Older version (ignored)' },
  { key: 'skipped', label: 'Withdrawn before import (skipped)' },
];

/**
 * Upload a RaceHub Entry Export, preview the dry run, map any unmapped classes, then confirm (L8, #16).
 */
export default function RaceHubImportDialog({ eventId, classes, open, onOpenChange }: RaceHubImportDialogProps) {
  const [exportDocument, setExportDocument] = useState<unknown>(null);
  const [fileError, setFileError] = useState<string | null>(null);
  const [preview, setPreview] = useState<RaceHubImportResult | null>(null);
  const [classChoices, setClassChoices] = useState<Record<string, string>>({});

  const importMutation = useRaceHubImport(eventId);
  const replaceMappings = useReplaceRaceHubClassMappings(eventId);
  const { data: mappings = [] } = useRaceHubClassMappings(eventId, open);
  const { data: racingClasses = [] } = useRacingClasses();

  function handleOpenChange(next: boolean) {
    if (!next) {
      setExportDocument(null);
      setFileError(null);
      setPreview(null);
      setClassChoices({});
    }
    onOpenChange(next);
  }

  function classLabel(cls: EventClassDto) {
    const racingClass = racingClasses.find(rc => rc.id === cls.racingClassId);
    return racingClass?.name ?? `Class ${cls.id}`;
  }

  async function runPreview(document: unknown) {
    try {
      setPreview(await importMutation.mutateAsync({ exportDocument: document, dryRun: true }));
    } catch {
      setPreview(null);
      setFileError('RaceHub could not check this file. Make sure it is an Entry Export (schema version 1).');
    }
  }

  async function handleFile(file: File | undefined) {
    setPreview(null);
    setFileError(null);
    setClassChoices({});
    if (!file) return;
    let parsed: unknown;
    try {
      parsed = JSON.parse(await file.text());
    } catch {
      setExportDocument(null);
      setFileError('This file is not valid JSON.');
      return;
    }
    setExportDocument(parsed);
    await runPreview(parsed);
  }

  async function saveMappingsAndRecheck() {
    const chosen = Object.entries(classChoices).filter(([, eventClassId]) => eventClassId);
    const byRaceHubId = new Map(mappings.map(m => [m.racehubEventClassId, m.eventClassId]));
    chosen.forEach(([racehubId, eventClassId]) => byRaceHubId.set(racehubId, Number(eventClassId)));
    try {
      await replaceMappings.mutateAsync(
        [...byRaceHubId].map(([racehubEventClassId, eventClassId]) => ({ racehubEventClassId, eventClassId })),
      );
      setClassChoices({});
      await runPreview(exportDocument);
    } catch {
      toast.error('Could not save the class mappings. Try again.');
    }
  }

  async function confirmImport() {
    try {
      const result = await importMutation.mutateAsync({ exportDocument, dryRun: false });
      if (!result.applied) {
        setPreview(result);
        toast.error('The import is blocked. Fix the problems listed and try again.');
        return;
      }
      const { created, updated, withdrawn } = result.summary;
      toast.success(`Imported from RaceHub: ${created} new, ${updated} updated, ${withdrawn} withdrawn.`);
      handleOpenChange(false);
    } catch {
      toast.error('The import failed. Check your connection and try again.');
    }
  }

  const unmapped = preview?.unmappedClasses ?? [];
  const allUnmappedChosen = unmapped.length > 0 && unmapped.every(u => classChoices[u.racehubEventClassId]);
  const busy = importMutation.isPending || replaceMappings.isPending;

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Import entries from RaceHub</DialogTitle>
          <DialogDescription>
            Choose the Entry Export file downloaded from RaceHub. You will see what changes before anything is saved.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-1.5">
          <Label htmlFor="racehub-file">Entry Export file</Label>
          <input
            id="racehub-file"
            type="file"
            accept=".json,application/json"
            onChange={e => void handleFile(e.target.files?.[0])}
            className="block w-full text-sm file:mr-3 file:rounded-md file:border file:bg-background file:px-3 file:py-1.5 file:text-sm"
          />
          {fileError && <p className="text-xs text-destructive">{fileError}</p>}
        </div>

        {importMutation.isPending && !preview && (
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Loader2 className="h-4 w-4 animate-spin" /> Checking the file…
          </div>
        )}

        {preview && (
          <div className="space-y-4" data-testid="racehub-preview">
            <p className="text-sm text-muted-foreground">
              {preview.racehubEventName ?? 'RaceHub event'}
              {preview.revision != null && <> · revision {preview.revision}</>}
            </p>

            <dl className="grid grid-cols-2 sm:grid-cols-3 gap-2">
              {SUMMARY_LABELS.map(({ key, label }) => (
                <div key={key} className="rounded-md border px-3 py-2">
                  <dt className="text-xs text-muted-foreground">{label}</dt>
                  <dd className="text-lg font-semibold" data-testid={`summary-${key}`}>
                    {preview.summary[key]}
                  </dd>
                </div>
              ))}
            </dl>

            {preview.errors.length > 0 && (
              <div className="rounded-md border border-destructive/50 p-3">
                <h3 className="text-sm font-semibold text-destructive mb-1">Problems that block the import</h3>
                <ul className="list-disc pl-5 text-sm space-y-0.5">
                  {preview.errors.map(e => <li key={e}>{e}</li>)}
                </ul>
              </div>
            )}

            {unmapped.length > 0 && (
              <div className="rounded-md border border-amber-500/50 p-3 space-y-3">
                <div>
                  <h3 className="text-sm font-semibold">Classes to map</h3>
                  <p className="text-xs text-muted-foreground">
                    These RaceHub classes do not match a class in this event. Choose one for each.
                  </p>
                </div>
                {unmapped.map(u => (
                  <div key={u.racehubEventClassId} className="flex flex-col sm:flex-row sm:items-center gap-2">
                    <div className="flex-1 text-sm">
                      <span className="font-medium">{u.rcClassName ?? u.className ?? u.racehubEventClassId}</span>
                      <span className="text-muted-foreground">
                        {' '}· {u.entryCount} {u.entryCount === 1 ? 'entry' : 'entries'}
                      </span>
                    </div>
                    <Select
                      value={classChoices[u.racehubEventClassId] ?? ''}
                      onValueChange={v => setClassChoices(c => ({ ...c, [u.racehubEventClassId]: v }))}
                    >
                      <SelectTrigger
                        className="sm:w-56"
                        aria-label={`Event class for ${u.rcClassName ?? u.racehubEventClassId}`}
                      >
                        <SelectValue placeholder="Choose a class" />
                      </SelectTrigger>
                      <SelectContent>
                        {classes.map(cls => (
                          <SelectItem key={cls.id} value={String(cls.id)}>
                            {classLabel(cls)}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                ))}
                <Button size="sm" onClick={() => void saveMappingsAndRecheck()} disabled={!allUnmappedChosen || busy}>
                  Save mappings and check again
                </Button>
              </div>
            )}

            {preview.warnings.length > 0 && (
              <div className="rounded-md border p-3">
                <h3 className="text-sm font-semibold mb-1">Warnings</h3>
                <ul className="list-disc pl-5 text-sm space-y-0.5">
                  {preview.warnings.map(w => <li key={w}>{w}</li>)}
                </ul>
              </div>
            )}
          </div>
        )}

        <DialogFooter>
          <Button variant="outline" onClick={() => handleOpenChange(false)}>
            Cancel
          </Button>
          <Button onClick={() => void confirmImport()} disabled={!preview || preview.blocked || busy}>
            {importMutation.isPending && preview && <Loader2 className="h-4 w-4 mr-1 animate-spin" />}
            Import entries
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
