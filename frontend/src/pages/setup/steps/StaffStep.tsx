import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useQueryClient } from '@tanstack/react-query';
import { isAxiosError } from 'axios';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Checkbox } from '@/components/ui/checkbox';
import {
  Form,
  FormField,
  FormItem,
  FormLabel,
  FormControl,
  FormMessage,
} from '@/components/ui/form';
import { createSetupStaff } from '@/lib/setupApi';

const ROLES = [
  { value: 'ADMIN', label: 'Admin' },
  { value: 'RACE_DIRECTOR', label: 'Race Director' },
  { value: 'REFEREE', label: 'Referee' },
] as const;

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
      queryClient.invalidateQueries({ queryKey: ['setup-status'] });
      queryClient.invalidateQueries({ queryKey: ['setup-progress'] });
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
            <FormField
              control={form.control}
              name="firstName"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>First name</FormLabel>
                  <FormControl>
                    <Input type="text" autoComplete="given-name" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="lastName"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Last name</FormLabel>
                  <FormControl>
                    <Input type="text" autoComplete="family-name" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>

          <FormField
            control={form.control}
            name="email"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Email</FormLabel>
                <FormControl>
                  <Input type="email" autoComplete="email" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="password"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Password</FormLabel>
                <FormControl>
                  <Input type="password" autoComplete="new-password" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="confirmPassword"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Confirm password</FormLabel>
                <FormControl>
                  <Input type="password" autoComplete="new-password" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          {/* Role checkboxes */}
          <Controller
            control={form.control}
            name="roles"
            render={({ field, fieldState }) => (
              <FormItem>
                <FormLabel>Roles</FormLabel>
                <div className="space-y-2 pt-1">
                  {ROLES.map((role) => {
                    const checked = field.value.includes(role.value);
                    return (
                      <label
                        key={role.value}
                        className="flex items-center gap-2 cursor-pointer"
                      >
                        <Checkbox
                          checked={checked}
                          onCheckedChange={(isChecked) => {
                            if (isChecked) {
                              field.onChange([...field.value, role.value]);
                            } else {
                              field.onChange(
                                field.value.filter((r) => r !== role.value),
                              );
                            }
                          }}
                        />
                        <span className="text-sm">{role.label}</span>
                      </label>
                    );
                  })}
                </div>
                {fieldState.error && (
                  <p className="text-sm font-medium text-destructive mt-1">
                    {fieldState.error.message}
                  </p>
                )}
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
        href="/admin/racers"
        className="text-sm text-muted-foreground underline mt-4 inline-block"
      >
        Manage more in Admin →
      </a>
    </div>
  );
}
