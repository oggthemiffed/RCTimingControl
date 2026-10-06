import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import MeetingGuidePage from './MeetingGuidePage';

describe('MeetingGuidePage', () => {
  it('renders the page title', () => {
    render(<MemoryRouter><MeetingGuidePage /></MemoryRouter>);
    expect(screen.getByText('Race Meeting Guide')).toBeTruthy();
  });

  it('renders the Print / Save as PDF button', () => {
    render(<MemoryRouter><MeetingGuidePage /></MemoryRouter>);
    expect(screen.getByText(/Print \/ Save as PDF/i)).toBeTruthy();
  });

  it('covers corrections after the finish and the boards', () => {
    render(<MemoryRouter><MeetingGuidePage /></MemoryRouter>);
    expect(screen.getByText('Corrections after the finish:')).toBeTruthy();
    expect(screen.getByText(/10\. Spectator Boards and the Streaming Overlay/)).toBeTruthy();
    expect(screen.getByText('Streaming overlay:')).toBeTruthy();
  });
});
