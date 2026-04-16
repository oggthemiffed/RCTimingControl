import { useState } from 'react';
import { Link } from 'react-router-dom';
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

const forgotSchema = z.object({
  email: z.string().email('Valid email required'),
});

type ForgotForm = z.infer<typeof forgotSchema>;

export default function ForgotPasswordPage() {
  const [submitted, setSubmitted] = useState(false);
  const [isPending, setIsPending] = useState(false);

  const form = useForm<ForgotForm>({
    resolver: zodResolver(forgotSchema),
    mode: 'onBlur',
    defaultValues: { email: '' },
  });

  async function onSubmit(values: ForgotForm) {
    setIsPending(true);
    try {
      await api.post('/api/v1/auth/password-reset/request', { email: values.email });
    } catch (err) {
      if (isAxiosError(err) && !err.response) {
        toast.error('Unable to reach server. Please try again.', { duration: 8000 });
        setIsPending(false);
        return;
      }
      // Any server error (including 4xx) — still show success (no email enumeration)
    } finally {
      setIsPending(false);
    }
    // Always show success regardless of whether email exists
    setSubmitted(true);
  }

  return (
    <AuthLayout
      title="Reset your password"
      subtitle={submitted ? undefined : "Enter your email and we'll send a reset link"}
      footer={
        <p className="text-sm text-muted-foreground">
          <Link to="/login" className="text-primary underline underline-offset-4">
            Back to sign in
          </Link>
        </p>
      }
    >
      {submitted ? (
        <p className="text-sm text-center text-muted-foreground">
          If an account exists for that email, a reset link has been sent.
        </p>
      ) : (
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
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
            <Button type="submit" className="w-full" disabled={isPending}>
              {isPending ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Sending...
                </>
              ) : (
                'Send reset link'
              )}
            </Button>
          </form>
        </Form>
      )}
    </AuthLayout>
  );
}
