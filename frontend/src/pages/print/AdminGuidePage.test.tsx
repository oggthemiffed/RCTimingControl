import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import AdminGuidePage from './AdminGuidePage';

describe('AdminGuidePage', () => {
  it('renders the page title', () => {
    render(<MemoryRouter><AdminGuidePage /></MemoryRouter>);
    expect(screen.getByText('Admin Configuration Guide')).toBeTruthy();
  });

  it('renders the Print / Save as PDF button', () => {
    render(<MemoryRouter><AdminGuidePage /></MemoryRouter>);
    expect(screen.getByText(/Print \/ Save as PDF/i)).toBeTruthy();
  });

  it('covers the CSV import, entry feed, swaps on re-import and the admin screens', () => {
    render(<MemoryRouter><AdminGuidePage /></MemoryRouter>);
    expect(screen.getByText('Import from a CSV file:')).toBeTruthy();
    expect(screen.getByText('Pull entries from a web address:')).toBeTruthy();
    expect(screen.getByText('Swaps survive a re-import:')).toBeTruthy();
    expect(screen.getByText(/9\. Backups, Results to RaceHub, Live Feed and Decoder/)).toBeTruthy();
    expect(screen.getByText('Back up now')).toBeTruthy();
    expect(screen.getByText('Test Connection')).toBeTruthy();
  });
});
