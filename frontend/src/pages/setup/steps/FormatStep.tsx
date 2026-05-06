import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
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
  name: z.string().min(1, 'Format name is required').max(200),
  type: z.enum(['TIMED', 'BUMP_UP', 'POINTS_FINALS']),
  durationMinutes: z.coerce.number().int().positive('Duration must be positive'),
});

type FormValues = z.infer<typeof schema>;

interface Props {
  onNext: () => void;
  onBack?: () => void;
}

export default function FormatStep({ onNext, onBack }: Props) {
  const queryClient = useQueryClient();

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    mode: 'onBlur',
    defaultValues: {
      name: '',
      type: 'TIMED',
      durationMinutes: 5,
    },
  });

  async function onSave(values: FormValues) {
    try {
      // Build a minimal valid config based on the chosen type
      const type = values.type;
      const duration = values.durationMinutes;

      const config =
        type === 'TIMED'
          ? {
              type: 'TIMED' as const,
              durationMinutes: duration,
              startType: 'STAGGER' as const,
              qualifyingType: 'FTQ' as const,
              racePaddingMinutes: 2,
              staggerIntervalSeconds: 5,
            }
          : type === 'BUMP_UP'
            ? {
                type: 'BUMP_UP' as const,
                qualifyingHeats: 3,
                heatDurationMinutes: duration,
                bestHeatsCount: 2,
                gridSize: 10,
                bumpSpots: 3,
                qualifyingStartType: 'STAGGER' as const,
                finalsStartType: 'GRID' as const,
                qualifyingType: 'FTQ' as const,
                racePaddingMinutes: 2,
                staggerIntervalSeconds: 5,
              }
            : {
                type: 'POINTS_FINALS' as const,
                qualifyingHeats: 3,
                finalsCount: 3,
                finalDurationMinutes: duration,
                heatDurationMinutes: duration,
                qualifyingStartType: 'STAGGER' as const,
                finalsStartType: 'GRID' as const,
                qualifyingType: 'FTQ' as const,
                racePaddingMinutes: 2,
                staggerIntervalSeconds: 5,
              };

      await adminApi.formats.create({ name: values.name, config });
      queryClient.invalidateQueries({ queryKey: ['setup-status'] });
      queryClient.invalidateQueries({ queryKey: ['setup-progress'] });
      toast.success('Race format saved');
      onNext();
    } catch {
      toast.error('Could not save race format. Try again.');
    }
  }

  function onSkip() {
    onNext();
  }

  return (
    <div>
      <h1 className="text-2xl font-semibold mb-2">Race Format</h1>
      <p className="text-sm text-muted-foreground mb-6">
        Create a race format template. You can create more from the Admin panel later.
      </p>

      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSave)} className="space-y-4">
          <FormField
            control={form.control}
            name="name"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Format Name</FormLabel>
                <FormControl>
                  <Input placeholder="e.g. Standard 5-minute Timed" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="type"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Format Type</FormLabel>
                <Select value={field.value} onValueChange={field.onChange}>
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue placeholder="Select a format type" />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    <SelectItem value="TIMED">Timed</SelectItem>
                    <SelectItem value="BUMP_UP">Bump Up</SelectItem>
                    <SelectItem value="POINTS_FINALS">Points Finals</SelectItem>
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="durationMinutes"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Duration (minutes)</FormLabel>
                <FormControl>
                  <Input
                    type="number"
                    min={1}
                    step={1}
                    placeholder="e.g. 5"
                    {...field}
                  />
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
        href="/admin/formats"
        className="text-sm text-muted-foreground underline mt-4 inline-block"
      >
        Manage more in Admin →
      </a>
    </div>
  );
}
