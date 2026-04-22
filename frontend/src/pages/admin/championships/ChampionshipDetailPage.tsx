import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Loader2, Plus, Trash2 } from 'lucide-react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';
import axios from 'axios';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog';
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
import { ChampionshipConfigForm } from './ChampionshipConfigForm';
import { PointsScaleEditor } from './PointsScaleEditor';
import { ChampionshipStandingsTable } from './ChampionshipStandingsTable';
import {
  useChampionshipDetail,
  useUpdateChampionship,
  useAddChampionshipClass,
  useRemoveChampionshipClass,
  useLinkChampionshipEvent,
  useUnlinkChampionshipEvent,
  useChampionshipExclusions,
  useCreateExclusion,
  useDeleteExclusion,
} from '@/hooks/admin/useAdminChampionships';
import { useAdminEventsList } from '@/hooks/admin/useAdminEvents';
import type { ChampionshipDto } from '@/lib/adminApi';
import { adminApi } from '@/lib/adminApi';
import { useQuery } from '@tanstack/react-query';
import { adminQueryKeys } from '@/hooks/admin/adminQueryKeys';

// ── Add class dialog ───────────────────────────────────────────────────────

const addClassSchema = z.object({
  racingClassId: z.coerce.number().int().positive('Racing class is required'),
  bestXFromYX: z.coerce.number().int().positive().nullable(),
  bestXFromYY: z.coerce.number().int().positive().nullable(),
});
type AddClassFormValues = z.infer<typeof addClassSchema>;

