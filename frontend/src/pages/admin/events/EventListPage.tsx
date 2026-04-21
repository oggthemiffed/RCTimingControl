import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  useReactTable,
  getCoreRowModel,
  getSortedRowModel,
  flexRender,
  createColumnHelper,
  type SortingState,
} from '@tanstack/react-table';
import { Plus, ArrowUpDown, RefreshCw } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog';
import { Badge } from '@/components/ui/badge';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

import { useAdminEventsList, useCreateAdminEvent } from '@/hooks/admin/useAdminEvents';
import type { AdminEventListDto, EventStatus } from '@/lib/adminApi';

// ── Status badge colors (D-06 / UI-SPEC.md) ───────────────────────────────

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

// ── Create Event form schema ───────────────────────────────────────────────

const createEventSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  eventDate: z.string().min(1, 'Date is required'),
});
type CreateEventFormValues = z.infer<typeof createEventSchema>;

// ── Column helper ──────────────────────────────────────────────────────────

const columnHelper = createColumnHelper<AdminEventListDto>();

// ── Component ─────────────────────────────────────────────────────────────

export default function EventListPage() {
  const navigate = useNavigate();
  const [sorting, setSorting] = useState<SortingState>([]);
  const [createOpen, setCreateOpen] = useState(false);

  const { data: events, isLoading, isError, refetch } = useAdminEventsList();
  const createEvent = useCreateAdminEvent();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<CreateEventFormValues>({
    resolver: zodResolver(createEventSchema),
  });

  const columns = [
    columnHelper.accessor('id', {
      header: ({ column }) => (
        <Button
          variant="ghost"
          size="sm"
          className="-ml-3"
          onClick={() => column.toggleSorting(column.getIsSorted() === 'asc')}
        >
          ID
          <ArrowUpDown className="ml-2 h-3 w-3" />
        </Button>
      ),
      cell: info => <span className="text-muted-foreground text-xs">#{info.getValue()}</span>,
      size: 64,
    }),
    columnHelper.accessor('name', {
      header: 'Name',
      cell: info => (
        <button
          className="text-left font-medium hover:underline text-primary"
          onClick={() => navigate(`/admin/events/${info.row.original.id}`)}
        >
          {info.getValue()}
        </button>
      ),
    }),
    columnHelper.accessor('eventDate', {
      header: ({ column }) => (
        <Button
          variant="ghost"
          size="sm"
          className="-ml-3"
          onClick={() => column.toggleSorting(column.getIsSorted() === 'asc')}
        >
          Date
          <ArrowUpDown className="ml-2 h-3 w-3" />
        </Button>
      ),
      cell: info =>
        new Intl.DateTimeFormat('en-GB', {
          year: 'numeric',
          month: 'short',
          day: 'numeric',
        }).format(new Date(info.getValue())),
    }),
    columnHelper.accessor('status', {
      header: 'Status',
      cell: info => {
        const status = info.getValue();
        return (
          <Badge className={statusColor[status]}>
            {statusLabel[status]}
          </Badge>
        );
      },
    }),
    columnHelper.accessor('trackName', {
      header: 'Track',
      cell: info => info.getValue() ?? <span className="text-muted-foreground">—</span>,
    }),
  ];

  const table = useReactTable({
    data: events ?? [],
    columns,
    state: { sorting },
    onSortingChange: setSorting,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
  });

  async function onCreateSubmit(values: CreateEventFormValues) {
    try {
      await createEvent.mutateAsync({
        name: values.name,
        eventDate: values.eventDate,
        trackId: null,
      });
      toast.success('Event created');
      setCreateOpen(false);
      reset();
    } catch {
      toast.error('Event could not be created. Check your connection and try again.');
    }
  }

  return (
    <div>
      {/* Page header */}
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-semibold">Events</h1>
        <Button onClick={() => setCreateOpen(true)}>
          <Plus className="h-4 w-4 mr-1" />
          Create Event
        </Button>
      </div>

      {/* Error state */}
      {isError && (
        <div className="rounded-lg border border-destructive/30 bg-destructive/5 p-4 flex items-center justify-between mb-4">
          <p className="text-sm text-destructive">Failed to load events.</p>
          <Button variant="outline" size="sm" onClick={() => refetch()}>
            <RefreshCw className="h-3 w-3 mr-1" />
            Retry
          </Button>
        </div>
      )}

      {/* Table */}
      {isLoading ? (
        <div className="space-y-2">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="h-12 rounded-lg bg-muted animate-pulse" />
          ))}
        </div>
      ) : events && events.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <h2 className="text-xl font-semibold mb-2">No events yet</h2>
          <p className="text-muted-foreground text-sm mb-6">
            Create your first event to get started.
          </p>
          <Button onClick={() => setCreateOpen(true)}>
            <Plus className="h-4 w-4 mr-1" />
            Create Event
          </Button>
        </div>
      ) : (
        <div className="rounded-lg border">
          <Table>
            <TableHeader>
              {table.getHeaderGroups().map(headerGroup => (
                <TableRow key={headerGroup.id}>
                  {headerGroup.headers.map(header => (
                    <TableHead key={header.id} style={{ width: header.getSize() }}>
                      {header.isPlaceholder
                        ? null
                        : flexRender(header.column.columnDef.header, header.getContext())}
                    </TableHead>
                  ))}
                </TableRow>
              ))}
            </TableHeader>
            <TableBody>
              {table.getRowModel().rows.map(row => (
                <TableRow key={row.id}>
                  {row.getVisibleCells().map(cell => (
                    <TableCell key={cell.id}>
                      {flexRender(cell.column.columnDef.cell, cell.getContext())}
                    </TableCell>
                  ))}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      {/* Create Event dialog */}
      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Create Event</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleSubmit(onCreateSubmit)} className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="name">Event Name</Label>
              <Input id="name" {...register('name')} placeholder="e.g. Round 3 — Buggy" />
              {errors.name && (
                <p className="text-xs text-destructive">{errors.name.message}</p>
              )}
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="eventDate">Date</Label>
              <Input id="eventDate" type="date" {...register('eventDate')} />
              {errors.eventDate && (
                <p className="text-xs text-destructive">{errors.eventDate.message}</p>
              )}
            </div>
            {/* TODO Plan 06: add track select when tracks API is available */}
            <DialogFooter>
              <Button type="submit" disabled={isSubmitting || createEvent.isPending}>
                {createEvent.isPending ? 'Creating…' : 'Create Event'}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
