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
}

export interface AdminEntryDto {
  id: number;
  userId: number;
  firstName: string;
  lastName: string;
  transponderNumber: string | null;
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
  driverId: number;
  firstName: string;
  lastName: string;
  racingClassId: number;
  totalPoints: number;
  rounds: RoundResultDto[];
}

export interface UserSummaryDto {
  id: number;
  firstName: string;
  lastName: string;
  memberships: { code: string; number: string }[];
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

export interface CarTagCategoryDto {
  id: number;
  name: string;
  color: string | null;
  sortOrder: number;
  archived: boolean;
}

// ── API client ─────────────────────────────────────────────────────────────

export const adminApi = {
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

  // Users (for driver search in exclusions etc.)
  users: {
    list: () =>
      api.get<UserSummaryDto[]>('/api/v1/admin/users').then(r => r.data),
  },

  // Car tag categories
  carTagCategories: {
    list: (includeArchived: boolean) =>
      api.get<CarTagCategoryDto[]>(`/api/v1/admin/car-tag-categories?includeArchived=${includeArchived}`).then(r => r.data),
    create: (body: Omit<CarTagCategoryDto, 'id' | 'archived'>) =>
      api.post<CarTagCategoryDto>('/api/v1/admin/car-tag-categories', body).then(r => r.data),
    update: (id: number, body: Partial<Omit<CarTagCategoryDto, 'id' | 'archived'>>) =>
      api.put<CarTagCategoryDto>(`/api/v1/admin/car-tag-categories/${id}`, body).then(r => r.data),
    archive: (id: number) =>
      api.delete(`/api/v1/admin/car-tag-categories/${id}`),
    unarchive: (id: number) =>
      api.post(`/api/v1/admin/car-tag-categories/${id}/unarchive`),
  },
};
