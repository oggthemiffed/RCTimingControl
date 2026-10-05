import api from './api';

// ── Types ──────────────────────────────────────────────────────────────────

export type EventStatus =
  | 'DRAFT'
  | 'PUBLISHED'
  | 'OPEN'
  | 'ENTRIES_CLOSED'
  | 'IN_PROGRESS'
  | 'COMPLETED';

export interface AdminEventListDto {
  id: number;
  name: string;
  eventDate: string; // ISO date e.g. "2026-06-15"
  status: EventStatus;
  trackName: string | null;
}

export type StartType = 'STAGGER' | 'GRID' | 'ROLLING';
export type QualifyingType = 'FTQ' | 'ROUND_BY_ROUND' | 'FASTEST_LAP' | 'CONSECUTIVE_LAPS';

export interface TimedRaceConfig {
  type: 'TIMED';
  durationMinutes: number;
  startType: StartType;
  qualifyingType: QualifyingType;
  racePaddingMinutes: number;
  staggerIntervalSeconds: number;
}

export interface BumpUpConfig {
  type: 'BUMP_UP';
  qualifyingHeats: number;
  heatDurationMinutes: number;
  bestHeatsCount: number;
  gridSize: number;
  bumpSpots: number;
  qualifyingStartType: StartType;
  finalsStartType: StartType;
  qualifyingType: QualifyingType;
  racePaddingMinutes: number;
  staggerIntervalSeconds: number;
}

export interface PointsFinalsConfig {
  type: 'POINTS_FINALS';
  qualifyingHeats: number;
  finalsCount: number;
  finalDurationMinutes: number;
  heatDurationMinutes: number;
  qualifyingStartType: StartType;
  finalsStartType: StartType;
  qualifyingType: QualifyingType;
  racePaddingMinutes: number;
  staggerIntervalSeconds: number;
}

export type RaceFormatConfig = TimedRaceConfig | BumpUpConfig | PointsFinalsConfig;

export interface EventClassDto {
  id: number;
  eventId: number | null;
  racingClassId: number | null;
  templateId: number | null;
  configSnapshot: RaceFormatConfig;
  configOverride: Record<string, unknown> | null;
  combinedRaceGroup: number | null;
}

export interface EventDetailDto {
  id: number;
  name: string;
  eventDate: string;
  status: EventStatus;
  trackId: number | null;
  classes: EventClassDto[];
  racehubLastImportAt: string | null;
  racehubLastRevision: number | null;
}

// RaceHub Entry Export v1 import (L7/L8)

export type RaceHubImportAction = 'CREATE' | 'UPDATE' | 'WITHDRAW' | 'UNCHANGED' | 'STALE' | 'SKIP';

export interface RaceHubImportResult {
  dryRun: boolean;
  blocked: boolean;
  applied: boolean;
  racehubEventName: string | null;
  revision: number | null;
  summary: {
    created: number;
    updated: number;
    withdrawn: number;
    unchanged: number;
    stale: number;
    skipped: number;
  };
  unmappedClasses: {
    racehubEventClassId: string;
    rcClassName: string | null;
    className: string | null;
    entryCount: number;
  }[];
  errors: string[];
  warnings: string[];
  rows: {
    entryId: string;
    entryVersion: number;
    driverDisplayName: string | null;
    action: RaceHubImportAction;
    eventClassId: number | null;
    rctcEntryId: number | null;
  }[];
}

export interface RaceHubClassMappingDto {
  racehubEventClassId: string;
  eventClassId: number;
}

export interface AdminEntryDto {
  id: number;
  userId: number | null;
  competitorId: number | null;
  displayName: string | null;
  transponderNumber: string | null;
  secondaryTransponderNumber: string | null;
  status: 'PENDING' | 'CONFIRMED' | 'WITHDRAWN';
  submittedAt: string;
  withdrawnAt: string | null;
}

export interface CreateEventRequest {
  name: string;
  eventDate: string;
  trackId: number | null;
}

