import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  fetchCars, createCar, updateCar, archiveCar,
  type CarDto, type CreateCarRequest, type UpdateCarRequest,
} from '@/lib/racerApi';
import { racerQueryKeys } from './racerQueryKeys';

export function useCars() {
  return useQuery<CarDto[]>({
    queryKey: racerQueryKeys.cars,
    queryFn: fetchCars,
  });
}

export function useCreateCar() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (req: CreateCarRequest) => createCar(req),
    onSettled: () => qc.invalidateQueries({ queryKey: racerQueryKeys.cars }),
  });
}

export function useUpdateCar() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, req }: { id: number; req: UpdateCarRequest }) => updateCar(id, req),
    onSettled: () => qc.invalidateQueries({ queryKey: racerQueryKeys.cars }),
  });
}

export function useArchiveCar() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => archiveCar(id),
    onSettled: () => qc.invalidateQueries({ queryKey: racerQueryKeys.cars }),
  });
}
