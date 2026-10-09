import { useCallback, useSyncExternalStore } from 'react';

const STORAGE_KEY = 'rc-audio-volume';
const DEFAULT_PERCENT = 80;

const listeners = new Set<() => void>();
// What was last set, for when storage refuses it (a private window), so the slider still moves
let unsaved: number | null = null;

function read(): number {
  if (unsaved != null) return unsaved;
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    const percent = stored == null ? NaN : parseInt(stored, 10);
    // A hand-edited value outside 0 to 100 would make the browser refuse to play anything
    return Number.isFinite(percent) ? Math.min(100, Math.max(0, percent)) : DEFAULT_PERCENT;
  } catch {
    return DEFAULT_PERCENT;
  }
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  // Another tab moving the slider changes this one too
  window.addEventListener('storage', listener);
  return () => {
    listeners.delete(listener);
    window.removeEventListener('storage', listener);
  };
}

/**
 * The announcer volume on this browser, 0 to 100, kept in localStorage. The cockpit and its audio settings
 * panel share it, so moving the slider changes what the cockpit's announcements play at straight away.
 */
export function useAudioVolume(): [number, (percent: number) => void] {
  const percent = useSyncExternalStore(subscribe, read, () => DEFAULT_PERCENT);
  const setPercent = useCallback((next: number) => {
    try {
      localStorage.setItem(STORAGE_KEY, String(next));
      unsaved = null;
    } catch {
      // Private windows can refuse storage; the volume then lasts only until the page reloads
      unsaved = next;
    }
    listeners.forEach((listener) => listener());
  }, []);
  return [percent, setPercent];
}
