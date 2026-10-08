import type { EventStatus } from '@/lib/adminApi';

/** Badge colours for an event's status, on the event list and the event page. */
export const eventStatusColor: Record<EventStatus, string> = {
  DRAFT: 'bg-muted text-muted-foreground',
  PUBLISHED: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300',
  OPEN: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300',
  ENTRIES_CLOSED: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300',
  IN_PROGRESS: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300',
  COMPLETED: 'bg-neutral-800 text-neutral-100 dark:bg-neutral-200 dark:text-neutral-900',
};

export const eventStatusLabel: Record<EventStatus, string> = {
  DRAFT: 'Draft',
  PUBLISHED: 'Published',
  OPEN: 'Open',
  ENTRIES_CLOSED: 'Entries Closed',
  IN_PROGRESS: 'In Progress',
  COMPLETED: 'Completed',
};
