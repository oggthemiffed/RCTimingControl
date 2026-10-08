// Dates and times as the club reads them. Everything is British format whatever the browser's language, so a
// laptop set to US English still shows 18 Oct 2026, and every screen and printout agrees.
const LOCALE = 'en-GB';

/**
 * A calendar date such as an event's `2026-10-18`, as that day in the viewer's own time zone.
 * `new Date('2026-10-18')` is midnight UTC, which is still the 17th west of Greenwich.
 */
export function parseLocalDate(isoDate: string): Date {
  const [year, month, day] = isoDate.split('-').map(Number);
  return new Date(year, month - 1, day);
}

/** An event's calendar date: "18 Oct 2026", or "18 October 2026" when long. */
export function formatEventDate(isoDate: string, style: 'medium' | 'long' = 'medium'): string {
  return new Intl.DateTimeFormat(LOCALE, { dateStyle: style }).format(parseLocalDate(isoDate));
}

/** The day of a timestamp: "18 Oct 2026", or "18 October 2026" when long. */
export function formatDate(iso: string, style: 'medium' | 'long' = 'medium'): string {
  return new Intl.DateTimeFormat(LOCALE, { dateStyle: style }).format(new Date(iso));
}

/** A timestamp: "18 Oct 2026, 14:05". */
export function formatDateTime(iso: string): string {
  return new Intl.DateTimeFormat(LOCALE, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(iso));
}

/** The time of a timestamp: "14:05", or "14:05:09" with seconds. */
export function formatTime(iso: string, withSeconds = false): string {
  return new Intl.DateTimeFormat(LOCALE, { timeStyle: withSeconds ? 'medium' : 'short' }).format(new Date(iso));
}
