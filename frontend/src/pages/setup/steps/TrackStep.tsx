import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Form,
  FormField,
  FormItem,
  FormLabel,
  FormControl,
  FormMessage,
} from '@/components/ui/form';
import { adminApi } from '@/lib/adminApi';

const schema = z.object({
  name: z.string().min(1, 'Track name is required').max(200),
  lengthMeters: z.coerce.number().int().positive().optional(),
  notes: z.string().optional().or(z.literal('')),
});

type FormValues = z.infer<typeof schema>;

interface Props {
  onNext: () => void;
  onBack?: () => void;
}

export default function TrackStep({ onNext, onBack }: Props) {
  const queryClient = useQueryClient();

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    mode: 'onBlur',
    defaultValues: {
      name: '',
      lengthMeters: undefined,
      notes: '',
    },
  });

  async function onSave(values: FormValues) {
    try {
      await adminApi.tracks.create({
        name: values.name,
        venueNotes: values.notes || null,
        trackLength: values.lengthMeters ?? null,
      });
      queryClient.invalidateQueries({ queryKey: ['setup-status'] });
      queryClient.invalidateQueries({ queryKey: ['setup-progress'] });
      toast.success('Track saved');
      onNext();
    } catch {
      toast.error('Could not save track. Try again.');
    }
  }

  function onSkip() {
    onNext();
  }

  return (
    <div>
      <h1 className="text-2xl font-semibold mb-2">Track</h1>
      <p className="text-sm text-muted-foreground mb-6">
        Define at least one track so you can assign it to events.
      </p>

      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSave)} className="space-y-4">
          <FormField
            control={form.control}
            name="name"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Track Name</FormLabel>
                <FormControl>
                  <Input placeholder="e.g. Club Track A" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="lengthMeters"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Track Length (m, optional)</FormLabel>
                <FormControl>
                  <Input
                    type="number"
                    min={1}
                    step={1}
                    placeholder="e.g. 150"
                    {...field}
                    value={field.value ?? ''}
                    onChange={e => field.onChange(e.target.value === '' ? undefined : e.target.value)}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="notes"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Notes (optional)</FormLabel>
                <FormControl>
                  <Input placeholder="Venue notes, directions, etc." {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <div className="flex justify-between gap-2 pt-4">
            <Button type="button" variant="ghost" onClick={onBack}>
              Back
            </Button>
            <div className="flex gap-2">
              <Button type="button" variant="ghost" onClick={onSkip}>
                Skip for now
              </Button>
              <Button type="submit" disabled={form.formState.isSubmitting}>
                Save and Continue
              </Button>
            </div>
          </div>
        </form>
      </Form>

      <a
        href="/admin/tracks"
        className="text-sm text-muted-foreground underline mt-4 inline-block"
      >
        Manage more in Admin →
      </a>
    </div>
  );
}
