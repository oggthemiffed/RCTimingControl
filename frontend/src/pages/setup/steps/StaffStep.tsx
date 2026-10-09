import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useQueryClient } from '@tanstack/react-query';
import { isAxiosError } from 'axios';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Form } from '@/components/ui/form';
import { createSetupStaff } from '@/lib/setupApi';
import { setupQueryKeys } from '@/hooks/setup/setupQueryKeys';
import { TextField } from '@/components/TextField';
import { OfficialRoleCheckboxes } from '@/components/OfficialRoleCheckboxes';

const schema = z
  .object({
    firstName: z.string().min(1, 'First name required').max(100),
    lastName: z.string().min(1, 'Last name required').max(100),
    email: z.string().email('Valid email required'),
    password: z.string().min(8, 'Password must be at least 8 characters'),
    confirmPassword: z.string().min(1, 'Please confirm your password'),
    roles: z
      .array(z.enum(['ADMIN', 'RACE_DIRECTOR', 'REFEREE']))
      .min(1, 'Select at least one role'),
  })
  .refine((d) => d.password === d.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  });

type FormValues = z.infer<typeof schema>;

interface Props {
  onNext: () => void;
  onBack?: () => void;
}

export default function StaffStep({ onNext, onBack }: Props) {
  const queryClient = useQueryClient();

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    mode: 'onBlur',
    defaultValues: {
      firstName: '',
      lastName: '',
      email: '',
      password: '',
      confirmPassword: '',
      roles: [],
    },
  });

  async function onSave(values: FormValues) {
    try {
      await createSetupStaff({
        firstName: values.firstName,
        lastName: values.lastName,
        email: values.email,
        password: values.password,
        roles: values.roles,
      });
      queryClient.invalidateQueries({ queryKey: setupQueryKeys.status() });
      queryClient.invalidateQueries({ queryKey: setupQueryKeys.progress() });
      toast.success('Staff account created');
      onNext();
    } catch (err) {
      if (isAxiosError(err)) {
        if (err.response?.status === 409) {
          form.setError('email', {
            message: 'An account with this email already exists.',
          });
        } else {
          toast.error('Something went wrong. Check your connection and try again.');
        }
      } else {
        toast.error('Could not create staff account. Try again.');
      }
    }
  }

  function onSkip() {
    onNext();
  }

  return (
    <div>
      <h1 className="text-2xl font-semibold mb-2">Staff Account</h1>
      <p className="text-sm text-muted-foreground mb-6">
        Add at least one staff account. Assign roles to control what each person can access.
      </p>

      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSave)} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <TextField
              control={form.control}
              name="firstName"
              label="First name"
              type="text"
              autoComplete="given-name"
            />
            <TextField
              control={form.control}
              name="lastName"
              label="Last name"
              type="text"
              autoComplete="family-name"
            />
          </div>

          <TextField control={form.control} name="email" label="Email" type="email" autoComplete="email" />

          <TextField
            control={form.control}
            name="password"
            label="Password"
            type="password"
            autoComplete="new-password"
          />

          <TextField
            control={form.control}
            name="confirmPassword"
            label="Confirm password"
            type="password"
            autoComplete="new-password"
          />

          <Controller
            control={form.control}
            name="roles"
            render={({ field, fieldState }) => (
              <div className="space-y-1">
                <OfficialRoleCheckboxes idPrefix="staff-role" value={field.value} onChange={field.onChange} />
                {fieldState.error && (
                  <p className="text-sm font-medium text-destructive">{fieldState.error.message}</p>
                )}
              </div>
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
    </div>
  );
}
