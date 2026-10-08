import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { DecoderSettingsForm } from './DecoderSettingsForm';
import * as setupApi from '@/lib/setupApi';

vi.mock('@/lib/setupApi', () => ({
  getDecoderConfig: vi.fn(),
  updateDecoderConfig: vi.fn(),
  testDecoderConfig: vi.fn(),
}));

// Radix UI Select as a native <select>, so the options can be inspected in jsdom
vi.mock('@/components/ui/select', () => ({
  Select: ({ children, value }: { children: React.ReactNode; value?: string }) => (
    <select role="combobox" value={value ?? ''} onChange={() => undefined}>
      {children}
    </select>
  ),
  SelectTrigger: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  SelectValue: () => null,
  SelectContent: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  SelectItem: ({ children, value, disabled }: { children: React.ReactNode; value: string; disabled?: boolean }) => (
    <option value={value} disabled={disabled}>
      {children}
    </option>
  ),
}));

beforeEach(() => {
  vi.mocked(setupApi.getDecoderConfig).mockResolvedValue({
    decoderHost: null,
    decoderPort: null,
    decoderProtocol: null,
  });
});

describe('DecoderSettingsForm', () => {
  it('lists the P3 protocol as not supported yet, and does not let it be chosen', () => {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={client}>
        <DecoderSettingsForm />
      </QueryClientProvider>,
    );

    const p3 = screen.getByRole('option', { name: /P3 binary/ });
    expect(p3).toBeDisabled();
    expect(p3).toHaveTextContent('not supported yet');
    expect(screen.getByRole('option', { name: /RC4/ })).toBeEnabled();
  });
});
