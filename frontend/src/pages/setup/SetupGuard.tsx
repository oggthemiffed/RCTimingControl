import { Navigate, useLocation } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import { useSetupStatus } from '@/hooks/setup/useSetupProgress';

export default function SetupGuard({ children }: { children: React.ReactNode }) {
  const { data, isLoading } = useSetupStatus();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-primary" role="status" aria-label="Loading" />
      </div>
    );
  }

  // Pitfall 1: do NOT redirect when already on /setup, else infinite redirect loop (RESEARCH.md Pitfall 1)
  if (!data?.setupComplete && !location.pathname.startsWith('/setup')) {
    return <Navigate to="/setup" replace />;
  }

  return <>{children}</>;
}
