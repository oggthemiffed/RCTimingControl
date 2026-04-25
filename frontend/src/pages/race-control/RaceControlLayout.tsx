import { Outlet, NavLink, useParams } from 'react-router-dom';
import { Flag, Shield, LogOut } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';

export default function RaceControlLayout() {
  const { eventId } = useParams<{ eventId: string }>();
  const { user, logout } = useAuth();

  const base = `/race-control/event/${eventId}`;

  return (
    <div className="flex flex-col h-screen overflow-hidden bg-background">
      {/* Top header */}
      <header className="flex items-center h-12 px-4 border-b bg-card shrink-0 gap-4">
        <span className="font-semibold text-sm">RC Timing — Race Control</span>

        <nav className="flex items-center gap-1 ml-4">
          <NavLink
            to={base}
            end
            className={({ isActive }) =>
              `flex items-center gap-1.5 px-3 py-1.5 rounded text-sm transition-colors ${
                isActive
                  ? 'bg-primary/10 text-primary font-medium'
                  : 'text-muted-foreground hover:text-foreground hover:bg-muted'
              }`
            }
          >
            <Flag className="h-3.5 w-3.5" />
            Cockpit
          </NavLink>
          <NavLink
            to={`${base}/referee`}
            className={({ isActive }) =>
              `flex items-center gap-1.5 px-3 py-1.5 rounded text-sm transition-colors ${
                isActive
                  ? 'bg-primary/10 text-primary font-medium'
                  : 'text-muted-foreground hover:text-foreground hover:bg-muted'
              }`
            }
          >
            <Shield className="h-3.5 w-3.5" />
            Referee
          </NavLink>
        </nav>

        <div className="ml-auto flex items-center gap-3">
          <span className="text-xs text-muted-foreground hidden sm:block">
            {user ? `${user.firstName} ${user.lastName}` : ''}
          </span>
          <Button variant="ghost" size="icon-sm" onClick={logout} aria-label="Log out">
            <LogOut className="h-4 w-4" />
          </Button>
        </div>
      </header>

      <Separator />

      {/* Page content fills remaining height */}
      <div className="flex-1 overflow-hidden">
        <Outlet />
      </div>
    </div>
  );
}
