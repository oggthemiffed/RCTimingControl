import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import CompetitorsPage from './CompetitorsPage';
import { adminApi } from '@/lib/adminApi';

vi.mock('@/lib/adminApi', () => ({ adminApi: { competitors: { list: vi.fn() } } }));

const api = vi.mocked(adminApi, true);

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <CompetitorsPage />
    </QueryClientProvider>,
  );
}

describe('CompetitorsPage', () => {
  beforeEach(() => vi.resetAllMocks());

  it('lists competitors and filters them by name, BRCA number or club', async () => {
    api.competitors.list.mockResolvedValue([
      { id: 1, displayName: 'Ada Lovelace', brcaNumber: '12345', homeClub: 'Analytical RC' },
      { id: 2, displayName: 'Grace Hopper', brcaNumber: null, homeClub: 'Compiler Club' },
    ]);
    renderPage();

    expect(await screen.findByText('Ada Lovelace')).toBeInTheDocument();
    expect(screen.getByText('BRCA 12345 · Analytical RC')).toBeInTheDocument();
    expect(screen.getByText('Grace Hopper')).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText('Search competitors'), { target: { value: 'compiler' } });
    expect(screen.queryByText('Ada Lovelace')).not.toBeInTheDocument();
    expect(screen.getByText('Grace Hopper')).toBeInTheDocument();
  });

  it('explains where competitors come from when there are none', async () => {
    api.competitors.list.mockResolvedValue([]);
    renderPage();

    expect(await screen.findByText('No competitors yet')).toBeInTheDocument();
  });
});
