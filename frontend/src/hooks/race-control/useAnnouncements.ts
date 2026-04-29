import { useCallback, useEffect, useRef } from 'react';
import { useStomp } from '@/hooks/race-control/useStomp';
import type { AudioSettingsDto } from '@/lib/audioApi';

const DEFAULT_COUNTDOWN_INTERVALS = [600, 300, 120, 60, 30];

interface RunningOrderEvent {
  type: 'running-order';
  positions: string[];
}

export interface GridEntry {
  carNumber: string | null;
  driverName: string;
}

interface UseAnnouncementsOptions {
  raceId: number | null;
  settings: AudioSettingsDto | null;
  volume?: number;
  raceState?: string | null;
  raceStartedAt?: string | null;
  raceDurationSecs?: number | null;
  gridEntries?: GridEntry[];
}

/**
 * Audio announcements hook (AUDIO-02, AUDIO-03, AUDIO-04, AUDIO-05, AUDIO-06, AUDIO-11).
 *
 * Subscribes to the race audio STOMP topic for running-order events and
 * provides utilities for Web Speech API fallback, beep sounds, clip playback,
 * countdown timer, stagger sequencer, and finish announcement.
 */
export function useAnnouncements({
  raceId,
  settings,
  volume = 0.8,
  raceState,
  raceStartedAt,
  raceDurationSecs,
  gridEntries,
}: UseAnnouncementsOptions) {
  // Refs so closures inside timeouts always read latest values without stale captures
  const volumeRef = useRef(volume);
  useEffect(() => { volumeRef.current = volume; }, [volume]);

  const settingsRef = useRef(settings);
  useEffect(() => { settingsRef.current = settings; }, [settings]);

  const raceStateRef = useRef(raceState);
  useEffect(() => { raceStateRef.current = raceState; }, [raceState]);

  // Clip URL cache — populated externally via setClipMap (AUDIO-10)
  const clipMapRef = useRef<Record<string, string>>({});

  // Timeout collections for scheduled announcements
  const countdownTimeoutsRef = useRef<ReturnType<typeof setTimeout>[]>([]);
  const staggerTimeoutsRef = useRef<ReturnType<typeof setTimeout>[]>([]);

  // Subscribe to race audio events (AUDIO-06 running-order trigger)
  const { data: audioEvent } = useStomp<RunningOrderEvent>(
    raceId ? `/topic/race/${raceId}/audio` : null,
  );

  // Web Speech API fallback (AUDIO-11)
  const fallbackSpeak = useCallback((text: string) => {
    if (typeof window === 'undefined' || !window.speechSynthesis) return;
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.volume = volumeRef.current;
    window.speechSynthesis.speak(utterance);
  }, []);

  // playClip: play a pre-generated clip URL; fall back to Web Speech API (AUDIO-10, AUDIO-11)
  const playClip = useCallback((key: string, fallbackText: string) => {
    const url = clipMapRef.current[key];
    if (url) {
      const audio = new Audio(url);
      audio.volume = volumeRef.current;
      audio.play().catch(() => fallbackSpeak(fallbackText));
    } else {
      fallbackSpeak(fallbackText);
    }
  }, [fallbackSpeak]);

  // setClipMap: populate the clip cache from outside (called by usePregeneratedClips)
  const setClipMap = useCallback((map: Record<string, string>) => {
    clipMapRef.current = map;
  }, []);

  // Handle running-order announcements (AUDIO-06)
  useEffect(() => {
    if (!audioEvent) return;
    const s = settingsRef.current;
    if (!s?.announceRunningOrder) return;
    if (audioEvent.type === 'running-order' && audioEvent.positions?.length > 0) {
      const text = `Running order: ${audioEvent.positions
        .map((name, i) => `${i + 1}, ${name}`)
        .join('. ')}.`;
      fallbackSpeak(text);
    }
  }, [audioEvent, fallbackSpeak]);

  // Countdown timer: schedule announcements when race starts (AUDIO-02)
  useEffect(() => {
    countdownTimeoutsRef.current.forEach(clearTimeout);
    countdownTimeoutsRef.current = [];

    if (raceState !== 'RUNNING') return;
    const s = settingsRef.current;
    if (!s?.announceCountdown) return;
    if (!raceDurationSecs) return;

    const raceEndMs = raceStartedAt
      ? new Date(raceStartedAt).getTime() + raceDurationSecs * 1000
      : Date.now() + raceDurationSecs * 1000;

    const intervals = s.countdownIntervals ?? DEFAULT_COUNTDOWN_INTERVALS;
    const now = Date.now();

    intervals.forEach((secsBeforeEnd) => {
      const delay = raceEndMs - secsBeforeEnd * 1000 - now;
      if (delay <= 0) return;

      const mins = Math.floor(secsBeforeEnd / 60);
      const secs = secsBeforeEnd % 60;
      const label =
        mins > 0
          ? `${mins} minute${mins > 1 ? 's' : ''}${secs > 0 ? ` ${secs} seconds` : ''}`
          : `${secs} seconds`;
      const text = `${label} remaining.`;

      const t = setTimeout(() => {
        if (raceStateRef.current !== 'RUNNING') return;
        if (!settingsRef.current?.announceCountdown) return;
        playClip(`countdown-${secsBeforeEnd}`, text);
      }, delay);

      countdownTimeoutsRef.current.push(t);
    });

    return () => {
      countdownTimeoutsRef.current.forEach(clearTimeout);
      countdownTimeoutsRef.current = [];
    };
  }, [raceState, raceStartedAt, raceDurationSecs, playClip]);

  // Finish announcement: fire when race transitions to FINISHED (AUDIO-05)
  useEffect(() => {
    if (raceState !== 'FINISHED') return;
    const s = settingsRef.current;
    if (!s?.announceFinish) return;
    playClip('finish', 'Race finished. Checkered flag.');
  }, [raceState, playClip]);

  // Stagger sequencer: call car numbers in sequence at GRID state (AUDIO-03)
  useEffect(() => {
    staggerTimeoutsRef.current.forEach(clearTimeout);
    staggerTimeoutsRef.current = [];

    if (raceState !== 'GRID') return;
    const s = settingsRef.current;
    if (!s?.announceStagger) return;
    if (!gridEntries || gridEntries.length === 0) return;

    gridEntries.forEach((entry, index) => {
      const t = setTimeout(() => {
        if (raceStateRef.current !== 'GRID') return;
        if (!settingsRef.current?.announceStagger) return;
        const carLabel = entry.carNumber ? `Car ${entry.carNumber}` : entry.driverName;
        playClip(
          `car-${entry.carNumber ?? entry.driverName}`,
          `${carLabel}, ${entry.driverName}.`,
        );
      }, index * 2000);

      staggerTimeoutsRef.current.push(t);
    });

    return () => {
      staggerTimeoutsRef.current.forEach(clearTimeout);
      staggerTimeoutsRef.current = [];
    };
  }, [raceState, gridEntries, playClip]);

  // Per-lap improvement beeps (AUDIO-04) via AudioContext
  const playBeep = useCallback(
    (improving: boolean) => {
      if (settingsRef.current && !settingsRef.current.announceLapBeep) return;
      try {
        const ctx = new AudioContext();
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        gain.gain.value = volumeRef.current;
        osc.frequency.value = improving ? 880 : 440;
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start();
        osc.stop(ctx.currentTime + 0.2);
        // Allow GC of AudioContext after beep
        osc.onended = () => ctx.close();
      } catch (e) {
        console.warn('Beep failed:', e);
      }
    },
    [],
  );

  // Test audio: speak a sample sentence (AUDIO-11)
  const testAudio = useCallback(() => {
    fallbackSpeak(
      'Testing audio. Race control online. First place, Car 12. Second place, Car 7.',
    );
  }, [fallbackSpeak]);

  // Global cleanup on unmount
  useEffect(() => {
    return () => {
      countdownTimeoutsRef.current.forEach(clearTimeout);
      staggerTimeoutsRef.current.forEach(clearTimeout);
    };
  }, []);

  return {
    fallbackSpeak,
    playBeep,
    playClip,
    setClipMap,
    testAudio,
  };
}
