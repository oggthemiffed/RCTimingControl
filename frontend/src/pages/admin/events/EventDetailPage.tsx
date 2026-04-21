import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Loader2 } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';
import axios from 'axios';

import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
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

import {
  useAdminEventDetail,
  useUpdateAdminEvent,
  useTransitionEvent,
} from '@/hooks/admin/useAdminEvents';
import type { EventStatus } from '@/lib/adminApi';
import EventClassSection from './EventClassSection';
import EntryListSection from './EntryListSection';

// ── Status colors (D-06 / UI-SPEC.md) ─────────────────────────────────────

const statusColor: Record<EventStatus, string> = {
  DRAFT: 'bg-muted text-muted-foreground',
  PUBLISHED: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300',
  OPEN: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300',
  ENTRIES_CLOSED: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300',
  IN_PROGRESS: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300',
  COMPLETED: 'bg-neutral-800 text-neutral-100 dark:bg-neutral-200 dark:text-neutral-900',
};

const statusLabel: Record<EventStatus, string> = {
  DRAFT: 'Draft',
  PUBLISHED: 'Published',
  OPEN: 'Open',
  ENTRIES_CLOSED: 'Entries Closed',
  IN_PROGRESS: 'In Progress',
  COMPLETED: 'Completed',
};

// ── Valid state transitions (D-04) ────────────────────────────────────────

const VALID_NEXT: Record<EventStatus, EventStatus[]> = {
  DRAFT: ['PUBLISHED'],
  PUBLISHED: ['OPEN'],
  OPEN: ['ENTRIES_CLOSED'],
  ENTRIES_CLOSED: ['IN_PROGRESS'],
  IN_PROGRESS: ['COMPLETED'],
  COMPLETED: [],
};

// ── Transition button labels (D-04 / UI-SPEC.md Copywriting Contract) ─────

const transitionLabel: Record<EventStatus, string> = {
  DRAFT: 'Publish Event',
  PUBLISHED: 'Open Entries',
  OPEN: 'Close Entries',
  ENTRIES_CLOSED: 'Start Event',
  IN_PROGRESS: 'Complete Event',
  COMPLETED: '',
};

// ── Destructive transitions (require special button variant) ──────────────

const isDestructiveTransition = (target: EventStatus): boolean =>
  target === 'ENTRIES_CLOSED';

// ── Confirm dialog copy (D-05 / UI-SPEC.md) ──────────────────────────────

type ConfirmCopy = { title: string; body: string; confirmLabel: string; destructive: boolean };

const transitionConfirmCopy: Partial<Record<EventStatus, ConfirmCopy>> = {
  ENTRIES_CLOSED: {
    title: 'Close entries?',
    body: 'This will prevent new entries from being submitted. Racers already entered will not be affected.',
    confirmLabel: 'Close Entries',
    destructive: true,
  },
  COMPLETED: {
    title: 'Mark event as completed?',
    body: 'This will finalise the event. Results will be published once race data is available.',
    confirmLabel: 'Complete Event',
    destructive: false,
  },
};

// ── Edit event form ────────────────────────────────────────────────────────

const editEventSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  eventDate: z.string().min(1, 'Date is required'),
});
type EditEventFormValues = z.infer<typeof editEventSchema>;

// ── Component ─────────────────────────────────────────────────────────────

