import { Outlet, Link, useParams, useMatch } from 'react-router-dom';
import { Flag, Shield, LogOut } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { ForwarderStatusBar } from './panels/ForwarderStatusBar';

export default function RaceControlLayout() {
  const { eventId } = useParams<{ eventId: string }>();
  const { user, logout } = useAuth();
  const refereeMatch = useMatch('/race-control/event/:eventId/referee');

  const base = `/race-control/event/${eventId}`;
  const isReferee = !!refereeMatch;

  function navClass(active: boolean) {
    return `flex items-center gap-1.5 px-3 py-1.5 rounded text-sm transition-colors ${
      active
        ? 'bg-primary text-primary-foreground font-medium'
        : 'text-muted-foreground hover:text-foreground hover:bg-muted'
    }`;
  }

  return (
    <div className="flex flex-col h-screen overflow-hidden bg-background">
      {/* Top header */}
      <header className="flex items-center h-12 px-4 border-b bg-card shrink-0 gap-4">
        <span className="font-semibold text-sm">RC Timing — Race Control</span>

        <nav className="flex items-center gap-1 ml-4">
          <Link to={base} className={navClass(!isReferee)}>
            <Flag className="h-3.5 w-3.5" />
            Cockpit
          </Link>
          <Link to={`${base}/referee`} className={navClass(isReferee)}>
            <Shield className="h-3.5 w-3.5" />
            Referee
          </Link>
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

      <ForwarderStatusBar />

      {/* Page content fills remaining height */}
      <div className="flex-1 overflow-hidden">
        <Outlet />
      </div>
    </div>
  );
}
