import { useCallback, useSyncExternalStore } from 'react';

const STORAGE_KEY = 'rc-audio-volume';
const DEFAULT_PERCENT = 80;

const listeners = new Set<() => void>();

function read(): number {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    const percent = stored == null ? NaN : parseInt(stored, 10);
    return Number.isFinite(percent) ? percent : DEFAULT_PERCENT;
  } catch {
    return DEFAULT_PERCENT;
  }
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => { listeners.delete(listener); };
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
    } catch {
      // Private windows can refuse storage; the volume then lasts only until the page reloads
    }
    listeners.forEach((listener) => listener());
  }, []);
  return [percent, setPercent];
}
