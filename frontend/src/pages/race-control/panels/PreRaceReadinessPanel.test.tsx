import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { PreRaceReadinessPanel } from './PreRaceReadinessPanel';

vi.mock('@/hooks/race-control/usePreRaceReadiness', () => ({
  usePreRaceReadiness: () => ({
    isLoading: false,
    error: null,
    data: {
      raceId: 1,
      raceLabel: 'Qualifying 1 — Stock — Heat 1',
      firstRaceOfEvent: true,
      marshalDuty: [],
      gridCall: [
        {
          gridPosition: 1,
          entryId: 10,
          driverName: 'Jane Doe',
          carNumber: null,
          className: 'Stock',
          checkedIn: true,
          racehubArrival: null,
        },
        {
          gridPosition: 2,
          entryId: 11,
          driverName: 'John Smith',
          carNumber: null,
          className: 'Stock',
          checkedIn: false,
          racehubArrival: 'ARRIVED',
        },
      ],
    },
  }),
}));

describe('PreRaceReadinessPanel', () => {
  it('shows who has not checked in, with the RaceHub arrival mark beside it', () => {
    render(<PreRaceReadinessPanel raceId={1} />);

    expect(screen.getByText('1 not checked in')).toBeInTheDocument();
    expect(screen.getByText('Checked in')).toBeInTheDocument();
    expect(screen.getByText('Not checked in')).toBeInTheDocument();
    expect(screen.getByText('RaceHub: arrived')).toBeInTheDocument();
  });
});
