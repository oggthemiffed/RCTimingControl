import { describe, it, expect, beforeEach } from 'vitest';
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
});
