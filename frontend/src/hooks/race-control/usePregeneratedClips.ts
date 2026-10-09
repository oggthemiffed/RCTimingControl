import { useEffect } from 'react';
import { getRaceClipMap } from '@/lib/audioApi';

interface UsePregeneratedClipsOptions {
  raceId: number | null;
  raceState?: string | null;
  setClipMap: (map: Record<string, string>) => void;
}

/** The server makes the clips after GRID starts, so ask again until they arrive. */
export const CLIP_POLL_MS = 2000;
export const CLIP_POLL_MAX_ATTEMPTS = 30;

/**
 * Fetches pre-generated TTS audio clips for a race at GRID or RUNNING (AUDIO-10).
 *
 * The server generates clips asynchronously and publishes them all at once, so an
 * early request returns an empty map. This polls until a non-empty map arrives,
 * then stops. Errors and a map that never arrives are non-fatal: the Web Speech
 * API fallback speaks instead.
 */
export function usePregeneratedClips({
  raceId,
  raceState,
  setClipMap,
}: UsePregeneratedClipsOptions) {
  const active = raceState === 'GRID' || raceState === 'RUNNING';

  useEffect(() => {
    if (!active || !raceId) return;

    let cancelled = false;
    let timer: ReturnType<typeof setTimeout> | undefined;
    let attempts = 0;

    const poll = () => {
      attempts += 1;
      getRaceClipMap(raceId)
        .then((data) => {
          if (cancelled) return;
          if (Object.keys(data).length > 0) {
            setClipMap(data);
          } else if (attempts < CLIP_POLL_MAX_ATTEMPTS) {
            timer = setTimeout(poll, CLIP_POLL_MS);
          }
        })
        .catch((err) => {
          if (cancelled) return;
          console.warn(
            'Failed to fetch pre-generated clips — falling back to Web Speech API:',
            err,
          );
          if (attempts < CLIP_POLL_MAX_ATTEMPTS) timer = setTimeout(poll, CLIP_POLL_MS);
        });
    };
    poll();

    return () => {
      cancelled = true;
      if (timer) clearTimeout(timer);
    };
  }, [active, raceId, setClipMap]);
}
