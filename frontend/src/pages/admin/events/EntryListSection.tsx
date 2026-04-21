import { useState } from 'react';
import { toast } from 'sonner';

import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';

import { useEntriesForClass, useWithdrawEntry } from '@/hooks/admin/useAdminEntries';
import type { AdminEntryDto, EventClassDto } from '@/lib/adminApi';

// ── Entry status colors ───────────────────────────────────────────────────

const entryStatusColor: Record<AdminEntryDto['status'], string> = {
  PENDING: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-300',
  CONFIRMED: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300',
  WITHDRAWN: 'bg-zinc-100 text-zinc-500 dark:bg-zinc-800 dark:text-zinc-400',
};

// ── Withdraw confirm dialog ───────────────────────────────────────────────

function WithdrawDialog({
  eventId,
  classId,
  entry,
  open,
  onOpenChange,
}: {
  eventId: number;
  classId: number;
  entry: AdminEntryDto | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const withdraw = useWithdrawEntry(eventId, classId);
  const [reason, setReason] = useState('');
  const [reasonError, setReasonError] = useState<string | null>(null);

  async function handleConfirm() {
    if (!entry) return;
    if (!reason.trim()) {
      setReasonError('Please enter a reason for withdrawal.');
      return;
    }
    setReasonError(null);
    try {
      await withdraw.mutateAsync({ entryId: entry.id, reason: reason.trim() });
      toast.success(`${entry.firstName} ${entry.lastName}'s entry has been withdrawn.`);
      onOpenChange(false);
      setReason('');
    } catch {
      toast.error('Could not withdraw entry. Try again.');
    }
  }

  function handleClose(open: boolean) {
    if (!open) {
      setReason('');
      setReasonError(null);
    }
    onOpenChange(open);
  }

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Withdraw entry?</DialogTitle>
          <DialogDescription>
            {entry &&
              `This will withdraw ${entry.firstName} ${entry.lastName}'s entry for this class. The racer will need to re-enter if entries are still open.`}
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-1.5">
          <Label htmlFor="withdraw-reason">Reason</Label>
          <textarea
            id="withdraw-reason"
            value={reason}
            onChange={e => setReason(e.target.value)}
            rows={3}
            className="w-full rounded-md border bg-background px-3 py-2 text-sm resize-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            placeholder="e.g. Racer requested withdrawal"
          />
          {reasonError && <p className="text-xs text-destructive">{reasonError}</p>}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => handleClose(false)}>
            Cancel
          </Button>
          <Button
            variant="destructive"
            onClick={handleConfirm}
            disabled={withdraw.isPending}
          >
            {withdraw.isPending ? 'Withdrawing…' : 'Withdraw Entry'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

// ── Entries table for a single class ─────────────────────────────────────

function EntriesTable({
  eventId,
  classId,
}: {
  eventId: number;
  classId: number;
}) {
  const { data: entries, isLoading } = useEntriesForClass(eventId, classId);
  const [withdrawTarget, setWithdrawTarget] = useState<AdminEntryDto | null>(null);
  const [withdrawOpen, setWithdrawOpen] = useState(false);

  if (isLoading) {
    return (
      <div className="space-y-2 py-4">
        {[...Array(3)].map((_, i) => (
          <div key={i} className="h-10 rounded bg-muted animate-pulse" />
        ))}
      </div>
    );
  }

  if (!entries || entries.length === 0) {
    return (
      <div className="py-12 text-center">
        <h3 className="text-base font-semibold mb-1">No entries</h3>
        <p className="text-sm text-muted-foreground">
          Entries will appear here once racers register for this class.
        </p>
      </div>
    );
  }

  return (
    <>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Racer</TableHead>
            <TableHead>Transponder</TableHead>
            <TableHead>Status</TableHead>
            <TableHead>Submitted</TableHead>
            <TableHead className="w-24" />
          </TableRow>
        </TableHeader>
        <TableBody>
          {entries.map(entry => (
            <TableRow key={entry.id}>
              <TableCell className="font-medium">
                {entry.firstName} {entry.lastName}
              </TableCell>
              <TableCell>
                {entry.transponderNumber ?? (
                  <span className="text-muted-foreground">—</span>
                )}
              </TableCell>
              <TableCell>
                <Badge className={entryStatusColor[entry.status]}>
                  {entry.status.charAt(0) + entry.status.slice(1).toLowerCase()}
                </Badge>
              </TableCell>
              <TableCell className="text-sm text-muted-foreground">
                {new Intl.DateTimeFormat('en-GB', {
                  dateStyle: 'medium',
                  timeStyle: 'short',
                }).format(new Date(entry.submittedAt))}
              </TableCell>
              <TableCell>
                {(entry.status === 'PENDING' || entry.status === 'CONFIRMED') && (
                  <Button
                    size="sm"
                    variant="destructive"
                    onClick={() => {
                      setWithdrawTarget(entry);
                      setWithdrawOpen(true);
                    }}
                  >
                    Withdraw
                  </Button>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>

      <WithdrawDialog
        eventId={eventId}
        classId={classId}
        entry={withdrawTarget}
        open={withdrawOpen}
        onOpenChange={open => {
          setWithdrawOpen(open);
          if (!open) setWithdrawTarget(null);
        }}
      />
    </>
  );
}

// ── Main component ────────────────────────────────────────────────────────

interface EntryListSectionProps {
  eventId: number;
  classes: EventClassDto[];
}

export default function EntryListSection({ eventId, classes }: EntryListSectionProps) {
  const [selectedClassIdx, setSelectedClassIdx] = useState(0);

  if (classes.length === 0) {
    return (
      <div className="py-12 text-center">
        <p className="text-sm text-muted-foreground">
          Add a class to see entries.
        </p>
      </div>
    );
  }

  const selectedClass = classes[selectedClassIdx];

  return (
    <div className="space-y-4">
      {/* Class selector */}
      {classes.length > 1 && (
        <div className="flex flex-wrap gap-2">
          {classes.map((cls, idx) => (
            <Button
              key={cls.id}
              size="sm"
              variant={idx === selectedClassIdx ? 'default' : 'outline'}
              onClick={() => setSelectedClassIdx(idx)}
            >
              Class {cls.racingClassId ?? cls.id}
            </Button>
          ))}
        </div>
      )}

      {/* Entries for selected class */}
      {selectedClass && (
        <div className="rounded-lg border">
          <EntriesTable eventId={eventId} classId={selectedClass.id} />
        </div>
      )}
    </div>
  );
}
