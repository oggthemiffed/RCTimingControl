import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { BackupsHelp } from './BackupsHelp';
import { ResultsExportsHelp } from './ResultsExportsHelp';
import { DecoderHelp } from './DecoderHelp';
import { EventManagementHelp } from './EventManagementHelp';
import { CheckInHelp } from './CheckInHelp';
import { RaceControlHelp } from './RaceControlHelp';
import { ChampionshipHelp } from './ChampionshipHelp';
import { PracticeHelp } from './PracticeHelp';
import { RefereeHelp } from './RefereeHelp';

describe('help panels', () => {
  it('explain the admin screens', () => {
    const { unmount } = render(<BackupsHelp />);
    expect(screen.getByText('Back up now:')).toBeTruthy();
    unmount();
    const second = render(<ResultsExportsHelp />);
    expect(screen.getByText('Send now:')).toBeTruthy();
    second.unmount();
    render(<DecoderHelp />);
    expect(screen.getByText('Test Connection:')).toBeTruthy();
  });

  it('cover the entry imports, swaps on re-import and corrections', () => {
    const first = render(<EventManagementHelp />);
    expect(screen.getByText('Import from a CSV file:')).toBeTruthy();
    expect(screen.getByText('Entry feed:')).toBeTruthy();
    first.unmount();
    const second = render(<CheckInHelp />);
    expect(screen.getByText('After a re-import:')).toBeTruthy();
    second.unmount();
    render(<RaceControlHelp />);
    expect(screen.getByText('Corrections after the finish:')).toBeTruthy();
    expect(screen.getByText('Boards for screens:')).toBeTruthy();
  });

  it('match the Championship, Practice and Referee screens', () => {
    const first = render(<ChampionshipHelp />);
    expect(screen.getByText('Config:')).toBeTruthy();
    expect(screen.getByText('Link Event:')).toBeTruthy();
    first.unmount();
    const second = render(<PracticeHelp />);
    expect(screen.getByText('Print Results:')).toBeTruthy();
    expect(screen.getByText('Names on the table:')).toBeTruthy();
    second.unmount();
    render(<RefereeHelp />);
    expect(screen.getByText('Unknown transponders:')).toBeTruthy();
  });

  it('never mention racer accounts, portals or self-service', () => {
    for (const Panel of [ChampionshipHelp, PracticeHelp, RefereeHelp, EventManagementHelp, CheckInHelp, RaceControlHelp]) {
      const { container, unmount } = render(<Panel />);
      expect(container.textContent).not.toMatch(/portal|self-service|racer account|log ?in as a racer/i);
      unmount();
    }
  });
});
