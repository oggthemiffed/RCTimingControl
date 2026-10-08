import { describe, it, expect } from 'vitest';
import { parseLocalDate } from './utils';

describe('parseLocalDate', () => {
  it('gives the same calendar day in the local time zone, whatever the offset', () => {
    const date = parseLocalDate('2026-10-18');

    expect(date.getFullYear()).toBe(2026);
    expect(date.getMonth()).toBe(9);
    expect(date.getDate()).toBe(18);
    expect(date.getHours()).toBe(0);
  });
});
