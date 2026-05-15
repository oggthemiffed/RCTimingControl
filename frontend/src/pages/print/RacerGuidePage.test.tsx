import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import RacerGuidePage from './RacerGuidePage';

describe('RacerGuidePage', () => {
  it('renders the page title', () => {
    render(<MemoryRouter><RacerGuidePage /></MemoryRouter>);
    expect(screen.getByText('Racer Quick-Start Guide')).toBeTruthy();
  });

  it('renders the Print / Save as PDF button', () => {
    render(<MemoryRouter><RacerGuidePage /></MemoryRouter>);
    expect(screen.getByText(/Print \/ Save as PDF/i)).toBeTruthy();
  });
});
