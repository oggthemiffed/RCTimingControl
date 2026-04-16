import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { isAxiosError } from 'axios';
import { Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import AuthLayout from '@/components/layout/AuthLayout';
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
import api from '@/lib/api';

const registerSchema = z
  .object({
    firstName: z.string().min(1, 'First name required').max(100, 'First name too long'),
    lastName: z.string().min(1, 'Last name required').max(100, 'Last name too long'),
    email: z.string().email('Valid email required'),
    password: z.string().min(8, 'Password must be at least 8 characters'),
    confirmPassword: z.string().min(1, 'Please confirm your password'),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  });

type RegisterForm = z.infer<typeof registerSchema>;

export default function RegisterPage() {
  const navigate = useNavigate();
  const [isPending, setIsPending] = useState(false);

  const form = useForm<RegisterForm>({
    resolver: zodResolver(registerSchema),
    mode: 'onBlur',
    defaultValues: {
      firstName: '',
      lastName: '',
      email: '',
      password: '',
      confirmPassword: '',
    },
  });

  async function onSubmit(values: RegisterForm) {
    setIsPending(true);
    try {
      await api.post('/api/v1/auth/register', {
        firstName: values.firstName,
        lastName: values.lastName,
        email: values.email,
        password: values.password,
      });
      toast.success('Account created. Please sign in.', { duration: 5000 });
      navigate('/login');
    } catch (err) {
      if (isAxiosError(err)) {
        if (err.response?.status === 409) {
          form.setError('email', { message: 'An account with this email already exists' });
        } else if (err.response?.status === 400) {
          const errors = err.response.data?.errors as Record<string, string> | undefined;
          if (errors) {
            Object.entries(errors).forEach(([field, message]) => {
              form.setError(field as keyof RegisterForm, { message });
            });
          } else {
            toast.error('Please check your details and try again.', { duration: 8000 });
          }
        } else {
          toast.error('Unable to reach server. Please try again.', { duration: 8000 });
        }
      }
    } finally {
      setIsPending(false);
    }
  }

  return (
    <AuthLayout
      title="Create account"
      subtitle="Join the club portal"
      footer={
        <p className="text-sm text-muted-foreground">
          Already have an account?{' '}
          <Link to="/login" className="text-primary underline underline-offset-4">
            Sign in
          </Link>
        </p>
      }
    >
      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
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
          <Button type="submit" className="w-full" disabled={isPending}>
            {isPending ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Creating account...
              </>
            ) : (
              'Create account'
            )}
          </Button>
        </form>
      </Form>
    </AuthLayout>
  );
}
