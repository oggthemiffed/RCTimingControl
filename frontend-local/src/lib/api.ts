import axios from 'axios';
import { getStoredSession, clearSession } from './auth';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
});

api.interceptors.request.use(async (config) => {
  const session = await getStoredSession();
  if (session) {
    config.headers.Authorization = `Bearer ${session.sessionToken}`;
  }
  return config;
});

// :localday's session model has no refresh-token concept. A 401 means the stored session is
// no longer valid (expired or unknown to the server) — clear it reactively so the UI falls
// back to the login screen. There is nothing to retry.
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      await clearSession();
    }
    return Promise.reject(error);
  },
);

export default api;

export interface OfficialSummary {
  credentialId: number;
  officialName: string;
  recovery: boolean;
}

export interface LoginResponse {
  sessionToken: string;
  officialName: string;
  credentialId: number;
}

export interface RecoverResponse {
  unlocked: boolean;
}

export async function listOfficials(): Promise<OfficialSummary[]> {
  const { data } = await api.get<OfficialSummary[]>('/api/v1/local-auth/officials');
  return data;
}

export async function login(credentialId: number, secret: string): Promise<LoginResponse> {
  const { data } = await api.post<LoginResponse>('/api/v1/local-auth/login', {
    credentialId,
    secret,
  });
  return data;
}

export async function recover(
  recoveryCredentialId: number,
  recoverySecret: string,
  targetCredentialId: number,
): Promise<RecoverResponse> {
  const { data } = await api.post<RecoverResponse>('/api/v1/local-auth/recover', {
    recoveryCredentialId,
    recoverySecret,
    targetCredentialId,
  });
  return data;
}

export interface CheckinEntry {
  cachedEntryId: number;
  racerName: string;
  carName: string;
  className: string;
  transponderNumber: string;
  checkedIn: boolean;
  checkedInAt: string | null;
}

export interface CheckinConfirmResponse {
  cachedEntryId: number;
  racerName: string;
  checkedIn: boolean;
  checkedInAt: string;
  alreadyCheckedIn: boolean;
}

export interface ReassignTransponderResponse {
  cachedEntryId: number;
  oldTransponderNumber: string;
  newTransponderNumber: string;
}

export async function checkinResolve(transponderNumber: string): Promise<CheckinEntry> {
  const { data } = await api.post<CheckinEntry>('/api/v1/checkin/resolve', {
    transponderNumber,
  });
  return data;
}

export async function checkinSearch(query: string): Promise<CheckinEntry[]> {
  const { data } = await api.get<CheckinEntry[]>('/api/v1/checkin/search', {
    params: { query },
  });
  return data;
}

export async function checkinConfirm(cachedEntryId: number): Promise<CheckinConfirmResponse> {
  const { data } = await api.post<CheckinConfirmResponse>(
    `/api/v1/checkin/${cachedEntryId}/confirm`,
  );
  return data;
}

export async function reassignTransponder(
  cachedEntryId: number,
  newTransponderNumber: string,
): Promise<ReassignTransponderResponse> {
  const { data } = await api.post<ReassignTransponderResponse>('/api/v1/transponders/reassign', {
    cachedEntryId,
    newTransponderNumber,
  });
  return data;
}

export type RaceStatus = 'PENDING' | 'GRID' | 'RUNNING' | 'STOPPED' | 'FINISHED';

export interface ScheduleEntryDto {
  id: number;
  cloudRaceId: number;
  roundNumber: number;
  heatNumber: number;
  sequence: number;
  className: string;
  finalLetter: string | null;
  scheduledStartAt: string | null;
  status: RaceStatus;
}

export interface GridEntryDto {
  cachedEntryId: number | null;
  racerName: string | null;
  transponderNumber: string | null;
  carNumber: string | null;
  gridPosition: number;
  bumped: boolean;
}

export interface ScheduleEntryDetailDto {
  race: ScheduleEntryDto;
  grid: GridEntryDto[];
}

export interface LiveTimingRowDto {
  entryId: number;
  driverName: string;
  position: number;
  lapsCompleted: number;
  lastPassingTimeMs: number | null;
  lastLapMs: number | null;
  bestLapMs: number | null;
  avgLapMs: number | null;
  overallFastestLapMs: number | null;
  lapsDown: number | null;
  intervalLapsDown: number | null;
  gapToLeaderMs: number | null;
  gapToAheadMs: number | null;
}

export interface LiveSnapshotDto {
  scheduleId: number;
  rows: LiveTimingRowDto[];
}

