import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/**
 * A calendar date such as an event's `2026-10-18`, as that day in the viewer's own time zone.
 * `new Date('2026-10-18')` is midnight UTC, which is still the 17th west of Greenwich.
 */
export function parseLocalDate(isoDate: string): Date {
  const [year, month, day] = isoDate.split('-').map(Number);
  return new Date(year, month - 1, day);
}