function AddClassDialog({
  championshipId,
  open,
  onOpenChange,
}: {
  championshipId: number;
  open: boolean;
  onOpenChange: (v: boolean) => void;
}) {
  const { data: racingClasses = [], isLoading } = useQuery({
    queryKey: adminQueryKeys.racingClasses.all(),
    queryFn: adminApi.listRacingClasses,
  });
  const addMutation = useAddChampionshipClass(championshipId);
  const { register, handleSubmit, control, reset, formState: { errors, isSubmitting } } =
    useForm<AddClassFormValues>({ resolver: zodResolver(addClassSchema) });

  async function onSubmit(values: AddClassFormValues) {
    try {
      await addMutation.mutateAsync({
        racingClassId: values.racingClassId,
        bestXFromYX: values.bestXFromYX,
        bestXFromYY: values.bestXFromYY,
      });
      toast.success('Class added');
      reset();
      onOpenChange(false);
    } catch {
      toast.error('Could not add class. Try again.');
    }
  }

  return (
    <Dialog open={open} onOpenChange={v => { onOpenChange(v); if (!v) reset(); }}>
      <DialogContent>
        <DialogHeader><DialogTitle>Add Class</DialogTitle></DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-1.5">
            <Label>Racing Class</Label>
            <Controller
              name="racingClassId"
              control={control}
              render={({ field }) => (
                <Select
                  disabled={isLoading}
                  value={field.value ? String(field.value) : ''}
                  onValueChange={v => field.onChange(Number(v))}
                >
                  <SelectTrigger><SelectValue placeholder="Select a class" /></SelectTrigger>
                  <SelectContent>
                    {racingClasses.map(rc => (
                      <SelectItem key={rc.id} value={String(rc.id)}>{rc.name}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
            {errors.racingClassId && <p className="text-xs text-destructive">{errors.racingClassId.message}</p>}
          </div>
          <div className="space-y-1.5">
            <Label>Per-class Best X/Y override (optional)</Label>
            <div className="flex items-center gap-2">
              <Input type="number" className="w-20" placeholder="X" {...register('bestXFromYX')} />
              <span className="text-sm text-muted-foreground">from</span>
              <Input type="number" className="w-20" placeholder="Y" {...register('bestXFromYY')} />
            </div>
          </div>
          <DialogFooter>
            <Button type="submit" disabled={isSubmitting || addMutation.isPending}>
              {addMutation.isPending ? 'Adding…' : 'Add Class'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

// ── Link event dialog ──────────────────────────────────────────────────────

const linkEventSchema = z.object({
  eventId: z.coerce.number().int().positive('Event is required'),
  roundNumber: z.coerce.number().int().positive('Round number is required'),
});
type LinkEventFormValues = z.infer<typeof linkEventSchema>;

function LinkEventDialog({
  championshipId,
  open,
  onOpenChange,
}: {
  championshipId: number;
  open: boolean;
  onOpenChange: (v: boolean) => void;
}) {
  const { data: events = [], isLoading } = useAdminEventsList();
  const linkMutation = useLinkChampionshipEvent(championshipId);
  const { register, handleSubmit, control, reset, formState: { errors, isSubmitting } } =
    useForm<LinkEventFormValues>({ resolver: zodResolver(linkEventSchema) });

  async function onSubmit(values: LinkEventFormValues) {
    try {
      await linkMutation.mutateAsync({ eventId: values.eventId, roundNumber: values.roundNumber });
      toast.success('Event linked');
      reset();
      onOpenChange(false);
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.status === 409) {
        toast.error(`Round ${values.roundNumber} is already assigned to another event.`);
      } else {
        toast.error('Could not link event. Try again.');
      }
    }
  }

  return (
    <Dialog open={open} onOpenChange={v => { onOpenChange(v); if (!v) reset(); }}>
      <DialogContent>
        <DialogHeader><DialogTitle>Link Event</DialogTitle></DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-1.5">
            <Label>Event</Label>
            <Controller
              name="eventId"
              control={control}
              render={({ field }) => (
                <Select
                  disabled={isLoading}
                  value={field.value ? String(field.value) : ''}
                  onValueChange={v => field.onChange(Number(v))}
                >
                  <SelectTrigger><SelectValue placeholder="Select an event" /></SelectTrigger>
                  <SelectContent>
                    {events.map(e => (
                      <SelectItem key={e.id} value={String(e.id)}>{e.name}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
            {errors.eventId && <p className="text-xs text-destructive">{errors.eventId.message}</p>}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="round-num">Round Number</Label>
            <Input id="round-num" type="number" min={1} {...register('roundNumber')} />
            {errors.roundNumber && <p className="text-xs text-destructive">{errors.roundNumber.message}</p>}
          </div>
          <DialogFooter>
            <Button type="submit" disabled={isSubmitting || linkMutation.isPending}>
              {linkMutation.isPending ? 'Linking…' : 'Link Event'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

// ── Create exclusion dialog ────────────────────────────────────────────────

const exclusionSchema = z.object({
  driverId: z.coerce.number().int().positive('Driver ID is required'),
  eventId: z.coerce.number().int().positive('Event is required'),
  reason: z.string().min(1, 'Reason is required'),
});
type ExclusionFormValues = z.infer<typeof exclusionSchema>;

function CreateExclusionDialog({
  championshipId,
  open,
  onOpenChange,
}: {
  championshipId: number;
  open: boolean;
  onOpenChange: (v: boolean) => void;
}) {
  const { data: events = [] } = useAdminEventsList();
  const createMutation = useCreateExclusion(championshipId);
  const { register, handleSubmit, control, reset, formState: { errors, isSubmitting } } =
    useForm<ExclusionFormValues>({ resolver: zodResolver(exclusionSchema) });

  async function onSubmit(values: ExclusionFormValues) {
    try {
      await createMutation.mutateAsync(values);
      toast.success('Exclusion created');
      reset();
      onOpenChange(false);
    } catch {
      toast.error('Could not create exclusion. Try again.');
    }
  }

  return (
    <Dialog open={open} onOpenChange={v => { onOpenChange(v); if (!v) reset(); }}>
      <DialogContent>
        <DialogHeader><DialogTitle>Add Exclusion</DialogTitle></DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="exc-driver">Driver ID</Label>
            <Input id="exc-driver" type="number" min={1} {...register('driverId')} />
            {errors.driverId && <p className="text-xs text-destructive">{errors.driverId.message}</p>}
          </div>
          <div className="space-y-1.5">
            <Label>Event</Label>
            <Controller
              name="eventId"
              control={control}
              render={({ field }) => (
                <Select
                  value={field.value ? String(field.value) : ''}
                  onValueChange={v => field.onChange(Number(v))}
                >
                  <SelectTrigger><SelectValue placeholder="Select an event" /></SelectTrigger>
                  <SelectContent>
                    {events.map(e => (
                      <SelectItem key={e.id} value={String(e.id)}>{e.name}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
            {errors.eventId && <p className="text-xs text-destructive">{errors.eventId.message}</p>}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="exc-reason">Reason</Label>
            <textarea
              id="exc-reason"
              {...register('reason')}
              rows={3}
              className="w-full rounded-md border bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring resize-none"
              placeholder="e.g. Illegal motor found at post-race scrutineering"
            />
            {errors.reason && <p className="text-xs text-destructive">{errors.reason.message}</p>}
          </div>
          <DialogFooter>
            <Button type="submit" disabled={isSubmitting || createMutation.isPending}>
              {createMutation.isPending ? 'Creating…' : 'Create Exclusion'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

// ── Main component ─────────────────────────────────────────────────────────

export default function ChampionshipDetailPage() {
  const { id: idParam } = useParams<{ id: string }>();
  const id = Number(idParam);
  const navigate = useNavigate();

  const { data, isLoading, isError, refetch } = useChampionshipDetail(id);
  const updateMutation = useUpdateChampionship(id);
  const removeClassMutation = useRemoveChampionshipClass(id);
  const unlinkEventMutation = useUnlinkChampionshipEvent(id);
  const { data: exclusions = [], isLoading: exclusionsLoading } = useChampionshipExclusions(id);
  const deleteExclusionMutation = useDeleteExclusion(id);

  const { data: racingClasses = [] } = useQuery({
    queryKey: adminQueryKeys.racingClasses.all(),
    queryFn: adminApi.listRacingClasses,
  });
  const { data: events = [] } = useAdminEventsList();

  const [addClassOpen, setAddClassOpen] = useState(false);
  const [linkEventOpen, setLinkEventOpen] = useState(false);
  const [createExclusionOpen, setCreateExclusionOpen] = useState(false);

  async function handleUpdate(body: Omit<ChampionshipDto, 'id'>) {
    await updateMutation.mutateAsync(body);
    toast.success('Championship updated');
  }

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
        <p className="text-muted-foreground">Failed to load championship.</p>
        <Button variant="outline" onClick={() => void refetch()}>Retry</Button>
        <Button variant="ghost" onClick={() => navigate('/admin/championships')}>
          <ArrowLeft className="h-4 w-4 mr-1" />
          Back to Championships
        </Button>
      </div>
    );
  }

  return (
    <div>
      <Button variant="ghost" size="sm" className="mb-4 -ml-1" onClick={() => navigate('/admin/championships')}>
        <ArrowLeft className="h-4 w-4 mr-1" />
        Championships
      </Button>

      <h1 className="text-2xl font-semibold mb-6">{data.name}</h1>

      <Tabs defaultValue="config">
        <TabsList>
          <TabsTrigger value="config">Config</TabsTrigger>
          <TabsTrigger value="classes">Classes ({data.classes.length})</TabsTrigger>
          <TabsTrigger value="events">Events ({data.events.length})</TabsTrigger>
          <TabsTrigger value="points-scale">Points Scale</TabsTrigger>
          <TabsTrigger value="standings">Standings</TabsTrigger>
          <TabsTrigger value="exclusions">Exclusions ({exclusions.length})</TabsTrigger>
        </TabsList>

        {/* Config tab */}
        <TabsContent value="config" className="mt-4">
          <ChampionshipConfigForm
            initialValue={data}
            onSubmit={handleUpdate}
            submitLabel="Save"
          />
        </TabsContent>

        {/* Classes tab */}
        <TabsContent value="classes" className="mt-4">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-base font-semibold">Racing Classes</h2>
            <Button size="sm" onClick={() => setAddClassOpen(true)}>
              <Plus className="h-4 w-4 mr-1" />
              Add Class
            </Button>
          </div>
          {data.classes.length === 0 ? (
            <p className="text-sm text-muted-foreground py-4">No classes added yet.</p>
          ) : (
            <div className="rounded-lg border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Class</TableHead>
                    <TableHead>Best X/Y Override</TableHead>
                    <TableHead className="w-16" />
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.classes.map(cls => {
                    const rcName = racingClasses.find(rc => rc.id === cls.racingClassId)?.name
                      ?? `Class ${cls.racingClassId}`;
                    return (
                      <TableRow key={cls.id}>
                        <TableCell>{rcName}</TableCell>
                        <TableCell>
                          {cls.bestXFromYX != null && cls.bestXFromYY != null
                            ? `${cls.bestXFromYX}/${cls.bestXFromYY}`
                            : <span className="text-muted-foreground text-sm">Inherit</span>}
                        </TableCell>
                        <TableCell>
                          <Button
                            variant="ghost"
                            size="icon-sm"
                            disabled={removeClassMutation.isPending}
                            onClick={async () => {
                              try {
                                await removeClassMutation.mutateAsync(cls.racingClassId);
                                toast.success('Class removed');
                              } catch {
                                toast.error('Could not remove class.');
                              }
                            }}
                          >
                            <Trash2 className="h-4 w-4 text-muted-foreground" />
                          </Button>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </div>
          )}
          <AddClassDialog
            championshipId={id}
            open={addClassOpen}
            onOpenChange={setAddClassOpen}
          />
        </TabsContent>

        {/* Events tab */}
        <TabsContent value="events" className="mt-4">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-base font-semibold">Linked Events</h2>
            <Button size="sm" onClick={() => setLinkEventOpen(true)}>
              <Plus className="h-4 w-4 mr-1" />
              Link Event
            </Button>
          </div>
          {data.events.length === 0 ? (
            <p className="text-sm text-muted-foreground py-4">No events linked yet.</p>
          ) : (
            <div className="rounded-lg border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-20">Round</TableHead>
                    <TableHead>Event</TableHead>
                    <TableHead className="w-16" />
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {[...data.events]
                    .sort((a, b) => a.roundNumber - b.roundNumber)
                    .map(link => {
                      const eventName = events.find(e => e.id === link.eventId)?.name
                        ?? `Event ${link.eventId}`;
                      return (
                        <TableRow key={link.id}>
                          <TableCell className="font-medium">R{link.roundNumber}</TableCell>
                          <TableCell>{eventName}</TableCell>
                          <TableCell>
                            <Button
                              variant="ghost"
                              size="icon-sm"
                              disabled={unlinkEventMutation.isPending}
                              onClick={async () => {
                                try {
                                  await unlinkEventMutation.mutateAsync(link.eventId);
                                  toast.success('Event unlinked');
                                } catch {
                                  toast.error('Could not unlink event.');
                                }
                              }}
                            >
                              <Trash2 className="h-4 w-4 text-muted-foreground" />
                            </Button>
                          </TableCell>
                        </TableRow>
                      );
                    })}
                </TableBody>
              </Table>
            </div>
          )}
          <LinkEventDialog
            championshipId={id}
            open={linkEventOpen}
            onOpenChange={setLinkEventOpen}
          />
        </TabsContent>

        {/* Points Scale tab */}
        <TabsContent value="points-scale" className="mt-4">
          <PointsScaleEditor championshipId={id} initialScale={data.pointsScale} />
        </TabsContent>

        {/* Standings tab */}
        <TabsContent value="standings" className="mt-4">
          <ChampionshipStandingsTable championshipId={id} />
        </TabsContent>

        {/* Exclusions tab */}
        <TabsContent value="exclusions" className="mt-4">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-base font-semibold">Exclusions</h2>
            <Button size="sm" onClick={() => setCreateExclusionOpen(true)}>
              <Plus className="h-4 w-4 mr-1" />
              Add Exclusion
            </Button>
          </div>
          {exclusionsLoading ? (
            <div className="flex items-center gap-2 py-4 text-muted-foreground">
              <Loader2 className="h-4 w-4 animate-spin" />
              <span className="text-sm">Loading…</span>
            </div>
          ) : exclusions.length === 0 ? (
            <p className="text-sm text-muted-foreground py-4">No exclusions recorded.</p>
          ) : (
            <div className="rounded-lg border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Driver</TableHead>
                    <TableHead>Event</TableHead>
                    <TableHead>Reason</TableHead>
                    <TableHead>Created</TableHead>
                    <TableHead className="w-16" />
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {exclusions.map(exc => {
                    const eventName = events.find(e => e.id === exc.eventId)?.name
                      ?? `Event ${exc.eventId}`;
                    return (
                      <TableRow key={exc.id}>
                        <TableCell>Driver {exc.driverId}</TableCell>
                        <TableCell>{eventName}</TableCell>
                        <TableCell className="max-w-xs truncate">{exc.reason}</TableCell>
                        <TableCell className="text-xs text-muted-foreground">
                          {new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium' }).format(new Date(exc.createdAt))}
                          <br />
                          by admin {exc.createdBy}
                        </TableCell>
                        <TableCell>
                          <Button
                            variant="ghost"
                            size="icon-sm"
                            disabled={deleteExclusionMutation.isPending}
                            onClick={async () => {
                              try {
                                await deleteExclusionMutation.mutateAsync(exc.id);
                                toast.success('Exclusion deleted');
                              } catch {
                                toast.error('Could not delete exclusion.');
                              }
                            }}
                          >
                            <Trash2 className="h-4 w-4 text-muted-foreground" />
                          </Button>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </div>
          )}
          <CreateExclusionDialog
            championshipId={id}
            open={createExclusionOpen}
            onOpenChange={setCreateExclusionOpen}
          />
        </TabsContent>
      </Tabs>
    </div>
  );
}
