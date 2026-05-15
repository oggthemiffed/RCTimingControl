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
});
