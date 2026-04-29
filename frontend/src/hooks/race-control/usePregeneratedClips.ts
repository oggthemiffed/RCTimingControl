import { useEffect, useRef } from 'react';
import { getRaceClipMap } from '@/lib/audioApi';

interface UsePregeneratedClipsOptions {
  raceId: number | null;
  raceState?: string | null;
  setClipMap: (map: Record<string, string>) => void;
}

/**
 * Fetches pre-generated TTS audio clips when a race enters GRID state (AUDIO-10).
 *
 * On GRID transition, calls getRaceClipMap and populates the clip cache via
 * setClipMap. Errors are non-fatal — Web Speech API fallback handles missing clips.
 */
export function usePregeneratedClips({
  raceId,
  raceState,
  setClipMap,
}: UsePregeneratedClipsOptions) {
  // Track which raceId we've already fetched to avoid duplicate requests
  const fetchedForRaceRef = useRef<number | null>(null);

  useEffect(() => {
    if (raceState !== 'GRID') return;
    if (!raceId) return;
    if (fetchedForRaceRef.current === raceId) return;

    fetchedForRaceRef.current = raceId;

    getRaceClipMap(raceId)
      .then(({ data }) => {
        setClipMap(data);
      })
      .catch((err) => {
        console.warn(
          'Failed to fetch pre-generated clips — falling back to Web Speech API:',
          err,
        );
      });
  }, [raceState, raceId, setClipMap]);
}
