import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  callGrid,
  startRace,
  stopRace,
  abandonRace,
  restartRace,
  marshalAdjustment,
  raiseIncident,
  applyPenalty,
  recordMarshalAbsent,
  type MarshalAdjustmentRequest,
  type IncidentReportRequest,
  type PenaltyRequest,
  type MarshalAbsenceRequest,
} from '@/lib/raceControlApi';
import { raceControlQueryKeys } from './raceControlQueryKeys';

export function useRaceStateMutations(raceId: number, eventId: number) {
  const queryClient = useQueryClient();

  const invalidateRunOrder = () =>
    queryClient.invalidateQueries({ queryKey: raceControlQueryKeys.runOrder(eventId) });

  const invalidateSnapshot = () =>
    queryClient.invalidateQueries({ queryKey: raceControlQueryKeys.resultSnapshot(raceId) });

  return {
    callGrid: useMutation({
      mutationFn: () => callGrid(raceId),
      onSuccess: invalidateRunOrder,
    }),
    start: useMutation({
      mutationFn: () => startRace(raceId),
      onSuccess: invalidateRunOrder,
    }),
    stop: useMutation({
      mutationFn: () => stopRace(raceId),
      onSuccess: invalidateRunOrder,
    }),
    abandon: useMutation({
      mutationFn: () => abandonRace(raceId),
      onSuccess: () => { invalidateRunOrder(); invalidateSnapshot(); },
    }),
    restart: useMutation({
      mutationFn: () => restartRace(raceId),
      onSuccess: () => { invalidateRunOrder(); invalidateSnapshot(); },
    }),
    marshalAdj: useMutation({
      mutationFn: (req: MarshalAdjustmentRequest) => marshalAdjustment(raceId, req),
    }),
    incident: useMutation({
      mutationFn: (req: IncidentReportRequest) => raiseIncident(raceId, req),
    }),
    penalty: useMutation({
      mutationFn: (req: PenaltyRequest) => applyPenalty(raceId, req),
    }),
    marshalAbsent: useMutation({
      mutationFn: (req: MarshalAbsenceRequest) => recordMarshalAbsent(raceId, req),
    }),
  };
}