export default function EventDetailPage() {
  const { id: idParam } = useParams<{ id: string }>();
  const id = Number(idParam);
  const navigate = useNavigate();

  const { data, isLoading, isError, refetch } = useAdminEventDetail(id);
  const updateEvent = useUpdateAdminEvent(id);
  const transitionMutation = useTransitionEvent(id);

  const [transitionTarget, setTransitionTarget] = useState<EventStatus | null>(null);
  const [confirmOpen, setConfirmOpen] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isDirty, isSubmitting },
    reset,
  } = useForm<EditEventFormValues>({
    resolver: zodResolver(editEventSchema),
    values: data ? { name: data.name, eventDate: data.eventDate } : undefined,
  });

  // ── Transition handlers ──────────────────────────────────────────────

  function requestTransition(target: EventStatus) {
    setTransitionTarget(target);
    // If this transition has confirm copy, show dialog; otherwise fire immediately
    if (transitionConfirmCopy[target]) {
      setConfirmOpen(true);
    } else {
      fireTransition(target);
    }
  }

  async function fireTransition(target: EventStatus) {
    try {
      await transitionMutation.mutateAsync(target);
      toast.success(`Event is now ${statusLabel[target]}`);
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.status === 409) {
        // Invalidate and show toast per UI-SPEC
        void refetch();
        toast.error('This transition is no longer valid. Refresh the page to see the current event status.');
      } else {
        toast.error('Event could not be updated. Check your connection and try again.');
      }
    }
  }

  async function confirmTransition() {
    if (!transitionTarget) return;
    setConfirmOpen(false);
    await fireTransition(transitionTarget);
    setTransitionTarget(null);
  }

  // ── Edit overview form submit ────────────────────────────────────────

  async function onEditSubmit(values: EditEventFormValues) {
    try {
      await updateEvent.mutateAsync({
        name: values.name,
        eventDate: values.eventDate,
        trackId: data?.trackId ?? null,
      });
      toast.success('Event details saved');
      reset(values);
    } catch {
      toast.error('Event could not be created. Check your connection and try again.');
    }
  }

  // ── Render ───────────────────────────────────────────────────────────

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (isError || !data) {
    return (
      <div className="flex flex-col items-center justify-center py-20 text-center gap-4">
        <p className="text-muted-foreground">Failed to load event.</p>
        <Button variant="outline" onClick={() => void refetch()}>
          Retry
        </Button>
        <Button variant="ghost" onClick={() => navigate('/admin/events')}>
          <ArrowLeft className="h-4 w-4 mr-1" />
          Back to Events
        </Button>
      </div>
    );
  }

  const validNextStatuses = VALID_NEXT[data.status];
  const confirmCopy = transitionTarget ? transitionConfirmCopy[transitionTarget] : null;
  const canEditDetails = data.status === 'DRAFT';

  return (
    <div>
      {/* Back nav */}
      <Button
        variant="ghost"
        size="sm"
        className="mb-4 -ml-1"
        onClick={() => navigate('/admin/events')}
      >
        <ArrowLeft className="h-4 w-4 mr-1" />
        Events
      </Button>

      {/* Page header */}
      <div className="flex flex-col sm:flex-row sm:items-center gap-3 mb-6">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-3 flex-wrap">
            <h1 className="text-2xl font-semibold truncate">{data.name}</h1>
            <Badge className={statusColor[data.status]}>{statusLabel[data.status]}</Badge>
          </div>
          <p className="text-sm text-muted-foreground mt-1">
            {new Intl.DateTimeFormat('en-GB', { dateStyle: 'long' }).format(
              new Date(data.eventDate)
            )}
            {data.trackId !== null && <span> · Track {data.trackId}</span>}
          </p>
        </div>

        {/* State-machine action buttons (D-04) */}
        {validNextStatuses.length > 0 && (
          <div className="flex gap-2 flex-wrap">
            {validNextStatuses.map(target => (
              <Button
                key={target}
                variant={isDestructiveTransition(target) ? 'destructive' : 'default'}
                size="sm"
                disabled={transitionMutation.isPending}
                onClick={() => requestTransition(target)}
              >
                {transitionMutation.isPending && (
                  <Loader2 className="h-4 w-4 mr-1 animate-spin" />
                )}
                {transitionLabel[data.status]}
              </Button>
            ))}
          </div>
        )}
      </div>

      {/* Tabs */}
      <Tabs defaultValue="overview">
        <TabsList>
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="classes">
            Classes ({data.classes.length})
          </TabsTrigger>
          <TabsTrigger value="entries">Entries</TabsTrigger>
        </TabsList>

        {/* Overview tab */}
        <TabsContent value="overview" className="mt-4">
          <form onSubmit={handleSubmit(onEditSubmit)} className="space-y-4 max-w-md">
            <div className="space-y-1.5">
              <Label htmlFor="ov-name">Event Name</Label>
              <Input
                id="ov-name"
                {...register('name')}
                disabled={!canEditDetails}
                aria-describedby={errors.name ? 'ov-name-err' : undefined}
              />
              {errors.name && (
                <p id="ov-name-err" className="text-xs text-destructive">
                  {errors.name.message}
                </p>
              )}
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="ov-date">Date</Label>
              <Input
                id="ov-date"
                type="date"
                {...register('eventDate')}
                disabled={!canEditDetails}
              />
              {errors.eventDate && (
                <p className="text-xs text-destructive">{errors.eventDate.message}</p>
              )}
            </div>
            {/* TODO Plan 06: track select from API */}
            {canEditDetails && (
              <Button
                type="submit"
                disabled={!isDirty || isSubmitting || updateEvent.isPending}
              >
                {updateEvent.isPending ? 'Saving…' : 'Save Event Details'}
              </Button>
            )}
            {!canEditDetails && (
              <p className="text-xs text-muted-foreground">
                Event details can only be edited while the event is in Draft status.
              </p>
            )}
          </form>
        </TabsContent>

        {/* Classes tab */}
        <TabsContent value="classes" className="mt-4">
          <EventClassSection eventId={id} classes={data.classes} />
        </TabsContent>

        {/* Entries tab */}
        <TabsContent value="entries" className="mt-4">
          <EntryListSection eventId={id} classes={data.classes} />
        </TabsContent>
      </Tabs>

      {/* Transition confirm dialog (D-05) */}
      {confirmCopy && (
        <Dialog open={confirmOpen} onOpenChange={setConfirmOpen}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>{confirmCopy.title}</DialogTitle>
              <DialogDescription>{confirmCopy.body}</DialogDescription>
            </DialogHeader>
            <DialogFooter>
              <Button variant="outline" onClick={() => setConfirmOpen(false)}>
                Cancel
              </Button>
              <Button
                variant={confirmCopy.destructive ? 'destructive' : 'default'}
                onClick={() => void confirmTransition()}
                disabled={transitionMutation.isPending}
              >
                {transitionMutation.isPending ? (
                  <Loader2 className="h-4 w-4 mr-1 animate-spin" />
                ) : null}
                {confirmCopy.confirmLabel}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      )}
    </div>
  );
}
