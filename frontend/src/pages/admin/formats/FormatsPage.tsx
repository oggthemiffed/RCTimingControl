import { useState } from 'react';
import { Plus, Pencil, Trash2 } from 'lucide-react';
import { toast } from 'sonner';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { FormatConfigFields } from './FormatConfigFields';
import {
  useFormatsList,
  useCreateFormat,
  useUpdateFormat,
  useDeleteFormat,
} from '@/hooks/admin/useAdminFormats';
import type { RaceFormatConfig, RaceFormatTemplateDto } from '@/lib/adminApi';

const DEFAULT_CONFIG: RaceFormatConfig = {
  type: 'TIMED',
  durationMinutes: 5,
  startType: 'STAGGER',
  qualifyingType: 'FTQ',
  racePaddingMinutes: 2,
  staggerIntervalSeconds: 5,
};

function FormatDialog({
  open,
  onOpenChange,
  initialName,
  initialConfig,
  onSubmit,
  title,
}: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  initialName?: string;
  initialConfig?: RaceFormatConfig;
  onSubmit: (name: string, config: RaceFormatConfig) => Promise<void>;
  title: string;
}) {
  const [name, setName] = useState(initialName ?? '');
  const [config, setConfig] = useState<RaceFormatConfig>(initialConfig ?? DEFAULT_CONFIG);
  const [submitting, setSubmitting] = useState(false);
  const [nameError, setNameError] = useState('');

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!name.trim()) { setNameError('Name is required'); return; }
    setNameError('');
    setSubmitting(true);
    try {
      await onSubmit(name, config);
    } finally {
      setSubmitting(false);
    }
  }

  function handleOpen(v: boolean) {
    onOpenChange(v);
    if (!v) {
      setName(initialName ?? '');
      setConfig(initialConfig ?? DEFAULT_CONFIG);
      setNameError('');
    }
  }

  return (
    <Dialog open={open} onOpenChange={handleOpen}>
      <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
        <DialogHeader><DialogTitle>{title}</DialogTitle></DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="fmt-name">Template Name</Label>
            <Input
              id="fmt-name"
              value={name}
              onChange={e => setName(e.target.value)}
              placeholder="e.g. Standard 5-minute Timed"
            />
            {nameError && <p className="text-xs text-destructive">{nameError}</p>}
          </div>
          <FormatConfigFields value={config} onChange={setConfig} />
          <DialogFooter>
            <Button type="submit" disabled={submitting}>
              {submitting ? 'Saving…' : 'Save'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

export default function FormatsPage() {
  const { data: formats, isLoading, isError, refetch } = useFormatsList();
  const createMutation = useCreateFormat();
  const updateMutation = useUpdateFormat();
  const deleteMutation = useDeleteFormat();

  const [createOpen, setCreateOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<RaceFormatTemplateDto | null>(null);

  async function handleCreate(name: string, config: RaceFormatConfig) {
    try {
      await createMutation.mutateAsync({ name, config });
      toast.success('Format template created');
      setCreateOpen(false);
    } catch {
      toast.error('Could not create format template. Try again.');
    }
  }

  async function handleUpdate(name: string, config: RaceFormatConfig) {
    if (!editTarget) return;
    try {
      await updateMutation.mutateAsync({ id: editTarget.id, body: { name, config } });
      toast.success('Format template updated');
      setEditTarget(null);
    } catch {
      toast.error('Could not update format template. Try again.');
    }
  }

  async function handleDelete(id: number) {
    try {
      await deleteMutation.mutateAsync(id);
      toast.success('Format template deleted');
    } catch {
      toast.error('Could not delete format template. Try again.');
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-semibold">Race Format Templates</h1>
        <Button onClick={() => setCreateOpen(true)}>
          <Plus className="h-4 w-4 mr-1" />
          Create Template
        </Button>
      </div>

      {isError && (
        <div className="rounded-lg border border-destructive/30 bg-destructive/5 p-4 flex items-center justify-between mb-4">
          <p className="text-sm text-destructive">Failed to load format templates.</p>
          <Button variant="outline" size="sm" onClick={() => refetch()}>Retry</Button>
        </div>
      )}

      {isLoading ? (
        <div className="space-y-2">
          {[...Array(3)].map((_, i) => <div key={i} className="h-12 rounded-lg bg-muted animate-pulse" />)}
        </div>
      ) : !formats || formats.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <h2 className="text-xl font-semibold mb-2">No format templates</h2>
          <p className="text-muted-foreground text-sm mb-6">
            Create a template to define race format defaults for event classes.
          </p>
          <Button onClick={() => setCreateOpen(true)}>
            <Plus className="h-4 w-4 mr-1" />
            Create Template
          </Button>
        </div>
      ) : (
        <div className="rounded-lg border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>Type</TableHead>
                <TableHead className="w-24" />
              </TableRow>
            </TableHeader>
            <TableBody>
              {formats.map(fmt => (
                <TableRow key={fmt.id}>
                  <TableCell className="font-medium">{fmt.name}</TableCell>
                  <TableCell className="text-sm text-muted-foreground">{fmt.config.type}</TableCell>
                  <TableCell>
                    <div className="flex items-center gap-1">
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        onClick={() => setEditTarget(fmt)}
                        aria-label="Edit format"
                      >
                        <Pencil className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        disabled={deleteMutation.isPending}
                        onClick={() => handleDelete(fmt.id)}
                        aria-label="Delete format"
                      >
                        <Trash2 className="h-4 w-4 text-destructive" />
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      <FormatDialog
        open={createOpen}
        onOpenChange={setCreateOpen}
        onSubmit={handleCreate}
        title="Create Format Template"
      />
      <FormatDialog
        key={editTarget?.id ?? 'edit'}
        open={!!editTarget}
        onOpenChange={v => { if (!v) setEditTarget(null); }}
        initialName={editTarget?.name}
        initialConfig={editTarget?.config}
        onSubmit={handleUpdate}
        title="Edit Format Template"
      />
    </div>
  );
}
