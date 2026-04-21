import { useState } from 'react';
import { Plus } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';

import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Checkbox } from '@/components/ui/checkbox';
import { Separator } from '@/components/ui/separator';

import {
  useAddEventClass,
  useCombineClasses,
  useUpdateEventClassOverrides,
} from '@/hooks/admin/useAdminEventClasses';
import type { EventClassDto } from '@/lib/adminApi';

// ── Add class form ─────────────────────────────────────────────────────────

const addClassSchema = z.object({
  racingClassId: z.coerce.number().int().positive('Racing class is required'),
  templateId: z.coerce.number().int().positive('Format template is required'),
});
type AddClassFormValues = z.infer<typeof addClassSchema>;

// ── Config snapshot summary ────────────────────────────────────────────────

function ConfigSummary({ config }: { config: EventClassDto['configSnapshot'] }) {
  if (!config) return <span className="text-muted-foreground text-xs">No config</span>;

  const entries: string[] = [];
  if (typeof config.type === 'string') entries.push(`Type: ${config.type}`);
  if (typeof config.durationMinutes === 'number')
    entries.push(`${config.durationMinutes} min`);
  if (typeof config.laps === 'number') entries.push(`${config.laps} laps`);
  if (typeof config.heats === 'number') entries.push(`${config.heats} heats`);

  return <span className="text-xs text-muted-foreground">{entries.join(' · ') || 'Custom'}</span>;
}

// ── Override editor dialog ─────────────────────────────────────────────────

