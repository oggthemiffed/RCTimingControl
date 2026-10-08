import { describe, it, expect } from 'vitest';

import type { CompetitorSummaryDto } from '@/lib/adminApi';
import { formatCompetitorMeta, matchesCompetitor } from './competitors';

const jane = { id: 1, displayName: 'Jane Doe', brcaNumber: '12345', homeClub: 'Hilltop RC' } as CompetitorSummaryDto;

describe('formatCompetitorMeta', () => {
  it('joins the BRCA number and club', () => {
    expect(formatCompetitorMeta(jane)).toBe('BRCA 12345 · Hilltop RC');
  });

  it('leaves out what is not known', () => {
    expect(formatCompetitorMeta({ brcaNumber: null, homeClub: 'Hilltop RC' })).toBe('Hilltop RC');
    expect(formatCompetitorMeta({ brcaNumber: null, homeClub: null })).toBe('');
  });
});

describe('matchesCompetitor', () => {
  it.each(['jane', ' DOE ', '234', 'hilltop', ''])('matches %j', search => {
    expect(matchesCompetitor(jane, search)).toBe(true);
  });

  it('does not match someone else', () => {
    expect(matchesCompetitor(jane, 'smith')).toBe(false);
  });
});
