import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '@/lib/adminApi';
import type { TrackDto } from '@/lib/adminApi';
import { adminQueryKeys } from './adminQueryKeys';

export function useTracksList() {
  return useQuery({
    queryKey: adminQueryKeys.tracks.all(),
    queryFn: adminApi.tracks.list,
  });
}

export function useCreateTrack() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: Omit<TrackDto, 'id'>) =>
      adminApi.tracks.create(body as Omit<TrackDto, 'id' | 'decoderLoops' | 'lapThresholds'>),
    onSuccess: () => qc.invalidateQueries({ queryKey: adminQueryKeys.tracks.all() }),
  });
}

export function useUpdateTrack() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: Omit<TrackDto, 'id'> }) =>
      adminApi.tracks.update(id, body as Omit<TrackDto, 'id' | 'decoderLoops' | 'lapThresholds'>),
    onSuccess: () => qc.invalidateQueries({ queryKey: adminQueryKeys.tracks.all() }),
  });
}

export function useDeleteTrack() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => adminApi.tracks.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: adminQueryKeys.tracks.all() }),
  });
}
