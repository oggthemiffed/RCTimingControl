import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '@/lib/adminApi';
import type { UpdateClubProfileRequest } from '@/lib/adminApi';
import { adminQueryKeys } from './adminQueryKeys';

export function useClubProfile() {
  return useQuery({
    queryKey: adminQueryKeys.club.profile(),
    queryFn: adminApi.club.getProfile,
  });
}

export function useUpdateClubProfile() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: UpdateClubProfileRequest) => adminApi.club.updateProfile(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: adminQueryKeys.club.profile() }),
  });
}

export function useUploadClubLogo() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (file: File) => adminApi.club.uploadLogo(file),
    onSuccess: () => qc.invalidateQueries({ queryKey: adminQueryKeys.club.profile() }),
  });
}
