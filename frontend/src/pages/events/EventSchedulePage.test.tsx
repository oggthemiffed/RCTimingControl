import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import EventSchedulePage from './EventSchedulePage';

vi.mock('@/lib/raceControlApi', () => ({
  getEventSchedule: vi.fn(),
}));

import { getEventSchedule } from '@/lib/raceControlApi';
import type { EventScheduleDto } from '@/lib/raceControlApi';

const baseEvent: EventScheduleDto = {
  id: 1,
  name: 'Spring Regional',
  eventDate: '2026-09-01',
  entryAvailability: 'ENTRY_OPEN',
  finishedRaceIds: [],
  championshipId: null,
  lastSyncedAt: null,
  syncDelayed: false,
  incompleteData: false,
};

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <EventSchedulePage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.mocked(getEventSchedule).mockReset();
});

describe('EventSchedulePage sync status badges (U14)', () => {
  it('shows no sync badge for an event never run via the Local Race Day Program', async () => {
    vi.mocked(getEventSchedule).mockResolvedValue([baseEvent]);

    renderPage();

    await screen.findByText('Spring Regional');
    expect(screen.queryByText(/may be delayed/i)).toBeNull();
    expect(screen.queryByText(/incomplete data/i)).toBeNull();
  });

  it('shows the delayed badge when syncDelayed is true', async () => {
    vi.mocked(getEventSchedule).mockResolvedValue([
      { ...baseEvent, lastSyncedAt: '2026-09-01T10:00:00Z', syncDelayed: true },
    ]);

    renderPage();

    expect(await screen.findByText(/may be delayed/i)).toBeInTheDocument();
    expect(screen.queryByText(/incomplete data/i)).toBeNull();
  });

  it('shows the permanent incomplete-data badge when a device loss was declared', async () => {
    vi.mocked(getEventSchedule).mockResolvedValue([
      { ...baseEvent, lastSyncedAt: '2026-09-01T10:00:00Z', incompleteData: true },
    ]);

    renderPage();

    expect(await screen.findByText(/incomplete data — device loss declared/i)).toBeInTheDocument();
  });

  it('shows both badges together when data is both delayed and incomplete', async () => {
    vi.mocked(getEventSchedule).mockResolvedValue([
      { ...baseEvent, lastSyncedAt: '2026-09-01T10:00:00Z', syncDelayed: true, incompleteData: true },
    ]);

    renderPage();

    expect(await screen.findByText(/may be delayed/i)).toBeInTheDocument();
    expect(await screen.findByText(/incomplete data/i)).toBeInTheDocument();
  });
});
