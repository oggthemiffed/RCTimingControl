import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminApi, type AddOfficialRequest, type OfficialRole } from '@/lib/adminApi';
import { adminQueryKeys } from './adminQueryKeys';

export function useOfficials() {
  return useQuery({
    queryKey: adminQueryKeys.officials.all(),
    queryFn: adminApi.officials.list,
  });
}

/** How many changes the server sends a page at a time (its default). */
const CHANGES_PAGE_SIZE = 50;

/** The changes made to officials, newest first, a page at a time. */
export function useOfficialChanges() {
  return useInfiniteQuery({
    queryKey: adminQueryKeys.officials.changes(),
    queryFn: ({ pageParam }) => adminApi.officials.changes(pageParam),
    initialPageParam: undefined as number | undefined,
    // A short page is the last one; otherwise the next starts after the oldest change shown
    getNextPageParam: lastPage =>
      lastPage.length < CHANGES_PAGE_SIZE ? undefined : lastPage[lastPage.length - 1].id,
    select: data => data.pages.flat(),
  });
}

/** Every change refreshes the list and the recent changes (the changes key sits under the list's). */
function useOfficialMutation<T, R>(mutationFn: (args: T) => Promise<R>) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: adminQueryKeys.officials.all() }),
  });
}

export function useAddOfficial() {
  return useOfficialMutation((body: AddOfficialRequest) => adminApi.officials.add(body));
}

export function useChangeOfficialRoles() {
  return useOfficialMutation(({ id, roles }: { id: number; roles: OfficialRole[] }) =>
    adminApi.officials.changeRoles(id, roles));
}

export function useSetOfficialPassword() {
  return useOfficialMutation(({ id, password }: { id: number; password: string }) =>
    adminApi.officials.setPassword(id, password));
}

export function useSetOfficialEnabled() {
  return useOfficialMutation(({ id, enabled }: { id: number; enabled: boolean }) =>
    enabled ? adminApi.officials.enable(id) : adminApi.officials.disable(id));
}
