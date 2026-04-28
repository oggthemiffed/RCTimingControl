import { describe, it, expect } from 'vitest';

describe.skip('AdminAudioSettingsPage', () => {
  it('renders default announcement toggles', () => {
    expect(true).toBe(false); // Plan 06
  });
  it('voice selector populated from /api/v1/audio/voices', () => {
    expect(true).toBe(false); // Plan 06
  });
  it('profanity blocklist displays custom terms', () => {
    expect(true).toBe(false); // Plan 06
  });
  it('add term fires POST /api/v1/admin/audio/blocklist', () => {
    expect(true).toBe(false); // Plan 06
  });
  it('remove term fires DELETE /api/v1/admin/audio/blocklist', () => {
    expect(true).toBe(false); // Plan 06
  });
});
