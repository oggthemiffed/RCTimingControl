import { Outlet, NavLink } from 'react-router-dom';
import { User, Car, Radio, FileText, LogOut } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';

const TrophyIcon = () => <i className="ri-trophy-line h-5 w-5" aria-hidden="true" />;

const navItems = [
  { to: '/racer/profile', label: 'Profile', Icon: User },
  { to: '/racer/cars', label: 'Cars', Icon: Car },
  { to: '/racer/transponders', label: 'Transponders', Icon: Radio },
  { to: '/racer/entries', label: 'Entries', Icon: FileText },
  { to: '/racer/results', label: 'Results', Icon: TrophyIcon },
] as const;

export default function RacerPortalLayout() {
  const { logout } = useAuth();

  return (
    <div className="min-h-screen flex flex-col bg-background">
      {/* Desktop top nav — md+ */}
      <nav className="hidden md:flex items-center border-b px-6 h-14 gap-6">
        <span className="font-semibold mr-4">RC Timing</span>
        {navItems.map(({ to, label }) => (
          <NavLink
            key={to}
            to={to}
            aria-label={label}
            className={({ isActive }) =>
              isActive
                ? 'text-primary font-medium border-b-2 border-primary pb-0.5'
                : 'text-muted-foreground hover:text-foreground'
            }
          >
            {label}
          </NavLink>
        ))}
        <button
          onClick={logout}
          aria-label="Log out"
          className="ml-auto flex items-center gap-1.5 text-muted-foreground hover:text-foreground text-sm"
        >
          <LogOut className="h-4 w-4" aria-hidden="true" />
          Log out
        </button>
      </nav>

      {/* Content — pb-16 reserves room for mobile bottom nav */}
      <main className="flex-1 pb-16 md:pb-0 p-4 md:p-6">
        <Outlet />
      </main>

      {/* Mobile bottom nav — hidden at md+ */}
      <nav className="fixed bottom-0 inset-x-0 flex md:hidden border-t bg-background z-10 h-14">
        {navItems.map(({ to, label, Icon }) => (
          <NavLink
            key={to}
            to={to}
            aria-label={label}
            className={({ isActive }) =>
              `flex-1 flex flex-col items-center justify-center py-2 text-xs gap-1 ${
                isActive ? 'text-primary' : 'text-muted-foreground'
              }`
            }
          >
            {({ isActive }) => (
              <>
                <Icon className="h-5 w-5" aria-hidden="true" />
                <span aria-current={isActive ? 'page' : undefined}>{label}</span>
              </>
            )}
          </NavLink>
        ))}
        <button
          onClick={logout}
          aria-label="Log out"
          className="flex-1 flex flex-col items-center justify-center py-2 text-xs gap-1 text-muted-foreground"
        >
          <LogOut className="h-5 w-5" aria-hidden="true" />
          <span>Log out</span>
        </button>
      </nav>
    </div>
  );
}
