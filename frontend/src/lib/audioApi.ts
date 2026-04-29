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

export interface BlocklistTermDto {
  id: number;
  word: string;
  addedAt: string;
}

// ── Public audio endpoints ─────────────────────────────────────────────────

/** List available TTS voices. */
export const listVoices = () =>
  api.get<VoiceInfo[]>('/api/v1/audio/voices');

/** Preview TTS output for a given voice. Returns audio/wav bytes. */
export const previewNameClip = (voice?: string) => {
  const params = voice ? `?voice=${encodeURIComponent(voice)}` : '';
  return api.get<Blob>(`/api/v1/audio/preview${params}`, { responseType: 'blob' });
};

/** Fetch the clip URL map for a race (used by race control for pre-generated clips). */
export const getRaceClipMap = (raceId: number) =>
  api.get<Record<string, string>>(`/api/v1/race/${raceId}/audio-clips`);

// ── Race-control audio settings ────────────────────────────────────────────

/** GET current audio settings for race control (announcement toggles, volume etc.). */
export const getAudioSettings = () =>
  api.get<AudioSettingsDto>('/api/v1/race-control/settings/audio');

/** PATCH audio settings for race control. */
export const patchAudioSettings = (settings: AudioSettingsDto) =>
  api.patch<AudioSettingsDto>('/api/v1/race-control/settings/audio', settings);

// ── Admin audio endpoints ──────────────────────────────────────────────────

/** GET admin-level audio settings (same DTO, different auth). */
export const getAdminAudioSettings = () =>
  api.get<AudioSettingsDto>('/api/v1/admin/audio/settings');

/** PUT admin-level audio settings. */
export const saveAdminAudioSettings = (settings: AudioSettingsDto) =>
  api.put<AudioSettingsDto>('/api/v1/admin/audio/settings', settings);

/** GET profanity blocklist. */
export const getBlocklist = () =>
  api.get<BlocklistTermDto[]>('/api/v1/admin/audio/blocklist');

/** Add a word to the profanity blocklist. */
export const addBlocklistTerm = (word: string) =>
  api.post<BlocklistTermDto>('/api/v1/admin/audio/blocklist', { word });

/** Remove a word from the profanity blocklist. */
export const removeBlocklistTerm = (id: number) =>
  api.delete(`/api/v1/admin/audio/blocklist/${id}`);

// ── Racer voice preference ──────────────────────────────────────────────────

/** Save a racer's preferred TTS voice. */
export const saveVoicePreference = (voiceId: string | null) =>
  api.put('/api/v1/racer/audio/voice', { voiceId });
