import { describe, it, expect, beforeEach, vi } from 'vitest';
import { act, renderHook } from '@testing-library/react';

import { useAudioVolume } from './useAudioVolume';

describe('useAudioVolume', () => {
  beforeEach(() => localStorage.clear());

  it('starts at 80% when nothing is saved', () => {
    expect(renderHook(() => useAudioVolume()).result.current[0]).toBe(80);
  });

  it('reads the saved volume', () => {
    localStorage.setItem('rc-audio-volume', '35');

    expect(renderHook(() => useAudioVolume()).result.current[0]).toBe(35);
  });

  it('tells every user of the volume when it changes', () => {
    const slider = renderHook(() => useAudioVolume());
    const cockpit = renderHook(() => useAudioVolume());

    act(() => slider.result.current[1](40));

    expect(cockpit.result.current[0]).toBe(40);
    expect(localStorage.getItem('rc-audio-volume')).toBe('40');
  });

  it('keeps a stored volume within 0 to 100', () => {
    localStorage.setItem('rc-audio-volume', '150');

    expect(renderHook(() => useAudioVolume()).result.current[0]).toBe(100);
  });

  it('still changes when the browser refuses to store it', () => {
    const setItem = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('QuotaExceededError');
    });
    const { result } = renderHook(() => useAudioVolume());

    act(() => result.current[1](30));

    expect(result.current[0]).toBe(30);
    setItem.mockRestore();
    act(() => result.current[1](80));
  });
});
