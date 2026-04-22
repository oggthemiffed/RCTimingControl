import { useState } from 'react';
import { Plus, Pencil, Archive, ArchiveRestore } from 'lucide-react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Switch } from '@/components/ui/switch';
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
import {
  useCarTagCategories,
  useCreateCarTagCategory,
  useUpdateCarTagCategory,
  useArchiveCarTagCategory,
  useUnarchiveCarTagCategory,
} from '@/hooks/admin/useAdminCarTagCategories';
import type { CarTagCategoryDto } from '@/lib/adminApi';

const categorySchema = z.object({
  name: z.string().min(1, 'Name is required'),
  color: z.string().nullable().or(z.literal('')),
  sortOrder: z.coerce.number().int().min(0),
});
type CategoryFormValues = z.infer<typeof categorySchema>;

function CategoryFormDialog({
  open,
  onOpenChange,
  initialValue,
  onSubmit,
  title,
}: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  initialValue?: CarTagCategoryDto;
  onSubmit: (values: CategoryFormValues) => Promise<void>;
  title: string;
}) {
  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors, isSubmitting },
  } = useForm<CategoryFormValues>({
    resolver: zodResolver(categorySchema),
    defaultValues: {
      name: initialValue?.name ?? '',
      color: initialValue?.color ?? '',
      sortOrder: initialValue?.sortOrder ?? 0,
    },
  });

  async function handleFormSubmit(values: CategoryFormValues) {
    await onSubmit(values);
    reset();
  }

  return (
    <Dialog open={open} onOpenChange={v => { onOpenChange(v); if (!v) reset(); }}>
      <DialogContent>
        <DialogHeader><DialogTitle>{title}</DialogTitle></DialogHeader>
        <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="cat-name">Name</Label>
            <Input id="cat-name" {...register('name')} placeholder="e.g. Stock Buggy" />
            {errors.name && <p className="text-xs text-destructive">{errors.name.message}</p>}
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <Label>Colour</Label>
              <Controller
                name="color"
                control={control}
                render={({ field }) => (
                  <div className="flex items-center gap-2">
                    <input
                      type="color"
                      value={field.value || '#000000'}
                      onChange={e => field.onChange(e.target.value)}
                      className="h-9 w-9 cursor-pointer rounded border p-0.5 bg-background"
                    />
                    <Input
                      value={field.value || ''}
                      onChange={field.onChange}
                      placeholder="#3b82f6"
                    />
                  </div>
                )}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="cat-sort">Sort Order</Label>
              <Input id="cat-sort" type="number" min={0} {...register('sortOrder')} />
            </div>
          </div>
          <DialogFooter>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Saving…' : 'Save'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

export default function CarTagCategoriesPage() {
  const [includeArchived, setIncludeArchived] = useState(false);
  const { data: categories, isLoading, isError, refetch } = useCarTagCategories(includeArchived);

  const createMutation = useCreateCarTagCategory();
  const updateMutation = useUpdateCarTagCategory();
  const archiveMutation = useArchiveCarTagCategory();
  const unarchiveMutation = useUnarchiveCarTagCategory();

  const [createOpen, setCreateOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<CarTagCategoryDto | null>(null);

  async function handleCreate(values: CategoryFormValues) {
    try {
      await createMutation.mutateAsync({
        name: values.name,
        color: values.color || null,
        sortOrder: values.sortOrder,
      });
      toast.success('Category created');
      setCreateOpen(false);
    } catch {
      toast.error('Could not create category. Try again.');
    }
  }

  async function handleUpdate(values: CategoryFormValues) {
    if (!editTarget) return;
    try {
      await updateMutation.mutateAsync({
        id: editTarget.id,
        body: { name: values.name, color: values.color || null, sortOrder: values.sortOrder },
      });
      toast.success('Category updated');
      setEditTarget(null);
    } catch {
      toast.error('Could not update category. Try again.');
    }
  }

  async function handleArchive(id: number) {
    try {
      await archiveMutation.mutateAsync(id);
      toast.success('Category archived');
    } catch {
      toast.error('Could not archive category. Try again.');
    }
  }

  async function handleUnarchive(id: number) {
    try {
      await unarchiveMutation.mutateAsync(id);
      toast.success('Category restored');
    } catch {
      toast.error('Could not restore category. Try again.');
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-semibold">Car Tag Categories</h1>
        <Button onClick={() => setCreateOpen(true)}>
          <Plus className="h-4 w-4 mr-1" />
          Create Category
        </Button>
      </div>

      <div className="flex items-center gap-3 mb-4">
        <Switch
          id="show-archived"
          checked={includeArchived}
          onCheckedChange={setIncludeArchived}
        />
        <Label htmlFor="show-archived" className="font-normal cursor-pointer">
          Show archived categories
        </Label>
      </div>

      {isError && (
        <div className="rounded-lg border border-destructive/30 bg-destructive/5 p-4 flex items-center justify-between mb-4">
          <p className="text-sm text-destructive">Failed to load categories.</p>
          <Button variant="outline" size="sm" onClick={() => refetch()}>Retry</Button>
        </div>
      )}

      {isLoading ? (
        <div className="space-y-2">
          {[...Array(4)].map((_, i) => <div key={i} className="h-12 rounded-lg bg-muted animate-pulse" />)}
        </div>
      ) : !categories || categories.length === 0 ? (
        <p className="text-sm text-muted-foreground py-8">
          {includeArchived ? 'No categories found.' : 'No active categories. Toggle "Show archived" to see archived categories.'}
        </p>
      ) : (
        <div className="rounded-lg border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>Colour</TableHead>
                <TableHead className="w-20">Sort</TableHead>
                {includeArchived && <TableHead className="w-24">Status</TableHead>}
                <TableHead className="w-24" />
              </TableRow>
            </TableHeader>
            <TableBody>
              {categories.map(cat => (
                <TableRow key={cat.id} className={cat.archived ? 'opacity-60' : ''}>
                  <TableCell className="font-medium">{cat.name}</TableCell>
                  <TableCell>
                    {cat.color ? (
                      <div className="flex items-center gap-2">
                        <div
                          className="h-4 w-4 rounded border"
                          style={{ backgroundColor: cat.color }}
                        />
                        <span className="text-xs text-muted-foreground">{cat.color}</span>
                      </div>
                    ) : (
                      <span className="text-muted-foreground">—</span>
                    )}
                  </TableCell>
                  <TableCell>{cat.sortOrder}</TableCell>
                  {includeArchived && (
                    <TableCell>
                      <span className={`text-xs ${cat.archived ? 'text-muted-foreground' : 'text-green-600'}`}>
                        {cat.archived ? 'Archived' : 'Active'}
                      </span>
                    </TableCell>
                  )}
                  <TableCell>
                    <div className="flex items-center gap-1">
                      {!cat.archived && (
                        <Button
                          variant="ghost"
                          size="icon-sm"
                          onClick={() => setEditTarget(cat)}
                          aria-label="Edit category"
                        >
                          <Pencil className="h-4 w-4" />
                        </Button>
                      )}
                      {cat.archived ? (
                        <Button
                          variant="ghost"
                          size="icon-sm"
                          disabled={unarchiveMutation.isPending}
                          onClick={() => handleUnarchive(cat.id)}
                          aria-label="Unarchive category"
                        >
                          <ArchiveRestore className="h-4 w-4 text-primary" />
                        </Button>
                      ) : (
                        <Button
                          variant="ghost"
                          size="icon-sm"
                          disabled={archiveMutation.isPending}
                          onClick={() => handleArchive(cat.id)}
                          aria-label="Archive category"
                        >
                          <Archive className="h-4 w-4 text-muted-foreground" />
                        </Button>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      <CategoryFormDialog
        open={createOpen}
        onOpenChange={setCreateOpen}
        onSubmit={handleCreate}
        title="Create Category"
      />
      <CategoryFormDialog
        open={!!editTarget}
        onOpenChange={v => { if (!v) setEditTarget(null); }}
        initialValue={editTarget ?? undefined}
        onSubmit={handleUpdate}
        title="Edit Category"
      />
    </div>
  );
}