export interface UpdateEventRequest {
  name: string;
  eventDate: string;
  trackId: number | null;
}

export interface TransitionEventRequest {
  targetStatus: EventStatus;
}

export interface ClassFinalsConfigDto {
  eventClassId: number;
  finalsCount: number | null;
  carsPerFinal: number | null;
  bumpCount: number | null;
}

export interface GenerateRoundsRequest {
  practiceRoundsCount: number;
  qualifyingRoundsCount: number;
  maxCarsPerHeat: number;
  classFinalsConfigs: ClassFinalsConfigDto[];
}

export interface QualifyingResultDto {
  entryId: number;
  bestLapMs: number;
  lapsCompleted: number;
}

export interface SeedFinalsRequest {
  eventClassId: number;
  finalsCount: number;
  carsPerFinal: number;
  bumpCount: number;
  qualifyingResults: QualifyingResultDto[];
}

export interface AddEventClassRequest {
  racingClassId: number;
  templateId: number;
}

export interface UpdateEventClassOverrideRequest {
  override: Record<string, unknown>;
}

export interface CombineClassesRequest {
  eventClassIds: number[];
}

export interface WithdrawEntryRequest {
  reason: string;
}

export interface RacingClassDto {
  id: number;
  name: string;
  description: string | null;
}

export interface RaceFormatTemplateDto {
  id: number;
  name: string;
  config: RaceFormatConfig;
}

export interface TrackSummaryDto {
  id: number;
  name: string;
}

export interface TrackDto {
  id: number;
  name: string;
  venueNotes: string | null;
  trackLength: number | null;
}

export type ScoringSource = 'QUALIFYING' | 'FINALS' | 'BOTH';

export interface ChampionshipDto {
  id: number;
  name: string;
  bestXFromYX: number | null;
  bestXFromYY: number | null;
  scoringSource: ScoringSource;
  tqBonusPoints: number;
  afinalWinnerBonusPoints: number;
}

export interface ChampionshipClassDto {
  id: number;
  championshipId: number;
  racingClassId: number;
  bestXFromYX: number | null;
  bestXFromYY: number | null;
}

export interface ChampionshipEventLinkDto {
  id: number;
  championshipId: number;
  eventId: number;
  roundNumber: number;
}

export interface PointsScaleEntryDto {
  position: number;
  points: number;
}

export interface ChampionshipExclusionDto {
  id: number;
  championshipId: number;
  /** Competitor id. */
  driverId: number;
  eventId: number;
  reason: string;
  createdBy: number;
  createdAt: string;
}

export interface ChampionshipDetailDto extends ChampionshipDto {
  classes: ChampionshipClassDto[];
  events: ChampionshipEventLinkDto[];
  pointsScale: PointsScaleEntryDto[];
}

export interface RoundResultDto {
  roundNumber: number;
  eventId: number;
  eventName: string;
  position: number;
  points: number;
  excluded: boolean;
  dropped: boolean;
}

export interface StandingsRowDto {
  /** Competitor id; exclusions use the same id. */
  driverId: number;
  displayName: string;
  racingClassId: number;
  totalPoints: number;
  rounds: RoundResultDto[];
}

export interface CreateWalkInEntryRequest {
  eventId: number;
  eventClassId: number;
  /** An existing competitor, or leave out and give competitorName for a new one. */
  competitorId?: number;
  competitorName?: string;
  primaryTransponder: string;
  secondaryTransponder?: string;
}

export interface CreateWalkInEntryResult {
  entry: { id: number; status: string; transponderNumberSnapshot: string };
  warnings: string[];
}

export interface CompetitorSummaryDto {
  id: number;
  displayName: string;
  brcaNumber: string | null;
  homeClub: string | null;
}

export interface ClubProfileDto {
  id: number;
  name: string;
  email: string | null;
  phone: string | null;
  websiteUrl: string | null;
  latitude: number | null;
  longitude: number | null;
  timezone: string;
  logoType: string | null;
  logoUrl: string | null;
}

