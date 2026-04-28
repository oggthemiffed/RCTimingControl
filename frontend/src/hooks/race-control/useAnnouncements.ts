import { useCallback, useEffect, useRef } from 'react';
import { useStomp } from '@/hooks/race-control/useStomp';
import type { AudioSettingsDto } from '@/lib/audioApi';

interface RunningOrderEvent {
  type: 'running-order';
  positions: string[];
}

interface UseAnnouncementsOptions {
  raceId: number | null;
  settings: AudioSettingsDto | null;
  volume?: number;
}

/**
 * Audio announcements hook (AUDIO-04, AUDIO-06, AUDIO-11).
 *
 * Subscribes to the race audio STOMP topic for running-order events and
 * provides utilities for Web Speech API fallback and beep sounds.
 */
export function useAnnouncements({
  raceId,
  settings,
  volume = 0.8,
}: UseAnnouncementsOptions) {
  // Ref so fallbackSpeak closure always uses latest volume without re-creating
  const volumeRef = useRef(volume);
  useEffect(() => {
    volumeRef.current = volume;
  }, [volume]);

  const settingsRef = useRef(settings);
  useEffect(() => {
    settingsRef.current = settings;
  }, [settings]);

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

  return {
    fallbackSpeak,
    playBeep,
    testAudio,
  };
}
