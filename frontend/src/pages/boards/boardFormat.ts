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

const OVERLAY_DEFAULT_TOP = 10;
const OVERLAY_MAX_TOP = 40;

/** The streaming overlay's options from its query string; anything unrecognised falls back to the default. */
export function parseOverlayOptions(params: URLSearchParams) {
  const top = Number(params.get('top'));
  return {
    eventId: parseEventParam(params.get('event')),
    top: Number.isInteger(top) && top > 0 ? Math.min(top, OVERLAY_MAX_TOP) : OVERLAY_DEFAULT_TOP,
    showClass: params.get('class') !== 'hide',
    theme: params.get('theme') === 'light' ? ('light' as const) : ('dark' as const),
  };
}

/** Race time as m:ss, rounded down to the second. */
export function fmtClock(ms: number): string {
  const total = Math.max(0, Math.floor(ms / 1000));
  return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, '0')}`;
}
