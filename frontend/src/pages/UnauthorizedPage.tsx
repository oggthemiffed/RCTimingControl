import { useNavigate } from 'react-router-dom';
import { ShieldOff } from 'lucide-react';
import { Button } from '@/components/ui/button';

export default function UnauthorizedPage() {
  const navigate = useNavigate();

  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4">
      <ShieldOff className="h-10 w-10 text-muted-foreground" />
      <h1 className="text-2xl font-semibold">Access denied</h1>
      <p className="text-muted-foreground">You don't have permission to view this page.</p>
      <Button variant="outline" onClick={() => navigate(-1)}>
        Go back
      </Button>
    </div>
  );
}
