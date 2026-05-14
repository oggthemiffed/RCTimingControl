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

const TIMEZONES = Intl.supportedValuesOf('timeZone');
const BROWSER_TZ = Intl.DateTimeFormat().resolvedOptions().timeZone;

const schema = z.object({
  name: z.string().min(1, 'Club name is required').max(200),
  timezone: z.string().min(1, 'Timezone is required'),
  email: z.string().email('Valid email required').optional().or(z.literal('')),
  phone: z.string().optional().or(z.literal('')),
  websiteUrl: z.string().url('Valid URL required').optional().or(z.literal('')),
});

type FormValues = z.infer<typeof schema>;

interface Props {
  onNext: () => void;
}

export default function ClubProfileStep({ onNext }: Props) {
  const queryClient = useQueryClient();

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    mode: 'onBlur',
    defaultValues: {
      name: '',
      timezone: BROWSER_TZ,
      email: '',
      phone: '',
      websiteUrl: '',
    },
  });

  async function onSave(values: FormValues) {
    try {
      await adminApi.club.updateProfile({
        name: values.name,
        email: values.email || null,
        phone: values.phone || null,
        websiteUrl: values.websiteUrl || null,
        latitude: null,
        longitude: null,
        timezone: values.timezone,
        logoType: null,
        showCarTagsInResults: false,
      });
      queryClient.invalidateQueries({ queryKey: ['setup-status'] });
      queryClient.invalidateQueries({ queryKey: ['setup-progress'] });
      toast.success('Club profile saved');
      onNext();
    } catch {
      toast.error('Could not save club profile. Try again.');
    }
  }

  return (
    <div>
      <h1 className="text-2xl font-semibold mb-2">Club Profile</h1>
      <p className="text-sm text-muted-foreground mb-6">
        Enter your club's name and contact details. This is the minimum required to use the system.
      </p>

      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSave)} className="space-y-4">
          <FormField
            control={form.control}
            name="name"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Club Name</FormLabel>
                <FormControl>
                  <Input placeholder="e.g. Glasgow RC Club" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="timezone"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Timezone</FormLabel>
                <Select onValueChange={field.onChange} value={field.value}>
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue placeholder="Select timezone" />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent className="max-h-60">
                    {TIMEZONES.map((tz) => (
                      <SelectItem key={tz} value={tz}>
                        {tz}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="email"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Contact Email (optional)</FormLabel>
                <FormControl>
                  <Input type="email" placeholder="club@example.com" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="phone"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Phone (optional)</FormLabel>
                <FormControl>
                  <Input type="tel" placeholder="+44 7700 000000" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="websiteUrl"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Website URL (optional)</FormLabel>
                <FormControl>
                  <Input type="url" placeholder="https://yourclub.example.com" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <div className="flex justify-end gap-2 pt-4">
            {/* No Back, no Skip on Step 1 (D-11) */}
            <Button type="submit" disabled={form.formState.isSubmitting}>
              Save and Continue
            </Button>
          </div>
        </form>
      </Form>
    </div>
  );
}