export interface UpdateClubProfileRequest {
  name: string;
  email: string | null;
  phone: string | null;
  websiteUrl: string | null;
  latitude: number | null;
  longitude: number | null;
  timezone: string;
  logoType: string | null;
}

// ── API client ─────────────────────────────────────────────────────────────

export interface BackupFileDto {
  name: string;
  sizeBytes: number;
  createdAt: string;
  /** manual, nightly or day-close */
  reason: string;
}

export interface BackupsDto {
  /** The folder backups are written to, as the server sees it */
  directory: string;
  backups: BackupFileDto[];
}

// Results sent to RaceHub (#27)
export type ResultsExportStatus = 'QUEUED' | 'FAILED' | 'SENT' | 'SUPERSEDED';
export type ResultsExportReason = 'RACE_FINISHED' | 'CORRECTION' | 'DAY_CLOSE';

export interface ResultsExportRowDto {
  id: number;
  eventId: number;
  eventName: string;
  revision: number;
  reason: ResultsExportReason;
  status: ResultsExportStatus;
  attempts: number;
  nextAttemptAt: string;
  lastError: string | null;
  createdAt: string;
  sentAt: string | null;
}

export interface ResultsExportsDto {
  /** Whether the RaceHub address and key are both set; without them, exports wait in the queue */
  sendingEnabled: boolean;
  resultsUrl: string | null;
  /** The settings still needed before anything is sent */
  missingSettings: string[];
  /** Newest first */
  exports: ResultsExportRowDto[];
}

