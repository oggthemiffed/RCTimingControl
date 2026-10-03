import { describe, it, expect, beforeEach } from 'vitest';
import { getStoredSession, storeSession, clearSession, type StoredSession } from './auth';

const sample: StoredSession = {
  sessionToken: 'abc-123',
  officialName: 'Jane Doe',
  credentialId: 1,
};

beforeEach(async () => {
  await clearSession();
});

describe('auth.ts (IndexedDB session storage)', () => {
  it('round-trips a stored session', async () => {
    await storeSession(sample);
    const result = await getStoredSession();
    expect(result).toEqual(sample);
  });

  it('returns null after clearing a stored session', async () => {
    await storeSession(sample);
    await clearSession();
    const result = await getStoredSession();
    expect(result).toBeNull();
  });

  it('returns null on a fresh/empty database without throwing', async () => {
    await expect(getStoredSession()).resolves.toBeNull();
  });

  it('overwrites a previous session on subsequent stores', async () => {
    await storeSession(sample);
    const second: StoredSession = { sessionToken: 'xyz-789', officialName: 'John Smith', credentialId: 2 };
    await storeSession(second);
    const result = await getStoredSession();
    expect(result).toEqual(second);
  });
});