export interface MarshalAdjustmentResponseDto {
  raceId: number;
  entryId: number;
  transponderNumber: string;
  lapDelta: number;
  actingUserName: string;
}

export async function listRaces(): Promise<ScheduleEntryDto[]> {
  const { data } = await api.get<ScheduleEntryDto[]>('/api/v1/race-control/races');
  return data;
}

export async function getRaceDetail(id: number): Promise<ScheduleEntryDetailDto> {
  const { data } = await api.get<ScheduleEntryDetailDto>(`/api/v1/race-control/races/${id}`);
  return data;
}

export async function getLiveSnapshot(id: number): Promise<LiveSnapshotDto> {
  const { data } = await api.get<LiveSnapshotDto>(`/api/v1/race-control/races/${id}/live`);
  return data;
}

export async function transitionRace(id: number, target: RaceStatus): Promise<ScheduleEntryDto> {
  const { data } = await api.post<ScheduleEntryDto>(
    `/api/v1/race-control/races/${id}/transition`,
    { target },
  );
  return data;
}

export async function recordMarshalAdjustment(
  id: number,
  cachedEntryId: number,
  lapDelta: 1 | -1,
): Promise<MarshalAdjustmentResponseDto> {
  const { data } = await api.post<MarshalAdjustmentResponseDto>(
    `/api/v1/race-control/races/${id}/marshal-adjustment`,
    { cachedEntryId, lapDelta },
  );
  return data;
}

export async function advanceRound(
  id: number,
  nextScheduleId: number,
  entryIdsInFinishingOrder: number[],
): Promise<ScheduleEntryDetailDto> {
  const { data } = await api.post<ScheduleEntryDetailDto>(
    `/api/v1/race-control/races/${id}/advance-round`,
    { nextScheduleId, entryIdsInFinishingOrder },
  );
  return data;
}

// Anonymous spectator-board endpoints (U9). No auth header is sent or required — these are the
// only two endpoints board code may call; everything under /api/v1/race-control/** requires an
// authenticated official session and must not be used here.
export interface NowNextDto {
  currentRace: ScheduleEntryDto | null;
  nextRace: ScheduleEntryDto | null;
  lastCompletedRace: ScheduleEntryDto | null;
}

export interface RaceResultRowDto {
  entryId: number;
  racerName: string | null;
  transponderNumber: string | null;
  position: number;
  lapsCompleted: number;
  bestLapMs: number | null;
}

export interface ResultsDto {
  race: ScheduleEntryDto | null;
  results: RaceResultRowDto[];
}

export async function getNowNext(): Promise<NowNextDto> {
  const { data } = await api.get<NowNextDto>('/api/v1/boards/now-next');
  return data;
}

export async function getResults(): Promise<ResultsDto> {
  const { data } = await api.get<ResultsDto>('/api/v1/boards/results');
  return data;
}

// Day-lifecycle endpoints (U10). Pre-cache/open/status are reachable with no local session —
// there is nothing to log into until the day is OPEN. Close requires an authenticated session
// and is called from within OfficialShell, where the request interceptor already attaches the
// bearer token.
export type DayLifecycleStatusValue = 'NOT_SET_UP' | 'PRE_CACHED' | 'OPEN' | 'CLOSED';

export interface DayLifecycleStatus {
  status: DayLifecycleStatusValue;
  eventId: number | null;
  generation: number | null;
  splitBrainWarning: boolean;
  pendingSyncCount: number;
  lastPreCachedAt: string | null;
}

export interface CloseDayResponse {
  status: 'closed' | 'pending';
  pendingSyncCount: number;
}

export async function getDayLifecycleStatus(): Promise<DayLifecycleStatus> {
  const { data } = await api.get<DayLifecycleStatus>('/api/v1/day-lifecycle/status');
  return data;
}

export async function preCacheDay(
  eventId: number,
  email: string,
  password: string,
): Promise<DayLifecycleStatus> {
  const { data } = await api.post<DayLifecycleStatus>('/api/v1/day-lifecycle/pre-cache', {
    eventId,
    email,
    password,
  });
  return data;
}

export async function openDay(
  eventId: number,
  email?: string,
  password?: string,
): Promise<DayLifecycleStatus> {
  const { data } = await api.post<DayLifecycleStatus>('/api/v1/day-lifecycle/open', {
    eventId,
    email,
    password,
  });
  return data;
}

export async function closeDay(): Promise<CloseDayResponse> {
  const { data } = await api.post<CloseDayResponse>('/api/v1/day-lifecycle/close');
  return data;
}
