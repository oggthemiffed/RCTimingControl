import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '@/lib/adminApi';
import type { EventStatus, UpdateEventRequest, CreateEventRequest } from '@/lib/adminApi';
import { adminQueryKeys } from './adminQueryKeys';

export function useAdminEventsList() {
  return useQuery({
    queryKey: adminQueryKeys.events.all(),
    queryFn: adminApi.listEvents,
  });
}

export function useAdminEventDetail(id: number) {
  return useQuery({
    queryKey: adminQueryKeys.events.detail(id),
    queryFn: () => adminApi.getEvent(id),
    enabled: Number.isFinite(id) && id > 0,
  });
}

export function useCreateAdminEvent() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreateEventRequest) => adminApi.createEvent(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: adminQueryKeys.events.all() }),
  });
}

export function useUpdateAdminEvent(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: UpdateEventRequest) => adminApi.updateEvent(id, body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: adminQueryKeys.events.detail(id) });
      qc.invalidateQueries({ queryKey: adminQueryKeys.events.all() });
    },
  });
}

export function useTransitionEvent(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (targetStatus: EventStatus) => adminApi.transitionEvent(id, targetStatus),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: adminQueryKeys.events.detail(id) });
      qc.invalidateQueries({ queryKey: adminQueryKeys.events.all() });
    },
  });
}
