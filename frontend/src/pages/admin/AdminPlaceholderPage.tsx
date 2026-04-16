import { useNavigate } from 'react-router-dom';
import AuthLayout from '@/components/layout/AuthLayout';
import { Button } from '@/components/ui/button';
import { useAuth } from '@/hooks/useAuth';

export default function AdminPlaceholderPage() {
  const { logout } = useAuth();
  const navigate = useNavigate();

  function handleSignOut() {
    logout();
    navigate('/login');
  }

  return (
    <AuthLayout title="Admin Panel" subtitle="">
      <div className="space-y-4">
        <p className="text-sm text-center text-muted-foreground">
          Admin configuration is coming in Phase 3.
        </p>
        <Button variant="outline" className="w-full" onClick={handleSignOut}>
          Sign out
        </Button>
      </div>
    </AuthLayout>
  );
}
