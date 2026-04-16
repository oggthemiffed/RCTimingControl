import { useState, useEffect } from 'react';
import { Link, useSearchParams, useNavigate } from 'react-router-dom';
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

const resetSchema = z
  .object({
    newPassword: z.string().min(8, 'Password must be at least 8 characters'),
    confirmPassword: z.string().min(1, 'Please confirm your password'),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  });

type ResetForm = z.infer<typeof resetSchema>;

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [isPending, setIsPending] = useState(false);
  const [tokenError, setTokenError] = useState(false);

  const token = searchParams.get('token');

  useEffect(() => {
    if (!token) {
      toast.error('Invalid or expired reset link. Please request a new one.', { duration: 8000 });
      navigate('/forgot-password', { replace: true });
    }
  }, [token, navigate]);

  const form = useForm<ResetForm>({
    resolver: zodResolver(resetSchema),
    mode: 'onBlur',
    defaultValues: { newPassword: '', confirmPassword: '' },
  });

  async function onSubmit(values: ResetForm) {
    if (!token) return;
    setIsPending(true);
    try {
      await api.post('/api/v1/auth/password-reset/confirm', {
        token,
        newPassword: values.newPassword,
      });
      toast.success('Password updated. Please sign in.', { duration: 5000 });
      navigate('/login');
    } catch (err) {
      if (isAxiosError(err) && (err.response?.status === 400 || err.response?.status === 410)) {
        setTokenError(true);
      } else {
        toast.error('Unable to reach server. Please try again.', { duration: 8000 });
      }
    } finally {
      setIsPending(false);
    }
  }

  if (tokenError) {
    return (
      <AuthLayout title="Link expired">
        <div className="space-y-4 text-center">
          <p className="text-sm text-muted-foreground">
            This reset link has expired or has already been used. Please request a new one.
          </p>
          <Link
            to="/forgot-password"
            className="text-sm text-primary underline underline-offset-4"
          >
            Request a new reset link
          </Link>
        </div>
      </AuthLayout>
    );
  }

  if (!token) {
    return null;
  }

  return (
    <AuthLayout title="Set new password" subtitle="Choose a strong password for your account">
      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
          <FormField
            control={form.control}
            name="newPassword"
            render={({ field }) => (
              <FormItem>
                <FormLabel>New password</FormLabel>
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
                <FormLabel>Confirm new password</FormLabel>
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
                Setting password...
              </>
            ) : (
              'Set password'
            )}
          </Button>
        </form>
      </Form>
    </AuthLayout>
  );
}
