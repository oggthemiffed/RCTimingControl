import api from './api';

// ── Types ──────────────────────────────────────────────────────────────────

export type SetupStatusDto = { bootstrapped: boolean; setupComplete: boolean };

export type SetupProgressDto = {
  club: boolean;
  track: boolean;
  format: boolean;
  staff: boolean;
  decoder: boolean;
};

export type BootstrapRequest = {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
};

export type AuthResponse = {
  accessToken: string;
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
};

export type DecoderConfigUpdateRequest = {
  decoderHost: string;
  decoderPort: number;
  decoderProtocol: 'RC4' | 'P3';
};

/** Stored decoder settings. Fields are null until the decoder has been configured. */
export type DecoderConfigDto = {
  decoderHost: string | null;
  decoderPort: number | null;
  decoderProtocol: 'RC4' | 'P3' | null;
};

export type SetupStaffRequest = {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  roles: string[];
};

// ── API functions ──────────────────────────────────────────────────────────

export async function getSetupStatus(): Promise<SetupStatusDto> {
  const { data } = await api.get<SetupStatusDto>('/api/v1/setup/status');
  return data;
}

export async function getSetupProgress(): Promise<SetupProgressDto> {
  const { data } = await api.get<SetupProgressDto>('/api/v1/setup/progress');
  return data;
}

export async function bootstrap(req: BootstrapRequest): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>('/api/v1/setup/bootstrap', req);
  return data;
}

export async function getDecoderConfig(): Promise<DecoderConfigDto> {
  const { data } = await api.get<DecoderConfigDto>('/api/v1/setup/decoder-config');
  return data;
}

export async function updateDecoderConfig(req: DecoderConfigUpdateRequest): Promise<SetupProgressDto> {
  const { data } = await api.patch<SetupProgressDto>('/api/v1/setup/decoder-config', req);
  return data;
}

export async function createSetupStaff(req: SetupStaffRequest): Promise<void> {
  await api.post('/api/v1/setup/staff', req);
}

