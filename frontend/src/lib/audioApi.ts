import api from './api';

// ── Types ──────────────────────────────────────────────────────────────────

export interface VoiceInfo {
  voiceId: string;
  label: string;
  isDefault: boolean;
}

export interface AudioSettingsDto {
  announceCountdown: boolean;
  announceStagger: boolean;
  announceLapBeep: boolean;
  announceFinish: boolean;
  announceRunningOrder: boolean;
  runningOrderDepth: number;
  defaultVoiceId: string;
  countdownIntervals?: number[];
}

// ── Public audio endpoints ─────────────────────────────────────────────────

/** List available TTS voices. */
export const listVoices = () =>
  api.get<VoiceInfo[]>('/api/v1/audio/voices').then(r => r.data);

/** Fetch the clip URL map for a race (used by race control for pre-generated clips). */
export const getRaceClipMap = (raceId: number) =>
  api.get<Record<string, string>>(`/api/v1/race/${raceId}/audio-clips`).then(r => r.data);

// ── Race-control audio settings ────────────────────────────────────────────

/** GET current audio settings for race control (announcement toggles, volume etc.). */
export const getAudioSettings = () =>
  api.get<AudioSettingsDto>('/api/v1/race-control/settings/audio').then(r => r.data);

/** PATCH audio settings for race control. */
export const patchAudioSettings = (settings: AudioSettingsDto) =>
  api.patch<AudioSettingsDto>('/api/v1/race-control/settings/audio', settings).then(r => r.data);

// ── Admin audio endpoints ──────────────────────────────────────────────────

/** GET admin-level audio settings (same DTO, different auth). */
export const getAdminAudioSettings = () =>
  api.get<AudioSettingsDto>('/api/v1/admin/audio/settings').then(r => r.data);

/** PUT admin-level audio settings. */
export const saveAdminAudioSettings = (settings: AudioSettingsDto) =>
  api.put<AudioSettingsDto>('/api/v1/admin/audio/settings', settings).then(r => r.data);
