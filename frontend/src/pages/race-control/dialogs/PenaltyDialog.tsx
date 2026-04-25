import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import type { PenaltyRequest } from '@/lib/raceControlApi';

const schema = z.object({
  entryId: z.coerce.number().int().positive(),
  penaltyType: z.enum(['LAP', 'TIME']),
  value: z.coerce.number().positive(),
  reason: z.string().min(3, 'Reason required'),
});

type FormValues = z.infer<typeof schema>;

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSubmit: (req: PenaltyRequest) => void;
  isPending: boolean;
};

export function PenaltyDialog({ open, onOpenChange, onSubmit, isPending }: Props) {
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { entryId: 0, penaltyType: 'LAP', value: 1, reason: '' },
  });

  function handleSubmit(values: FormValues) {
    onSubmit({ entryId: values.entryId, penaltyType: values.penaltyType, value: values.value, reason: values.reason });
    form.reset();
  }

  const penaltyType = form.watch('penaltyType');

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>Apply Penalty</DialogTitle>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="entryId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Entry ID</FormLabel>
                  <FormControl>
                    <Input type="number" placeholder="Entry ID" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="penaltyType"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Penalty Type</FormLabel>
                  <FormControl>
                    <RadioGroup
                      onValueChange={field.onChange}
                      defaultValue={field.value}
                      className="flex gap-4"
                    >
                      <div className="flex items-center gap-2">
                        <RadioGroupItem value="LAP" id="p-lap" />
                        <label htmlFor="p-lap" className="text-sm cursor-pointer">Lap deduction</label>
                      </div>
                      <div className="flex items-center gap-2">
                        <RadioGroupItem value="TIME" id="p-time" />
                        <label htmlFor="p-time" className="text-sm cursor-pointer">Time penalty (s)</label>
                      </div>
                    </RadioGroup>
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="value"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{penaltyType === 'LAP' ? 'Laps' : 'Seconds'}</FormLabel>
                  <FormControl>
                    <Input type="number" min={1} step={penaltyType === 'TIME' ? 0.1 : 1} {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="reason"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Reason</FormLabel>
                  <FormControl>
                    <Input placeholder="Brief reason" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                Cancel
              </Button>
              <Button type="submit" disabled={isPending} variant="destructive">
                {isPending ? 'Applying…' : 'Apply Penalty'}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
