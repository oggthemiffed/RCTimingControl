import api from './api';

export interface RacerProfileDto {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber: string | null;
  emergencyContactName: string | null;
  emergencyContactPhone: string | null;
  phoneticName: string | null;
  preferredVoiceId: string | null;
  memberships: MembershipDto[];
  classRatings: ClassRatingDto[];
}

export interface MembershipDto {
  governingBodyCode: string;
  membershipNumber: string;
}

export interface ClassRatingDto {
  racingClassId: number;
  rating: number;
}

export interface UpdateRacerProfileRequest {
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  phoneticName?: string;
}

export interface UpsertMembershipRequest {
  governingBodyCode: string;
  membershipNumber: string;
}

export interface CarDto {
  id: number;
  name: string;
  primaryClassId: number | null;
  notes: string | null;
  archived: boolean;
  tags: { categoryId: number; categoryName: string; value: string }[];
}

export interface CreateCarRequest {
  name: string;
  primaryClassId?: number | null;
  notes?: string;
  tags?: { categoryId: number; value: string }[];
}

export type UpdateCarRequest = Partial<CreateCarRequest>;

export const fetchProfile = () =>
  api.get<RacerProfileDto>('/api/v1/racer/profile').then(r => r.data);

export const patchProfile = (req: UpdateRacerProfileRequest) =>
  api.patch<RacerProfileDto>('/api/v1/racer/profile', req).then(r => r.data);

export const fetchMemberships = () =>
  api.get<MembershipDto[]>('/api/v1/racer/memberships').then(r => r.data);

export const addMembership = (req: UpsertMembershipRequest) =>
  api.post<MembershipDto>('/api/v1/racer/memberships', req).then(r => r.data);

export const removeMembership = (code: string) =>
  api.delete(`/api/v1/racer/memberships/${encodeURIComponent(code)}`);

export const fetchCars = () =>
  api.get<CarDto[]>('/api/v1/racer/cars').then(r => r.data);

export const createCar = (req: CreateCarRequest) =>
  api.post<CarDto>('/api/v1/racer/cars', req).then(r => r.data);

export const updateCar = (id: number, req: UpdateCarRequest) =>
  api.patch<CarDto>(`/api/v1/racer/cars/${id}`, req).then(r => r.data);

export const archiveCar = (id: number) =>
  api.delete(`/api/v1/racer/cars/${id}`);

export interface GoverningBodyAffiliationDto {
  id: number;
  code: string;
  displayName: string;
  membershipRequired: boolean;
}

export const fetchAffiliations = () =>
  api.get<GoverningBodyAffiliationDto[]>('/api/v1/racer/affiliations').then(r => r.data);

export interface RaceResult {
  raceId: number;
  raceLabel: string;
  position: number;
  lapsCompleted: number;
  bestLapMs: number | null;
}

export interface RacerResultHistoryDto {
  eventId: number;
  eventName: string;
  eventDate: string;
  races: RaceResult[];
}

export async function getMyResults(): Promise<RacerResultHistoryDto[]> {
  const { data } = await api.get<RacerResultHistoryDto[]>('/api/v1/racer/results');
  return data;
}
