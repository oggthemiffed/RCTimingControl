import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import RaceHubImportDialog from './RaceHubImportDialog';
import { adminApi } from '@/lib/adminApi';
import type { EventClassDto, RaceHubImportResult } from '@/lib/adminApi';

vi.mock('sonner', () => ({ toast: { success: vi.fn(), error: vi.fn() } }));

vi.mock('@/lib/adminApi', () => ({
  adminApi: {
    importRaceHubEntries: vi.fn(),
    listRaceHubClassMappings: vi.fn(),
    replaceRaceHubClassMappings: vi.fn(),
    listRacingClasses: vi.fn(),
  },
}));

// Radix Select renders in a portal that jsdom cannot drive; use a native <select> instead
vi.mock('@/components/ui/select', () => ({
  Select: ({ children, value, onValueChange }: {
    children: React.ReactNode; value?: string; onValueChange?: (v: string) => void;
  }) => (
    <select value={value ?? ''} onChange={e => onValueChange?.(e.target.value)}>
      {children}
    </select>
  ),
  SelectTrigger: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  SelectValue: ({ placeholder }: { placeholder?: string }) => <option value="">{placeholder}</option>,
  SelectContent: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  SelectItem: ({ children, value }: { children: React.ReactNode; value: string }) => (
    <option value={value}>{children}</option>
  ),
}));

const api = vi.mocked(adminApi);

const classes = [
  { id: 11, eventId: 5, racingClassId: 101 },
  { id: 12, eventId: 5, racingClassId: 102 },
] as EventClassDto[];

function result(overrides: Partial<RaceHubImportResult> = {}): RaceHubImportResult {
  return {
    dryRun: true,
    blocked: false,
    applied: false,
    racehubEventName: 'Club Round 3',
    revision: 4,
    summary: { created: 2, updated: 1, withdrawn: 1, unchanged: 3, stale: 0, skipped: 1 },
    unmappedClasses: [],
    errors: [],
    warnings: [],
    rows: [],
    ...overrides,
  };
}

const exportFile = () =>
  new File([JSON.stringify({ schema_version: 1, entries: [] })], 'entries.json', { type: 'application/json' });

function renderDialog(onOpenChange = vi.fn()) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <RaceHubImportDialog eventId={5} classes={classes} open onOpenChange={onOpenChange} />
    </QueryClientProvider>,
  );
  return onOpenChange;
}

function chooseFile(file: File) {
  fireEvent.change(screen.getByLabelText('Entry Export file'), { target: { files: [file] } });
}

describe('RaceHubImportDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    api.listRaceHubClassMappings.mockResolvedValue([]);
    api.listRacingClasses.mockResolvedValue([
      { id: 101, name: '2WD Buggy', description: null },
      { id: 102, name: 'Stadium Truck', description: null },
    ]);
  });

  it('shows the dry-run preview with counts and warnings', async () => {
    api.importRaceHubEntries.mockResolvedValue(
      result({ warnings: ['Transponder 7101 is used by more than one entry: Ada, Grace'] }),
    );
    renderDialog();

    chooseFile(exportFile());

    expect(await screen.findByTestId('racehub-preview')).toBeInTheDocument();
    expect(api.importRaceHubEntries).toHaveBeenCalledWith(5, { schema_version: 1, entries: [] }, true);
    expect(screen.getByText(/Club Round 3/)).toHaveTextContent('revision 4');
    expect(screen.getByTestId('summary-created')).toHaveTextContent('2');
    expect(screen.getByTestId('summary-updated')).toHaveTextContent('1');
    expect(screen.getByTestId('summary-withdrawn')).toHaveTextContent('1');
    expect(screen.getByTestId('summary-unchanged')).toHaveTextContent('3');
    expect(screen.getByText(/Transponder 7101 is used by more than one entry/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Import entries' })).toBeEnabled();
  });

  it('rejects a file that is not JSON without calling the server', async () => {
    renderDialog();

    chooseFile(new File(['not json'], 'entries.json'));

    expect(await screen.findByText('This file is not valid JSON.')).toBeInTheDocument();
    expect(api.importRaceHubEntries).not.toHaveBeenCalled();
  });

  it('blocks the import until unmapped classes are mapped, then saves the mapping and checks again', async () => {
    api.listRaceHubClassMappings.mockResolvedValue([{ racehubEventClassId: 'rh-old', eventClassId: 12 }]);
    api.importRaceHubEntries
      .mockResolvedValueOnce(result({
        blocked: true,
        unmappedClasses: [{ racehubEventClassId: 'rh-buggy', rcClassName: 'Buggy 2WD', className: '2WD', entryCount: 2 }],
      }))
      .mockResolvedValueOnce(result());
    api.replaceRaceHubClassMappings.mockImplementation(async (_eventId, mappings) => mappings);
    renderDialog();

    chooseFile(exportFile());

    expect(await screen.findByText('Buggy 2WD')).toBeInTheDocument();
    expect(screen.getByText(/2 entries/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Import entries' })).toBeDisabled();
    const save = screen.getByRole('button', { name: 'Save mappings and check again' });
    expect(save).toBeDisabled();

    // The dropdown lists the event's classes by racing class name
    const dropdown = screen.getByRole('combobox');
    await waitFor(() => expect(screen.getByRole('option', { name: '2WD Buggy' })).toBeInTheDocument());
    fireEvent.change(dropdown, { target: { value: '11' } });
    fireEvent.click(save);

    await waitFor(() =>
      expect(api.replaceRaceHubClassMappings).toHaveBeenCalledWith(5, [
        { racehubEventClassId: 'rh-old', eventClassId: 12 },
        { racehubEventClassId: 'rh-buggy', eventClassId: 11 },
      ]),
    );
    await waitFor(() => expect(api.importRaceHubEntries).toHaveBeenCalledTimes(2));
    expect(await screen.findByRole('button', { name: 'Import entries' })).toBeEnabled();
    expect(screen.queryByText('Buggy 2WD')).not.toBeInTheDocument();
  });

  it('confirms the import and closes', async () => {
    api.importRaceHubEntries
      .mockResolvedValueOnce(result())
      .mockResolvedValueOnce(result({ dryRun: false, applied: true }));
    const onOpenChange = renderDialog();

    chooseFile(exportFile());
    fireEvent.click(await screen.findByRole('button', { name: 'Import entries' }));

    await waitFor(() => expect(onOpenChange).toHaveBeenCalledWith(false));
    expect(api.importRaceHubEntries).toHaveBeenLastCalledWith(5, { schema_version: 1, entries: [] }, false);
  });

  it('lists problems that block the import', async () => {
    api.importRaceHubEntries.mockResolvedValue(result({ blocked: true, errors: ['entry x has no entry_version'] }));
    renderDialog();

    chooseFile(exportFile());

    expect(await screen.findByText('entry x has no entry_version')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Import entries' })).toBeDisabled();
  });
});
