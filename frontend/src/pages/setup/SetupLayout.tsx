import { useState } from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { Loader2, Menu } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { Sheet, SheetContent, SheetHeader, SheetTitle } from '@/components/ui/sheet';
import { useSetupStatus, useSetupProgress } from '@/hooks/setup/useSetupProgress';
import { useAuth } from '@/hooks/useAuth';
import AdminBootstrapGate from './AdminBootstrapGate';

// ── Step sidebar item ──────────────────────────────────────────────────────

type StepState = 'complete' | 'current' | 'incomplete';

function StepItem({
  number,
  label,
  state,
}: {
  number: number;
  label: string;
  state: StepState;
}) {
  return (
    <div
      aria-current={state === 'current' ? 'step' : undefined}
      className={
        state === 'current'
          ? 'flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-semibold text-primary border-l-[3px] border-primary pl-[calc(0.75rem-3px)] w-full text-left'
          : state === 'complete'
            ? 'flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium text-foreground w-full text-left'
            : 'flex items-center gap-3 px-3 py-2 rounded-lg text-sm text-muted-foreground w-full text-left'
      }
    >
      <i
        className={
          state === 'complete'
            ? 'ri-checkbox-circle-fill text-[var(--flag-green)]'
            : state === 'current'
              ? 'ri-record-circle-line text-primary'
              : 'ri-circle-line'
        }
        aria-hidden="true"
      />
      <span className="text-xs text-muted-foreground mr-1">{number}.</span>
      {label}
    </div>
  );
}

// ── Sidebar content ────────────────────────────────────────────────────────

const STEPS: { key: 'club' | 'track' | 'format' | 'staff' | 'decoder'; label: string }[] = [
  { key: 'club', label: 'Club Profile' },
  { key: 'track', label: 'Track' },
  { key: 'format', label: 'Race Format' },
  { key: 'staff', label: 'Staff Account' },
  { key: 'decoder', label: 'Decoder Config' },
];

function SidebarContent({ onNavClick }: { onNavClick?: () => void }) {
  const { data: progress } = useSetupProgress();

  function getStepState(key: string, index: number): StepState {
    if (!progress) return index === 0 ? 'current' : 'incomplete';
    const isComplete = progress[key as keyof typeof progress] as boolean;
    if (isComplete) return 'complete';
    // Current = first incomplete step
    const allComplete = STEPS.slice(0, index).every(
      (s) => (progress[s.key as keyof typeof progress] as boolean),
    );
    return allComplete ? 'current' : 'incomplete';
  }

  return (
    <div className="flex flex-col h-full">
      {/* Brand */}
      <div className="px-4 py-5">
        <span className="font-semibold text-base">RC Timing — Setup</span>
      </div>
      <Separator />

      {/* Step navigation */}
      <nav className="flex-1 overflow-y-auto px-3 py-4 space-y-1" onClick={onNavClick}>
        {STEPS.map(({ key, label }, index) => (
          <StepItem key={key} number={index + 1} label={label} state={getStepState(key, index)} />
        ))}
      </nav>

      <Separator />

      {/* Skip wizard link */}
      <div className="px-4 py-4">
        <Button variant="ghost" size="sm" className="text-sm text-muted-foreground w-full" asChild>
          <a href="/admin">Skip wizard</a>
        </Button>
      </div>
    </div>
  );
}

// ── Layout ─────────────────────────────────────────────────────────────────

export default function SetupLayout() {
  const [sheetOpen, setSheetOpen] = useState(false);
  const { data: statusData, isLoading: statusLoading } = useSetupStatus();
  const { user } = useAuth();

  if (statusLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  // Pre-gate: setup not complete and user not authenticated → show bootstrap form
  if (statusData?.setupComplete === false && !user) {
    return <AdminBootstrapGate />;
  }

  // Pitfall 5: setup complete but user is not ADMIN → redirect to login
  // This prevents non-admin users from accessing the wizard after first-run
  if (statusData?.setupComplete === true && (!user || !user.roles.includes('ADMIN'))) {
    return <Navigate to="/login" replace />;
  }

  return (
    <div className="min-h-screen bg-background">
      {/* Desktop: fixed left sidebar */}
      <aside className="hidden md:flex fixed inset-y-0 left-0 w-60 border-r bg-card flex-col z-10">
        <SidebarContent />
      </aside>

      {/* Mobile: top bar with hamburger */}
      <header className="md:hidden flex items-center border-b h-14 px-4 sticky top-0 bg-background z-10">
        <Button
          variant="ghost"
          size="icon-sm"
          onClick={() => setSheetOpen(true)}
          aria-label="Open navigation"
        >
          <Menu className="h-5 w-5" />
        </Button>
        <span className="ml-3 font-semibold text-sm">RC Timing — Setup</span>
      </header>

      {/* Mobile: Sheet drawer */}
      <Sheet open={sheetOpen} onOpenChange={setSheetOpen}>
        <SheetContent side="left" showCloseButton className="w-72 p-0">
          <SheetHeader className="sr-only">
            <SheetTitle>Setup Steps</SheetTitle>
          </SheetHeader>
          <SidebarContent onNavClick={() => setSheetOpen(false)} />
        </SheetContent>
      </Sheet>

      {/* Main content */}
      <main className="md:pl-60">
        <div className="px-6 pt-6 pb-10 min-h-screen">
          <Outlet />
          {/* Plans 05–06 will render the active step here. */}
          <div className="text-muted-foreground text-sm">Plans 05–06 will render the active step here.</div>
        </div>
      </main>
    </div>
  );
}
