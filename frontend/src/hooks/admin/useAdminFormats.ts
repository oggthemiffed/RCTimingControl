import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '@/lib/adminApi';
import type { RaceFormatConfig } from '@/lib/adminApi';
import { adminQueryKeys } from './adminQueryKeys';

export function useFormatsList() {
  return useQuery({
    queryKey: adminQueryKeys.formats.all(),
    queryFn: adminApi.formats.list,
  });
}

export function useCreateFormat() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { name: string; config: RaceFormatConfig }) =>
      adminApi.formats.create(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: adminQueryKeys.formats.all() }),
  });
}

export function useUpdateFormat() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: { name: string; config: RaceFormatConfig } }) =>
      adminApi.formats.update(id, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: adminQueryKeys.formats.all() }),
  });
}

export function useDeleteFormat() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => adminApi.formats.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: adminQueryKeys.formats.all() }),
  });
}
