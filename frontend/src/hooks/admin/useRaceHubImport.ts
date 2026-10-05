import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '@/lib/adminApi';
import type { RaceHubClassMappingDto } from '@/lib/adminApi';
import { adminQueryKeys } from './adminQueryKeys';

export function useRaceHubClassMappings(eventId: number, enabled = true) {
  return useQuery({
    queryKey: adminQueryKeys.events.racehubClassMappings(eventId),
    queryFn: () => adminApi.listRaceHubClassMappings(eventId),
    enabled: enabled && Number.isFinite(eventId) && eventId > 0,
  });
}

export function useReplaceRaceHubClassMappings(eventId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (mappings: RaceHubClassMappingDto[]) =>
      adminApi.replaceRaceHubClassMappings(eventId, mappings),
    onSuccess: data => {
      qc.setQueryData(adminQueryKeys.events.racehubClassMappings(eventId), data);
    },
  });
}

/** Runs a dry-run preview or a real import. A real import refreshes the event and its entries. */
export function useRaceHubImport(eventId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (args: { exportDocument: unknown; dryRun: boolean }) =>
      adminApi.importRaceHubEntries(eventId, args.exportDocument, args.dryRun),
    onSuccess: result => {
      if (result.applied) {
        qc.invalidateQueries({ queryKey: adminQueryKeys.events.detail(eventId) });
      }
    },
  });
}

/**
 * Runs a dry-run preview of an RC-Timing CSV, or the real import with the picked updates and
 * withdrawals. A real import refreshes the event and its entries.
 */
export function useCsvImport(eventId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (args: { file: File; dryRun: boolean; update?: string[]; withdraw?: number[] }) =>
      adminApi.importCsvEntries(eventId, args.file, args.dryRun, args.update, args.withdraw),
    onSuccess: result => {
      if (result.applied) {
        qc.invalidateQueries({ queryKey: adminQueryKeys.events.detail(eventId) });
      }
    },
  });
}
