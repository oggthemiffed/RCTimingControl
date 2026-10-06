import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import {
  useAnnouncements,
  gridClipKey,
  gridFallbackText,
  CLIP_GRACE_MS,
  type GridEntry,
} from './useAnnouncements';
import type { AudioSettingsDto } from '@/lib/audioApi';

vi.mock('@/hooks/race-control/useStomp', () => ({ useStomp: () => ({ data: null }) }));

const settings = {
  announceCountdown: true,
  announceStagger: true,
  announceLapBeep: true,
  announceFinish: true,
  announceRunningOrder: true,
} as AudioSettingsDto;

const grid: GridEntry[] = [
  { entryId: 11, carNumber: null, driverName: 'Alex Rowe' },
  { entryId: 12, carNumber: null, driverName: 'Sam Ito' },
];

const audioPlay = vi.fn(() => Promise.resolve());
const audioUrls: string[] = [];
const speak = vi.fn();

beforeEach(() => {
  vi.useFakeTimers();
  audioPlay.mockClear();
  speak.mockClear();
  audioUrls.length = 0;
  vi.stubGlobal(
    'Audio',
    class {
      volume = 1;
      constructor(url: string) { audioUrls.push(url); }
      play = audioPlay;
    },
  );
  vi.stubGlobal('SpeechSynthesisUtterance', class { volume = 1; text: string; constructor(text: string) { this.text = text; } });
  Object.defineProperty(window, 'speechSynthesis', { value: { speak }, configurable: true });
});
afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllGlobals();
});

describe('grid call helpers', () => {
  it('keys the clip by entry id', () => {
    expect(gridClipKey(grid[0])).toBe('grid-11');
  });
  it('falls back to the name, with the car number when there is one', () => {
    expect(gridFallbackText(grid[0])).toBe('Alex Rowe.');
    expect(gridFallbackText({ ...grid[0], carNumber: '7' })).toBe('Car 7, Alex Rowe.');
  });
});

describe('useAnnouncements grid call', () => {
  function setup(raceState: string, entries: GridEntry[] = grid) {
    return renderHook(
      (p: { raceState: string }) =>
        useAnnouncements({ raceId: 5, settings, raceState: p.raceState, gridEntries: entries }),
      { initialProps: { raceState } },
    );
  }

  it('plays each driver\'s clip in turn once the clips have arrived', () => {
    const { result } = setup('GRID');
    act(() => result.current.setClipMap({ 'grid-11': 'http://x/11.wav', 'grid-12': 'http://x/12.wav' }));

    act(() => { vi.advanceTimersByTime(0); });
    expect(audioUrls).toEqual(['http://x/11.wav']);
    act(() => { vi.advanceTimersByTime(2000); });
    expect(audioUrls).toEqual(['http://x/11.wav', 'http://x/12.wav']);
    expect(speak).not.toHaveBeenCalled();
  });

  it('waits for the clips instead of speaking at once', () => {
    setup('GRID');
    act(() => { vi.advanceTimersByTime(CLIP_GRACE_MS - 1); });
    expect(speak).not.toHaveBeenCalled();
    expect(audioPlay).not.toHaveBeenCalled();
  });

  it('uses the browser voice when the clips never arrive', () => {
    setup('GRID');
    act(() => { vi.advanceTimersByTime(CLIP_GRACE_MS); });
    act(() => { vi.advanceTimersByTime(2000); });
    expect(speak).toHaveBeenCalledTimes(2);
  });

  it('keeps the scheduled calls when the grid refetches', () => {
    const { result, rerender } = setup('GRID');
    act(() => result.current.setClipMap({ 'grid-11': 'http://x/11.wav', 'grid-12': 'http://x/12.wav' }));
    rerender({ raceState: 'GRID' });
    act(() => { vi.advanceTimersByTime(2000); });
    expect(audioUrls).toHaveLength(2);
  });
});

describe('useAnnouncements finish', () => {
  it('plays the race-finished clip', () => {
    const { result, rerender } = renderHook(
      (p: { raceState: string }) => useAnnouncements({ raceId: 5, settings, raceState: p.raceState }),
      { initialProps: { raceState: 'RUNNING' } },
    );
    act(() => result.current.setClipMap({ finish: 'http://x/finish.wav' }));
    rerender({ raceState: 'FINISHED' });
    expect(audioUrls).toEqual(['http://x/finish.wav']);
  });
});