export const adminApi = {
  // RaceHub import. A blocked import answers 422 with the same preview body, so return it.
  importRaceHubEntries: (eventId: number, exportDocument: unknown, dryRun: boolean) =>
    api
      .post<RaceHubImportResult>(
        `/api/v1/admin/events/${eventId}/racehub-import`,
        exportDocument,
        { params: { dryRun }, validateStatus: s => (s >= 200 && s < 300) || s === 422 },
      )
      .then(r => r.data),

  listRaceHubClassMappings: (eventId: number) =>
    api
      .get<RaceHubClassMappingDto[]>(`/api/v1/admin/events/${eventId}/racehub-class-mappings`)
      .then(r => r.data),

  replaceRaceHubClassMappings: (eventId: number, mappings: RaceHubClassMappingDto[]) =>
    api
      .put<RaceHubClassMappingDto[]>(`/api/v1/admin/events/${eventId}/racehub-class-mappings`, mappings)
      .then(r => r.data),

  // Events
  listEvents: () =>
    api.get<AdminEventListDto[]>('/api/v1/admin/events').then(r => r.data),

  getEvent: (id: number) =>
    api.get<EventDetailDto>(`/api/v1/admin/events/${id}`).then(r => r.data),

  createEvent: (body: CreateEventRequest) =>
    api.post<EventDetailDto>('/api/v1/admin/events', body).then(r => r.data),

  updateEvent: (id: number, body: UpdateEventRequest) =>
    api.put<EventDetailDto>(`/api/v1/admin/events/${id}`, body).then(r => r.data),

  transitionEvent: (id: number, targetStatus: EventStatus) =>
    api
      .post<EventDetailDto>(`/api/v1/admin/events/${id}/transition`, { targetStatus })
      .then(r => r.data),

  generateRounds: (id: number, body: GenerateRoundsRequest) =>
    api
      .post<void>(`/api/v1/admin/events/${id}/generate-rounds`, body)
      .then(r => r.data),

  seedFinals: (id: number, body: SeedFinalsRequest) =>
    api
      .post<void>(`/api/v1/admin/events/${id}/seed-finals`, body)
      .then(r => r.data),

  // Event classes
  listEventClasses: (eventId: number) =>
    api
      .get<EventClassDto[]>(`/api/v1/admin/events/${eventId}/classes`)
      .then(r => r.data),

  addEventClass: (eventId: number, body: AddEventClassRequest) =>
    api
      .post<EventClassDto>(`/api/v1/admin/events/${eventId}/classes`, body)
      .then(r => r.data),

  updateOverrides: (eventId: number, classId: number, override: Record<string, unknown>) =>
    api
      .put<EventClassDto>(
        `/api/v1/admin/events/${eventId}/classes/${classId}/overrides`,
        { override }
      )
      .then(r => r.data),

  combineClasses: (eventId: number, eventClassIds: number[]) =>
    api
      .post<EventClassDto[]>(`/api/v1/admin/events/${eventId}/classes/combine`, {
        eventClassIds,
      })
      .then(r => r.data),

  // Tracks (summary — for selects/dropdowns)
  listTracks: () =>
    api.get<TrackSummaryDto[]>('/api/v1/admin/tracks').then(r => r.data),

  // Racing classes
  listRacingClasses: () =>
    api.get<RacingClassDto[]>('/api/v1/admin/classes').then(r => r.data),

  // Format templates (list only — full CRUD via adminApi.formats below)
  listFormatTemplates: () =>
    api.get<RaceFormatTemplateDto[]>('/api/v1/admin/formats').then(r => r.data),

  // Entries
  listEntriesForClass: (eventId: number, classId: number) =>
    api
      .get<AdminEntryDto[]>(
        `/api/v1/admin/entries/events/${eventId}/classes/${classId}`
      )
      .then(r => r.data),

  createWalkInEntry: (body: CreateWalkInEntryRequest) =>
    api.post<CreateWalkInEntryResult>('/api/v1/admin/entries', body).then(r => r.data),

  withdrawEntry: (entryId: number, reason: string) =>
    api
      .post(`/api/v1/admin/entries/${entryId}/withdraw`, { reason })
      .then(r => r.data),

  // Championships
  championships: {
    list: () =>
      api.get<ChampionshipDto[]>('/api/v1/admin/championships').then(r => r.data),
    get: (id: number) =>
      api.get<ChampionshipDetailDto>(`/api/v1/admin/championships/${id}`).then(r => r.data),
    create: (body: Omit<ChampionshipDto, 'id'>) =>
      api.post<ChampionshipDto>('/api/v1/admin/championships', body).then(r => r.data),
    update: (id: number, body: Omit<ChampionshipDto, 'id'>) =>
      api.put<ChampionshipDto>(`/api/v1/admin/championships/${id}`, body).then(r => r.data),
    addClass: (id: number, body: { racingClassId: number; bestXFromYX: number | null; bestXFromYY: number | null }) =>
      api.post<ChampionshipClassDto>(`/api/v1/admin/championships/${id}/classes`, body).then(r => r.data),
    removeClass: (id: number, racingClassId: number) =>
      api.delete(`/api/v1/admin/championships/${id}/classes/${racingClassId}`),
    linkEvent: (id: number, body: { eventId: number; roundNumber: number }) =>
      api.post<ChampionshipEventLinkDto>(`/api/v1/admin/championships/${id}/events`, body).then(r => r.data),
    unlinkEvent: (id: number, eventId: number) =>
      api.delete(`/api/v1/admin/championships/${id}/events/${eventId}`),
    replacePointsScale: (id: number, entries: PointsScaleEntryDto[]) =>
      api.put<PointsScaleEntryDto[]>(`/api/v1/admin/championships/${id}/points-scale`, { entries }).then(r => r.data),
    listExclusions: (id: number) =>
      api.get<ChampionshipExclusionDto[]>(`/api/v1/admin/championships/${id}/exclusions`).then(r => r.data),
    createExclusion: (id: number, body: { driverId: number; eventId: number; reason: string }) =>
      api.post<ChampionshipExclusionDto>(`/api/v1/admin/championships/${id}/exclusions`, body).then(r => r.data),
    deleteExclusion: (id: number, exclusionId: number) =>
      api.delete(`/api/v1/admin/championships/${id}/exclusions/${exclusionId}`),
    getStandings: (id: number) =>
      api.get<StandingsRowDto[]>(`/api/v1/admin/championships/${id}/standings`).then(r => r.data),
  },

  // Club profile
  club: {
    getProfile: () =>
      api.get<ClubProfileDto>('/api/v1/admin/club/profile').then(r => r.data),
    updateProfile: (body: UpdateClubProfileRequest) =>
      api.put<ClubProfileDto>('/api/v1/admin/club/profile', body).then(r => r.data),
    uploadLogo: (file: File) => {
      const fd = new FormData();
      fd.append('file', file);
      return api.put<{ logoUrl: string }>('/api/v1/admin/club/logo', fd, {
        headers: { 'Content-Type': 'multipart/form-data' },
      }).then(r => r.data);
    },
  },

  // Tracks (full CRUD)
  tracks: {
    list: () =>
      api.get<TrackDto[]>('/api/v1/admin/tracks').then(r => r.data),
    get: (id: number) =>
      api.get<TrackDto>(`/api/v1/admin/tracks/${id}`).then(r => r.data),
    create: (body: Omit<TrackDto, 'id' | 'decoderLoops' | 'lapThresholds'>) =>
      api.post<TrackDto>('/api/v1/admin/tracks', body).then(r => r.data),
    update: (id: number, body: Omit<TrackDto, 'id' | 'decoderLoops' | 'lapThresholds'>) =>
      api.put<TrackDto>(`/api/v1/admin/tracks/${id}`, body).then(r => r.data),
    delete: (id: number) =>
      api.delete(`/api/v1/admin/tracks/${id}`),
  },

  // Format templates (full CRUD)
  formats: {
    list: () =>
      api.get<RaceFormatTemplateDto[]>('/api/v1/admin/formats').then(r => r.data),
    get: (id: number) =>
      api.get<RaceFormatTemplateDto>(`/api/v1/admin/formats/${id}`).then(r => r.data),
    create: (body: { name: string; config: RaceFormatConfig }) =>
      api.post<RaceFormatTemplateDto>('/api/v1/admin/formats', body).then(r => r.data),
    update: (id: number, body: { name: string; config: RaceFormatConfig }) =>
      api.put<RaceFormatTemplateDto>(`/api/v1/admin/formats/${id}`, body).then(r => r.data),
    delete: (id: number) =>
      api.delete(`/api/v1/admin/formats/${id}`),
  },

  // Competitors (drivers, with or without a login — for driver search in exclusions)
  competitors: {
    list: () =>
      api.get<CompetitorSummaryDto[]>('/api/v1/admin/competitors').then(r => r.data),
  },

  // Database backups (#22)
  backups: {
    list: () =>
      api.get<BackupsDto>('/api/v1/admin/backups').then(r => r.data),
    create: () =>
      api.post<BackupFileDto>('/api/v1/admin/backups').then(r => r.data),
  },

  // Results sent to RaceHub (#27)
  resultsExports: {
    list: () =>
      api.get<ResultsExportsDto>('/api/v1/admin/results-exports').then(r => r.data),
    retry: (id: number) =>
      api.post(`/api/v1/admin/results-exports/${id}/retry`).then(() => undefined),
    /** The event's results as they stand now, as a Results Export v1 file */
    download: (eventId: number) =>
      api
        .get<Blob>(`/api/v1/admin/events/${eventId}/results-export`, { responseType: 'blob' })
        .then(r => ({ blob: r.data, filename: filenameFrom(r.headers['content-disposition']) ?? `results-event-${eventId}.json` })),
  },
};

function filenameFrom(contentDisposition: unknown): string | null {
  if (typeof contentDisposition !== 'string') return null;
  const match = /filename="?([^";]+)"?/.exec(contentDisposition);
  return match ? match[1] : null;
}
