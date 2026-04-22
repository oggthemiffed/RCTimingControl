import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '@/lib/adminApi';
import type { CarTagCategoryDto } from '@/lib/adminApi';
import { adminQueryKeys } from './adminQueryKeys';

export function useCarTagCategories(includeArchived: boolean) {
  return useQuery({
    queryKey: adminQueryKeys.carTagCategories.all(includeArchived),
    queryFn: () => adminApi.carTagCategories.list(includeArchived),
  });
}

export function useCreateCarTagCategory() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: Omit<CarTagCategoryDto, 'id' | 'archived'>) =>
      adminApi.carTagCategories.create(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'car-tag-categories'] }),
  });
}

export function useUpdateCarTagCategory() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: Partial<Omit<CarTagCategoryDto, 'id' | 'archived'>> }) =>
      adminApi.carTagCategories.update(id, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'car-tag-categories'] }),
  });
}

export function useArchiveCarTagCategory() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => adminApi.carTagCategories.archive(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'car-tag-categories'] }),
  });
}

export function useUnarchiveCarTagCategory() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => adminApi.carTagCategories.unarchive(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'car-tag-categories'] }),
  });
}
