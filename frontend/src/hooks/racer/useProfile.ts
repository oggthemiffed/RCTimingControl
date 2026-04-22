import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  fetchProfile, patchProfile, addMembership, removeMembership, fetchAffiliations,
  type RacerProfileDto, type UpdateRacerProfileRequest, type UpsertMembershipRequest,
  type GoverningBodyAffiliationDto,
} from '@/lib/racerApi';
import { racerQueryKeys } from './racerQueryKeys';

export function useProfile() {
  return useQuery<RacerProfileDto>({
    queryKey: racerQueryKeys.profile,
    queryFn: fetchProfile,
  });
}

export function useUpdateProfile() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (req: UpdateRacerProfileRequest) => patchProfile(req),
    onSettled: () => queryClient.invalidateQueries({ queryKey: racerQueryKeys.profile }),
  });
}

export function useAddMembership() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (req: UpsertMembershipRequest) => addMembership(req),
    onSettled: () => queryClient.invalidateQueries({ queryKey: racerQueryKeys.profile }),
  });
}

export function useRemoveMembership() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (code: string) => removeMembership(code),
    onSettled: () => queryClient.invalidateQueries({ queryKey: racerQueryKeys.profile }),
  });
}

export function useAffiliations() {
  return useQuery<GoverningBodyAffiliationDto[]>({
    queryKey: racerQueryKeys.affiliations,
    queryFn: fetchAffiliations,
    staleTime: 5 * 60 * 1000,
  });
}
