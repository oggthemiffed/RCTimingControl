import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { isAxiosError } from 'axios';
import { Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Form, FormField, FormItem, FormLabel, FormControl, FormMessage,
} from '@/components/ui/form';
import {
  Sheet, SheetContent, SheetHeader, SheetTitle, SheetFooter,
} from '@/components/ui/sheet';
import { Separator } from '@/components/ui/separator';
import {
  useCreateCar, useUpdateCar, useArchiveCar,
} from '@/hooks/racer/useCars';
import type { CarDto } from '@/lib/racerApi';

// Form uses string for primaryClassId so RHF binds cleanly to the input;
// we parse to number in onSubmit.
const carSchema = z.object({
  name: z.string().min(1, 'Required').max(100),
  primaryClassId: z
    .string()
    .optional()
    .refine(v => v == null || v === '' || !Number.isNaN(Number(v)), {
      message: 'Must be a number',
    }),
  notes: z.string().max(2000).optional(),
});
type CarForm = z.infer<typeof carSchema>;

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  car: CarDto | null; // null means create mode
}

export default function CarEditSheet({ open, onOpenChange, car }: Props) {
  const createCar = useCreateCar();
  const updateCar = useUpdateCar();
  const archiveCar = useArchiveCar();

  const form = useForm<CarForm>({
    resolver: zodResolver(carSchema),
    mode: 'onBlur',
    defaultValues: { name: '', primaryClassId: '', notes: '' },
  });

  useEffect(() => {
    if (open) {
      form.reset({
        name: car?.name ?? '',
        primaryClassId: car?.primaryClassId == null ? '' : String(car.primaryClassId),
        notes: car?.notes ?? '',
      });
    }
  }, [open, car, form]);

  async function onSubmit(values: CarForm) {
    try {
      const classId =
        values.primaryClassId == null || values.primaryClassId === ''
          ? null
          : Number(values.primaryClassId);
      const payload = {
        name: values.name,
        primaryClassId: classId,
        notes: values.notes || undefined,
      };
      if (car) {
        await updateCar.mutateAsync({ id: car.id, req: payload });
        toast.success('Car saved');
      } else {
        await createCar.mutateAsync(payload);
        toast.success('Car saved');
      }
      onOpenChange(false);
    } catch (err) {
      if (isAxiosError(err) && err.response?.status === 400) {
        const errors = err.response.data?.errors as Record<string, string> | undefined;
        if (errors) {
          Object.entries(errors).forEach(([f, m]) =>
            form.setError(f as keyof CarForm, { message: m }));
          return;
        }
      }
      toast.error('Unable to reach server. Please try again.', { duration: 8000 });
    }
  }

  async function onArchive() {
    if (!car) return;
    if (!window.confirm(
      `Archive ${car.name}? Archived cars are hidden from entries but their race history is preserved.`
    )) return;
    try {
      await archiveCar.mutateAsync(car.id);
      toast.success(`${car.name} archived`);
      onOpenChange(false);
    } catch {
      toast.error('Unable to reach server. Please try again.', { duration: 8000 });
    }
  }

  const saving = createCar.isPending || updateCar.isPending;

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="overflow-y-auto">
        <SheetHeader>
          <SheetTitle>{car ? `Edit ${car.name}` : 'Add a car'}</SheetTitle>
        </SheetHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 mt-4">
            <FormField control={form.control} name="name" render={({ field }) => (
              <FormItem>
                <FormLabel>Name</FormLabel>
                <FormControl><Input autoFocus {...field} /></FormControl>
                <FormMessage />
              </FormItem>
            )} />
            <FormField control={form.control} name="primaryClassId" render={({ field }) => (
              <FormItem>
                <FormLabel>Primary class (ID)</FormLabel>
                <FormControl>
                  <Input
                    type="number"
                    {...field}
                    value={field.value ?? ''}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )} />
            <FormField control={form.control} name="notes" render={({ field }) => (
              <FormItem>
                <FormLabel>Notes</FormLabel>
                <FormControl>
                  <Input {...field} value={field.value ?? ''} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )} />
            <Separator />
            <SheetFooter className="flex gap-2">
              <Button type="submit" disabled={saving}>
                {saving
                  ? <><Loader2 className="mr-2 h-4 w-4 animate-spin" />Saving…</>
                  : car ? 'Save car' : 'Add car'}
              </Button>
              {car && (
                <Button
                  type="button"
                  variant="destructive"
                  onClick={onArchive}
                  disabled={archiveCar.isPending}
                >
                  Archive
                </Button>
              )}
            </SheetFooter>
          </form>
        </Form>
      </SheetContent>
    </Sheet>
  );
}
