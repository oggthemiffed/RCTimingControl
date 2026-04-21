import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '@/lib/adminApi';
import { adminQueryKeys } from './adminQueryKeys';

export function useEntriesForClass(eventId: number, classId: number) {
  return useQuery({
    queryKey: adminQueryKeys.events.entriesForClass(eventId, classId),
    queryFn: () => adminApi.listEntriesForClass(eventId, classId),
    enabled: Number.isFinite(eventId) && eventId > 0 && Number.isFinite(classId) && classId > 0,
  });
}

export function useWithdrawEntry(eventId: number, classId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (args: { entryId: number; reason: string }) =>
      adminApi.withdrawEntry(args.entryId, args.reason),
    onSuccess: () => {
      qc.invalidateQueries({
        queryKey: adminQueryKeys.events.entriesForClass(eventId, classId),
      });
      qc.invalidateQueries({ queryKey: adminQueryKeys.events.detail(eventId) });
    },
  });
}
