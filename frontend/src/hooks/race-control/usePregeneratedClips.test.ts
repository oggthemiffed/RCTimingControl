import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { usePregeneratedClips, CLIP_POLL_MS, CLIP_POLL_MAX_ATTEMPTS } from './usePregeneratedClips';

const getRaceClipMap = vi.fn();
vi.mock('@/lib/audioApi', () => ({ getRaceClipMap: (id: number) => getRaceClipMap(id) }));

const flush = () => act(async () => { await vi.advanceTimersByTimeAsync(0); });

describe('usePregeneratedClips', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    getRaceClipMap.mockReset();
  });
  afterEach(() => vi.useRealTimers());

  it('does not fetch before the race reaches GRID', async () => {
    renderHook(() => usePregeneratedClips({ raceId: 5, raceState: 'PENDING', setClipMap: vi.fn() }));
    await flush();
    expect(getRaceClipMap).not.toHaveBeenCalled();
  });

  it('polls until the server has made the clips, then stops', async () => {
    const setClipMap = vi.fn();
    getRaceClipMap
      .mockResolvedValueOnce({ data: {} })
      .mockResolvedValueOnce({ data: {} })
      .mockResolvedValue({ data: { 'grid-1': 'http://x/grid-1.wav' } });

    renderHook(() => usePregeneratedClips({ raceId: 5, raceState: 'GRID', setClipMap }));
    await flush();
    expect(setClipMap).not.toHaveBeenCalled();

    await act(async () => { await vi.advanceTimersByTimeAsync(CLIP_POLL_MS * 2); });
    expect(setClipMap).toHaveBeenCalledWith({ 'grid-1': 'http://x/grid-1.wav' });
    expect(getRaceClipMap).toHaveBeenCalledTimes(3);

    await act(async () => { await vi.advanceTimersByTimeAsync(CLIP_POLL_MS * 5); });
    expect(getRaceClipMap).toHaveBeenCalledTimes(3);
  });

  it('keeps one poll running when the race goes from GRID to RUNNING', async () => {
    getRaceClipMap.mockResolvedValue({ data: { finish: 'http://x/finish.wav' } });
    const setClipMap = vi.fn();
    const { rerender } = renderHook(
      ({ raceState }) => usePregeneratedClips({ raceId: 5, raceState, setClipMap }),
      { initialProps: { raceState: 'GRID' } },
    );
    await flush();
    rerender({ raceState: 'RUNNING' });
    await flush();
    expect(getRaceClipMap).toHaveBeenCalledTimes(1);
  });

  it('gives up after the maximum number of attempts', async () => {
    getRaceClipMap.mockResolvedValue({ data: {} });
    renderHook(() => usePregeneratedClips({ raceId: 5, raceState: 'GRID', setClipMap: vi.fn() }));
    await act(async () => { await vi.advanceTimersByTimeAsync(CLIP_POLL_MS * (CLIP_POLL_MAX_ATTEMPTS + 5)); });
    expect(getRaceClipMap).toHaveBeenCalledTimes(CLIP_POLL_MAX_ATTEMPTS);
  });

  it('stops polling when the race changes', async () => {
    getRaceClipMap.mockResolvedValue({ data: {} });
    const { rerender } = renderHook(
      ({ raceState }) => usePregeneratedClips({ raceId: 5, raceState, setClipMap: vi.fn() }),
      { initialProps: { raceState: 'GRID' } },
    );
    await flush();
    rerender({ raceState: 'FINISHED' });
    await act(async () => { await vi.advanceTimersByTimeAsync(CLIP_POLL_MS * 5); });
    expect(getRaceClipMap).toHaveBeenCalledTimes(1);
  });
});
