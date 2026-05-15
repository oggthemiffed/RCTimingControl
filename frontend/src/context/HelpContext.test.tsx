import { describe, it, expect, vi } from 'vitest';
import { render, screen, renderHook } from '@testing-library/react';
import React from 'react';
import { HelpProvider, useHelp } from '@/context/HelpContext';

describe('HelpProvider', () => {
  it('renders children without crashing', () => {
    render(
      <HelpProvider>
        <span>child</span>
      </HelpProvider>
    );
    expect(screen.getByText('child')).toBeTruthy();
  });

  it('provides helpContent as null initially', () => {
    const { result } = renderHook(() => useHelp(), {
      wrapper: ({ children }) => <HelpProvider>{children}</HelpProvider>,
    });
    expect(result.current.helpContent).toBeNull();
  });

  it('provides isOpen as false initially', () => {
    const { result } = renderHook(() => useHelp(), {
      wrapper: ({ children }) => <HelpProvider>{children}</HelpProvider>,
    });
    expect(result.current.isOpen).toBe(false);
  });
});

describe('useHelp', () => {
  it('throws when used outside HelpProvider', () => {
    // Suppress React error boundary output in test console
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    expect(() => renderHook(() => useHelp())).toThrow('useHelp must be used within HelpProvider');
    consoleSpy.mockRestore();
  });
});
