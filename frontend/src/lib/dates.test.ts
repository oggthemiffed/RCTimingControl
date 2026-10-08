import { describe, it, expect } from 'vitest';
import { formatDate, formatDateTime, formatEventDate, formatTime, parseLocalDate } from './dates';

describe('parseLocalDate', () => {
  it('gives the same calendar day in the local time zone, whatever the offset', () => {
    const date = parseLocalDate('2026-10-18');

    expect(date.getFullYear()).toBe(2026);
    expect(date.getMonth()).toBe(9);
    expect(date.getDate()).toBe(18);
    expect(date.getHours()).toBe(0);
  });
});

describe('formatting', () => {
  it('shows an event date in British format', () => {
    expect(formatEventDate('2026-10-18')).toBe('18 Oct 2026');
    expect(formatEventDate('2026-10-18', 'long')).toBe('18 October 2026');
  });

  it('shows timestamps in British format, in local time', () => {
    const iso = new Date(2026, 9, 18, 14, 5, 9).toISOString();

    expect(formatDate(iso)).toBe('18 Oct 2026');
    expect(formatDateTime(iso)).toBe('18 Oct 2026, 14:05');
    expect(formatTime(iso)).toBe('14:05');
    expect(formatTime(iso, true)).toBe('14:05:09');
  });
});
