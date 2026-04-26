import api from './api';

// ── Types ──────────────────────────────────────────────────────────────────

export type GridCallSlotDto = {
  gridPosition: number;
  entryId: number;
  driverName: string;
  carNumber: string | null;
  className: string;
};

export type MarshalDutyRowDto = {
  entryId: number;
  driverName: string;
  carNumber: string | null;
  missedThisEvent: number;
};

export type PreRaceReadinessDto = {
  raceId: number;
  raceLabel: string;
  firstRaceOfEvent: boolean;
  gridCall: GridCallSlotDto[];
  marshalDuty: MarshalDutyRowDto[];
};

export type RunOrderItemDto = {
  raceId: number;
  sequenceInEvent: number;
  roundNumber: number;
  roundType: 'PRACTICE' | 'QUALIFIER' | 'FINAL';
  className: string;
  heatNumber: number;
  finalLetter: string | null;
  status: 'PENDING' | 'GRID' | 'RUNNING' | 'STOPPED' | 'FINISHED';
  sequenceInRound: number;
};

export type ResultRow = {
  position: number;
  entryId: number;
  driverName: string;
  carNumber: string | null;
  lapsCompleted: number;
  totalTimeMs: number;
  bestLapMs: number | null;
  gapToLeaderMs: number | null;
};

export type PositionAtLap = {
  lapNumber: number;
  entryId: number;
  position: number;
};

export type ClubBrandingDto = {
  clubName: string;
  logoUrl: string | null;
};

export type ResultSnapshotDto = {
  raceId: number;
  raceLabel: string;
  finishedAt: string;
  positions: ResultRow[];
  lapHistory: PositionAtLap[];
  clubBranding: ClubBrandingDto;
};

export type LiveTimingRowDto = {
  entryId: number;
  position: number;
  lapsCompleted: number;
  lastPassingTimeMs: number;
  bestLapMs: number | null;
  gapToLeaderMs: number | null;
  gapToAheadMs: number | null;
};

export type RaceStateChangeDto = {
  raceId: number;
  newStatus: string;
};

export type MarshalAdjustmentRequest = {
  entryId: number;
  transponderNumber: string;
  lapDelta: number;
};

export type PenaltyRequest = {
  entryId: number;
  penaltyType: 'LAP' | 'TIME';
  value: number;
  reason: string;
};

export type IncidentReportRequest = {
  entryId: number;
  incidentType: string;
  description: string;
};

export type MarshalAbsenceRequest = {
  entryId: number;
  eventId: number;
  notes?: string;
};

// ── API client ─────────────────────────────────────────────────────────────

export async function getPreRaceReadiness(raceId: number): Promise<PreRaceReadinessDto> {
  const { data } = await api.get<PreRaceReadinessDto>(
    `/api/v1/race-control/race/${raceId}/pre-race-readiness`,
  );
  return data;
}

export async function getRunOrder(eventId: number): Promise<RunOrderItemDto[]> {
  const { data } = await api.get<RunOrderItemDto[]>(
    `/api/v1/race-control/event/${eventId}/run-order`,
  );
  return data;
}

export async function getResultSnapshot(raceId: number): Promise<ResultSnapshotDto> {
  const { data } = await api.get<ResultSnapshotDto>(
    `/api/v1/race-control/race/${raceId}/result-snapshot`,
  );
  return data;
}

export async function callGrid(raceId: number): Promise<void> {
  await api.post(`/api/v1/race-control/race/${raceId}/call-grid`);
}

export async function startRace(raceId: number): Promise<void> {
  await api.post(`/api/v1/race-control/race/${raceId}/start`);
}

export async function stopRace(raceId: number): Promise<void> {
  await api.post(`/api/v1/race-control/race/${raceId}/stop`);
}

export async function abandonRace(raceId: number): Promise<void> {
  await api.post(`/api/v1/race-control/race/${raceId}/abandon`);
}

export async function restartRace(raceId: number): Promise<void> {
  await api.post(`/api/v1/race-control/race/${raceId}/restart`);
}

export async function marshalAdjustment(
  raceId: number,
  req: MarshalAdjustmentRequest,
): Promise<void> {
  await api.post(`/api/v1/race-control/race/${raceId}/marshal-adjustment`, req);
}

export async function raiseIncident(raceId: number, req: IncidentReportRequest): Promise<void> {
  await api.post(`/api/v1/race-control/referee/race/${raceId}/incident-report`, req);
}

export async function applyPenalty(raceId: number, req: PenaltyRequest): Promise<void> {
  await api.post(`/api/v1/race-control/referee/race/${raceId}/penalty`, req);
}

export async function recordMarshalAbsent(
  raceId: number,
  req: MarshalAbsenceRequest,
): Promise<void> {
  await api.post(`/api/v1/race-control/referee/race/${raceId}/marshal-absent`, req);
}

// ── Phase 5: Unknown Transponder Linking ─────────────────────────────────────

export type RaceEntryDto = {
  entryId: number;
  racerName: string;
  carNumber: string | null;
};

export type LinkTransponderRequest = {
  transponderNumber: string;
  entryId: number;
};

export type LinkTransponderResponse = {
  lapsCredited: number;
};

export async function getRaceEntries(raceId: number): Promise<RaceEntryDto[]> {
  const { data } = await api.get<RaceEntryDto[]>(`/api/v1/race-control/races/${raceId}/entries`);
  return data;
}

export async function linkUnknownTransponder(
  raceId: number,
  transponderNumber: string,
  entryId: number,
): Promise<LinkTransponderResponse> {
  const { data } = await api.post<LinkTransponderResponse>(
    `/api/v1/race-control/races/${raceId}/transponders/link`,
    { transponderNumber, entryId },
  );
  return data;
}

// ── Phase 5: Admin Forwarder Token Management ────────────────────────────────

export type ForwarderTokenStatus = 'ACTIVE' | 'REVOKED' | 'NONE';

export type ForwarderTokenStatusResponse = {
  status: ForwarderTokenStatus;
  generatedAt: string | null;
};

export type GenerateTokenResponse = {
  token: string;
};

export async function getForwarderTokenStatus(): Promise<ForwarderTokenStatusResponse> {
  const { data } = await api.get<ForwarderTokenStatusResponse>('/api/v1/admin/forwarder/token');
  return data;
}

export async function generateForwarderToken(): Promise<GenerateTokenResponse> {
  const { data } = await api.post<GenerateTokenResponse>('/api/v1/admin/forwarder/token', {});
  return data;
}

export async function revokeForwarderToken(): Promise<void> {
  await api.delete('/api/v1/admin/forwarder/token');
}
