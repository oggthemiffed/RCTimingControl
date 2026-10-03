import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import PublicChampionshipPage from './PublicChampionshipPage';

vi.mock('@/lib/raceControlApi', () => ({
  getPublicChampionshipStandings: vi.fn(),
}));

import { getPublicChampionshipStandings } from '@/lib/raceControlApi';

describe('PublicChampionshipPage', () => {
  it("shows each competitor's display name, including drivers with no login", async () => {
    vi.mocked(getPublicChampionshipStandings).mockResolvedValue([
      { driverId: 7, displayName: 'Wendy Walkin', racingClassId: 1, totalPoints: 10, rounds: [] },
      { driverId: 3, displayName: 'Larry Login', racingClassId: 1, totalPoints: 8, rounds: [] },
    ]);

    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/championships/5']}>
          <Routes>
            <Route path="/championships/:id" element={<PublicChampionshipPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(await screen.findByText('Wendy Walkin')).toBeInTheDocument();
    expect(screen.getByText('Larry Login')).toBeInTheDocument();
    expect(getPublicChampionshipStandings).toHaveBeenCalledWith(5);
  });
});
