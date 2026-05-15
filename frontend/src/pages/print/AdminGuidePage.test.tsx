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
});
