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
import { useAuth } from '@/hooks/useAuth';

const loginSchema = z.object({
  email: z.string().email('Valid email required'),
  password: z.string().min(1, 'Password required'),
});

type LoginForm = z.infer<typeof loginSchema>;

export default function LoginPage() {
  const { login } = useAuth();
  const [isPending, setIsPending] = useState(false);

  const form = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
    mode: 'onBlur',
    defaultValues: { email: '', password: '' },
  });

  async function onSubmit(values: LoginForm) {
    setIsPending(true);
    try {
      await login(values.email, values.password);
    } catch (err) {
      if (isAxiosError(err) && err.response?.status === 401) {
        form.setError('password', { message: 'Invalid email or password' });
      } else {
        toast.error('Unable to reach server. Please try again.', { duration: 8000 });
      }
    } finally {
      setIsPending(false);
    }
  }

  return (
    <AuthLayout
      title="Sign in"
      subtitle="Enter your email and password"
      footer={
        <p className="text-sm text-muted-foreground">
          Don't have an account?{' '}
          <Link to="/register" className="text-primary underline underline-offset-4">
            Register
          </Link>
        </p>
      }
    >
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
          <FormField
            control={form.control}
            name="password"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Password</FormLabel>
                <FormControl>
                  <Input type="password" autoComplete="current-password" {...field} />
                </FormControl>
                <FormMessage />
                <div className="text-right">
                  <Link
                    to="/forgot-password"
                    className="text-sm text-primary underline underline-offset-4"
                  >
                    Forgot password?
                  </Link>
                </div>
              </FormItem>
            )}
          />
          <Button type="submit" className="w-full" disabled={isPending}>
            {isPending ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Signing in...
              </>
            ) : (
              'Sign in'
            )}
          </Button>
        </form>
      </Form>
    </AuthLayout>
  );
}
