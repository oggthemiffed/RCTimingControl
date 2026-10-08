import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { RoundGeneratorWizard } from './RoundGeneratorWizard';
import { adminApi, type EventDetailDto, type RacingClassDto } from '@/lib/adminApi';

vi.mock('sonner', () => ({ toast: { success: vi.fn(), error: vi.fn() } }));
vi.mock('@/lib/adminApi', () => ({
  adminApi: { getEvent: vi.fn(), listRacingClasses: vi.fn(), generateRounds: vi.fn() },
}));

function wizard(open: boolean) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return (
    <QueryClientProvider client={client}>
      <RoundGeneratorWizard open={open} onOpenChange={vi.fn()} eventId={5} />
    </QueryClientProvider>
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  vi.mocked(adminApi.getEvent).mockResolvedValue({
    id: 5,
    classes: [{ id: 11, racingClassId: 3, configSnapshot: { type: 'TIMED', durationMinutes: 5 } }],
  } as unknown as EventDetailDto);
  vi.mocked(adminApi.listRacingClasses).mockResolvedValue([{ id: 3, name: 'Mod Buggy' }] as RacingClassDto[]);
});

describe('RoundGeneratorWizard', () => {
  it('starts again from the event\'s classes after a Cancel, not from the rows as they were edited', async () => {
    const { rerender } = render(wizard(true));
    await screen.findByText('Mod Buggy');
    const finals = () => screen.getAllByRole('spinbutton')[3] as HTMLInputElement;
    const before = finals().value;

    fireEvent.change(finals(), { target: { value: '3' } });
    expect(finals().value).toBe('3');
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

    rerender(wizard(false));
    rerender(wizard(true));

    await waitFor(() => expect(screen.getByText('Mod Buggy')).toBeInTheDocument());
    expect(finals().value).toBe(before);
  });
});
