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
