import type { CsvImportGroup, CsvImportResult, CsvImportRow } from '@/lib/adminApi';

/** The order the preview shows its groups in. */
export const GROUP_ORDER: CsvImportGroup[] = ['NEW', 'CHANGED', 'MISSING', 'UNCHANGED', 'SKIPPED'];

export function groupRows(rows: CsvImportRow[]): Record<CsvImportGroup, CsvImportRow[]> {
  const groups: Record<CsvImportGroup, CsvImportRow[]> = {
    NEW: [],
    CHANGED: [],
    UNCHANGED: [],
    MISSING: [],
    SKIPPED: [],
  };
  rows.forEach(row => groups[row.group].push(row));
  return groups;
}

/**
 * What the official has picked: changed rows to update (by key) and missing entries to withdraw (by
 * entry id). `keepMissing` is the "Don't withdraw racers missing from this file" switch.
 */
export interface CsvImportSelection {
  update: Set<string>;
  withdraw: Set<number>;
  keepMissing: boolean;
}

/** Every changed row ticked, no missing entry ticked, and missing racers kept (#40). */
export function defaultSelection(preview: CsvImportResult): CsvImportSelection {
  return {
    update: new Set(preview.rows.filter(r => r.group === 'CHANGED' && r.key).map(r => r.key as string)),
    withdraw: new Set(),
    keepMissing: true,
  };
}

export function toggled<T>(set: Set<T>, value: T, on: boolean): Set<T> {
  const next = new Set(set);
  if (on) next.add(value);
  else next.delete(value);
  return next;
}

/** All of a group's rows picked, or none. */
export function selectAll(
  selection: CsvImportSelection,
  rows: CsvImportRow[],
  group: 'CHANGED' | 'MISSING',
  on: boolean,
): CsvImportSelection {
  if (group === 'CHANGED') {
    const keys = rows.filter(r => r.group === 'CHANGED' && r.key).map(r => r.key as string);
    return { ...selection, update: on ? new Set(keys) : new Set() };
  }
  const ids = rows.filter(r => r.group === 'MISSING' && r.entryId != null).map(r => r.entryId as number);
  return { ...selection, withdraw: on ? new Set(ids) : new Set() };
}

/** What confirming sends: missing entries are only withdrawn when the keep switch is off. */
export function confirmPayload(selection: CsvImportSelection): { update: string[]; withdraw: number[] } {
  return {
    update: [...selection.update],
    withdraw: selection.keepMissing ? [] : [...selection.withdraw],
  };
}

function plural(count: number, one: string, many: string) {
  return `${count} ${count === 1 ? one : many}`;
}

/** The line shown before confirming, such as "3 new, 2 updates, 1 withdrawal". */
export function summaryLine(preview: CsvImportResult, selection: CsvImportSelection): string {
  const payload = confirmPayload(selection);
  return [
    `${preview.summary.newEntries} new`,
    plural(payload.update.length, 'update', 'updates'),
    plural(payload.withdraw.length, 'withdrawal', 'withdrawals'),
  ].join(', ');
}