function OverrideEditorDialog({
  eventId,
  classId,
  currentOverride,
  open,
  onOpenChange,
}: {
  eventId: number;
  classId: number;
  currentOverride: Record<string, unknown> | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const updateOverrides = useUpdateEventClassOverrides(eventId);
  const [raw, setRaw] = useState(
    currentOverride ? JSON.stringify(currentOverride, null, 2) : '{}'
  );
  const [parseError, setParseError] = useState<string | null>(null);

  async function handleSave() {
    let parsed: Record<string, unknown>;
    try {
      parsed = JSON.parse(raw) as Record<string, unknown>;
      setParseError(null);
    } catch {
      setParseError('Invalid JSON — check the syntax and try again.');
      return;
    }
    try {
      await updateOverrides.mutateAsync({ classId, override: parsed });
      toast.success('Overrides saved');
      onOpenChange(false);
    } catch {
      toast.error('One or more overrides are invalid for this format type. Check the highlighted fields.');
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Edit Format Overrides</DialogTitle>
          <DialogDescription>
            Enter override fields as a JSON object. These values replace the corresponding
            template defaults for this class only.
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-2">
          <textarea
            value={raw}
            onChange={e => setRaw(e.target.value)}
            rows={8}
            className="w-full rounded-md border bg-background px-3 py-2 text-sm font-mono resize-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            spellCheck={false}
          />
          {parseError && <p className="text-xs text-destructive">{parseError}</p>}
        </div>
        <DialogFooter>
          <Button onClick={handleSave} disabled={updateOverrides.isPending}>
            {updateOverrides.isPending ? 'Saving…' : 'Save Overrides'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

// ── Combine confirm dialog ─────────────────────────────────────────────────

function CombineConfirmDialog({
  eventId,
  selectedIds,
  open,
  onOpenChange,
}: {
  eventId: number;
  selectedIds: number[];
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const combineClasses = useCombineClasses(eventId);

  async function handleConfirm() {
    try {
      await combineClasses.mutateAsync(selectedIds);
      toast.success('Classes combined into a shared race');
      onOpenChange(false);
    } catch {
      toast.error('Could not combine classes. Try again.');
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Combine into shared race?</DialogTitle>
          <DialogDescription>
            These classes will race together but score separately. This cannot be undone without
            removing and re-adding the classes.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button onClick={handleConfirm} disabled={combineClasses.isPending}>
            {combineClasses.isPending ? 'Combining…' : 'Combine Classes'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

// ── Main component ─────────────────────────────────────────────────────────

interface EventClassSectionProps {
  eventId: number;
  classes: EventClassDto[];
}

export default function EventClassSection({ eventId, classes }: EventClassSectionProps) {
  const addEventClass = useAddEventClass(eventId);

  const [addOpen, setAddOpen] = useState(false);
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [combineOpen, setCombineOpen] = useState(false);
  const [overrideState, setOverrideState] = useState<{
    open: boolean;
    classId: number;
    currentOverride: Record<string, unknown> | null;
  }>({ open: false, classId: 0, currentOverride: null });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<AddClassFormValues>({ resolver: zodResolver(addClassSchema) });

  async function onAddClass(values: AddClassFormValues) {
    try {
      await addEventClass.mutateAsync({
        racingClassId: values.racingClassId,
        templateId: values.templateId,
      });
      toast.success('Class added');
      setAddOpen(false);
      reset();
    } catch {
      toast.error('Could not add class. Try again.');
    }
  }

  function toggleSelect(id: number) {
    setSelectedIds(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  if (classes.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-12 text-center">
        <h3 className="text-base font-semibold mb-1">No classes added</h3>
        <p className="text-sm text-muted-foreground mb-4">
          Add a racing class to configure entry groups for this event.
        </p>
        <Button size="sm" onClick={() => setAddOpen(true)}>
          <Plus className="h-4 w-4 mr-1" />
          Add Class
        </Button>
        <AddClassDialog
          open={addOpen}
          onOpenChange={setAddOpen}
          onSubmit={handleSubmit(onAddClass)}
          register={register}
          errors={errors}
          isSubmitting={isSubmitting || addEventClass.isPending}
        />
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h3 className="text-base font-semibold">Classes</h3>
        <div className="flex items-center gap-2">
          {selectedIds.size >= 2 && (
            <Button
              size="sm"
              variant="outline"
              onClick={() => setCombineOpen(true)}
            >
              Combine into Shared Race ({selectedIds.size})
            </Button>
          )}
          <Button size="sm" onClick={() => setAddOpen(true)}>
            <Plus className="h-4 w-4 mr-1" />
            Add Class
          </Button>
        </div>
      </div>

      {/* Class cards */}
      <div className="space-y-3">
        {classes.map(cls => (
          <div
            key={cls.id}
            className="rounded-lg border bg-card p-4 flex items-start gap-3"
          >
            <Checkbox
              id={`cls-${cls.id}`}
              checked={selectedIds.has(cls.id)}
              onCheckedChange={() => toggleSelect(cls.id)}
              aria-label={`Select class ${cls.id} for combining`}
              className="mt-0.5"
            />
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="font-medium text-sm">
                  {/* Racing class name would be resolved via a query — using ID for now */}
                  Class {cls.racingClassId ?? cls.id}
                </span>
                {cls.configOverride && Object.keys(cls.configOverride).length > 0 && (
                  <Badge className="bg-amber-100 text-amber-700 text-xs">
                    Overrides applied
                  </Badge>
                )}
                {cls.combinedRaceGroup !== null && (
                  <Badge variant="secondary" className="text-xs">
                    Combined: group {cls.combinedRaceGroup}
                  </Badge>
                )}
              </div>
              <div className="mt-1">
                <ConfigSummary config={cls.configSnapshot} />
              </div>
            </div>
            <Button
              size="sm"
              variant="outline"
              onClick={() =>
                setOverrideState({
                  open: true,
                  classId: cls.id,
                  currentOverride: cls.configOverride,
                })
              }
            >
              Edit Overrides
            </Button>
          </div>
        ))}
      </div>

      <Separator />

      {/* Dialogs */}
      <AddClassDialog
        open={addOpen}
        onOpenChange={setAddOpen}
        onSubmit={handleSubmit(onAddClass)}
        register={register}
        errors={errors}
        isSubmitting={isSubmitting || addEventClass.isPending}
      />

      <OverrideEditorDialog
        eventId={eventId}
        classId={overrideState.classId}
        currentOverride={overrideState.currentOverride}
        open={overrideState.open}
        onOpenChange={open => setOverrideState(s => ({ ...s, open }))}
      />

      <CombineConfirmDialog
        eventId={eventId}
        selectedIds={[...selectedIds]}
        open={combineOpen}
        onOpenChange={open => {
          setCombineOpen(open);
          if (!open) setSelectedIds(new Set());
        }}
      />
    </div>
  );
}

// ── Add class dialog (extracted to avoid inline JSX with hook form) ────────

function AddClassDialog({
  open,
  onOpenChange,
  onSubmit,
  register,
  errors,
  isSubmitting,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSubmit: (e?: React.BaseSyntheticEvent) => Promise<void>;
  register: ReturnType<typeof useForm<AddClassFormValues>>['register'];
  errors: ReturnType<typeof useForm<AddClassFormValues>>['formState']['errors'];
  isSubmitting: boolean;
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Add Class</DialogTitle>
          <DialogDescription>
            {/* TODO Plan 06: populate racing class and template selects from API */}
            Enter the racing class ID and format template ID.
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={onSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="racingClassId">Racing Class ID</Label>
            <Input
              id="racingClassId"
              type="number"
              {...register('racingClassId')}
              placeholder="e.g. 1"
            />
            {errors.racingClassId && (
              <p className="text-xs text-destructive">{errors.racingClassId.message}</p>
            )}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="templateId">Format Template ID</Label>
            <Input
              id="templateId"
              type="number"
              {...register('templateId')}
              placeholder="e.g. 1"
            />
            {errors.templateId && (
              <p className="text-xs text-destructive">{errors.templateId.message}</p>
            )}
          </div>
          <DialogFooter>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Adding…' : 'Add Class'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
