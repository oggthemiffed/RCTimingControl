import { describe, it, expect } from 'vitest';
import { fmtMs, roundName } from './format';

describe('fmtMs', () => {
  it('shows a dash for missing or zero times', () => {
    expect(fmtMs(null)).toBe('—');
    expect(fmtMs(undefined)).toBe('—');
    expect(fmtMs(0)).toBe('—');
  });

  it('shows seconds and milliseconds under a minute', () => {
    expect(fmtMs(9050)).toBe('9.050');
  });

  it('shows minutes over a minute', () => {
    expect(fmtMs(65_007)).toBe('1:05.007');
    expect(fmtMs(300_000)).toBe('5:00.000');
  });
});

describe('roundName', () => {
  it('names finals with their letter', () => {
    expect(roundName({ roundType: 'FINAL', roundNumber: 1, finalLetter: 'A' })).toBe('Final A');
  });

  it('names qualifiers long or short', () => {
    const q2 = { roundType: 'QUALIFIER', roundNumber: 2, finalLetter: null } as const;
    expect(roundName(q2)).toBe('Qualifier 2');
    expect(roundName(q2, true)).toBe('Q2');
  });
});
