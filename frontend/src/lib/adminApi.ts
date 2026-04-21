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

export interface RaceFormatConfig {
  type: string;
  [key: string]: unknown;
}

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
};
