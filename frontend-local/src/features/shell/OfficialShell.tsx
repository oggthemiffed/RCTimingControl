// Post-login shell for an authenticated official: a simple tab switcher between Race Control
// (default) and the Check-in Desk. Local useState toggle, same pattern as CheckInDesk's own
// `showReassign` — no router, no external state library.
import { useState } from 'react';
import RaceControl from '@/features/race-control/RaceControl';
import CheckInDesk from '@/features/checkin/CheckInDesk';

type Tab = 'race-control' | 'checkin';

export default function OfficialShell() {
  const [tab, setTab] = useState<Tab>('race-control');

  return (
    <div className="flex min-h-screen flex-col">
      <nav className="flex gap-2 border-b bg-white p-3">
        <button
          type="button"
          onClick={() => setTab('race-control')}
          aria-current={tab === 'race-control'}
          className={`rounded px-3 py-2 text-sm font-medium ${
            tab === 'race-control' ? 'bg-blue-600 text-white' : 'text-slate-600 hover:bg-slate-100'
          }`}
        >
          Race Control
        </button>
        <button
          type="button"
          onClick={() => setTab('checkin')}
          aria-current={tab === 'checkin'}
          className={`rounded px-3 py-2 text-sm font-medium ${
            tab === 'checkin' ? 'bg-blue-600 text-white' : 'text-slate-600 hover:bg-slate-100'
          }`}
        >
          Check-in Desk
        </button>
      </nav>

      <div className="flex-1">{tab === 'race-control' ? <RaceControl /> : <CheckInDesk />}</div>
    </div>
  );
}
