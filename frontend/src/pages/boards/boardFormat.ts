/** How often the boards re-poll their public endpoints. */
export const BOARD_POLL_MS = 5000;

/** Lap/gap time for a TV board: m:ss.mmm over a minute, else s.mmm; '—' when missing. */
export function fmtMs(ms: number | null | undefined): string {
  if (ms === null || ms === undefined || ms <= 0) return '—';
  const m = Math.floor(ms / 60000);
  const rem = ms % 60000;
  const secs = Math.floor(rem / 1000);
  const millis = String(rem % 1000).padStart(3, '0');
  return m > 0 ? `${m}:${String(secs).padStart(2, '0')}.${millis}` : `${secs}.${millis}`;
}

/** Reads the optional `?event=ID` board parameter; anything that isn't a positive id is ignored. */
export function parseEventParam(value: string | null): number | null {
  if (!value) return null;
  const id = Number(value);
  return Number.isInteger(id) && id > 0 ? id : null;
}
