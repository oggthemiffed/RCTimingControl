import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';

import {
  clearAccessToken, currentAccessToken, endSession, getAccessToken, refreshAccessToken, setAccessToken,
} from './auth';

vi.mock('axios', () => ({ default: { post: vi.fn() } }));
const post = vi.mocked(axios.post);

/** A token that expires `seconds` from now. */
function tokenExpiringIn(seconds: number): string {
  const payload = btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + seconds }));
  return `header.${payload}.signature`;
}

describe('access token renewal', () => {
  beforeEach(() => {
    post.mockReset();
    clearAccessToken();
  });

  it('gives an anonymous visitor no token and does not try to refresh', async () => {
    expect(await currentAccessToken()).toBeNull();
    expect(post).not.toHaveBeenCalled();
  });

  it('keeps a token that is not about to expire', async () => {
    const token = tokenExpiringIn(600);
    setAccessToken(token);

    expect(await currentAccessToken()).toBe(token);
    expect(post).not.toHaveBeenCalled();
  });

  it('renews an expired token and stores the new one', async () => {
    setAccessToken(tokenExpiringIn(-10));
    post.mockResolvedValue({ data: { accessToken: 'renewed' } });

    expect(await currentAccessToken()).toBe('renewed');
    expect(getAccessToken()).toBe('renewed');
  });

  it('renews a token that expires within 30 seconds', async () => {
    setAccessToken(tokenExpiringIn(10));
    post.mockResolvedValue({ data: { accessToken: 'renewed' } });

    expect(await currentAccessToken()).toBe('renewed');
  });

  it('falls back to no token when the session can no longer be refreshed', async () => {
    setAccessToken(tokenExpiringIn(-10));
    post.mockRejectedValue(new Error('401'));

    expect(await currentAccessToken()).toBeNull();
  });

  it('keeps a token it cannot read the expiry of', async () => {
    setAccessToken('not-a-jwt');

    expect(await currentAccessToken()).toBe('not-a-jwt');
    expect(post).not.toHaveBeenCalled();
  });

  it('shares one request between refreshes that overlap, because the refresh token rotates', async () => {
    let resolve!: (v: { data: { accessToken: string } }) => void;
    post.mockReturnValue(new Promise((r) => (resolve = r)));

    const first = refreshAccessToken();
    const second = refreshAccessToken();
    resolve({ data: { accessToken: 'one' } });

    expect(await first).toBe('one');
    expect(await second).toBe('one');
    expect(post).toHaveBeenCalledTimes(1);
  });

  it('allows a new refresh once the last one has finished', async () => {
    post.mockResolvedValueOnce({ data: { accessToken: 'one' } }).mockResolvedValueOnce({ data: { accessToken: 'two' } });

    expect(await refreshAccessToken()).toBe('one');
    expect(await refreshAccessToken()).toBe('two');
  });
});

describe('ending the session', () => {
  beforeEach(() => {
    post.mockReset();
    clearAccessToken();
  });

  it('drops the token at once', async () => {
    setAccessToken('token');

    const ended = endSession();

    expect(getAccessToken()).toBeNull();
    await ended;
  });

  it('does not let a refresh already under way sign the page back in', async () => {
    let resolve!: (v: { data: { accessToken: string } }) => void;
    post.mockReturnValue(new Promise((r) => (resolve = r)));
    const inFlight = refreshAccessToken();
    inFlight.catch(() => {});

    const ended = endSession();
    resolve({ data: { accessToken: 'late' } });
    await ended;

    expect(getAccessToken()).toBeNull();
    await expect(inFlight).rejects.toThrow('Signed out');
  });

  it('waits for a refresh under way, so the refresh cookie is final before it is revoked', async () => {
    let resolve!: (v: { data: { accessToken: string } }) => void;
    post.mockReturnValue(new Promise((r) => (resolve = r)));
    refreshAccessToken().catch(() => {});
    let finished = false;

    const ended = endSession().then(() => {
      finished = true;
    });
    await Promise.resolve();
    expect(finished).toBe(false);

    resolve({ data: { accessToken: 'late' } });
    await ended;
    expect(finished).toBe(true);
  });

  it('lets a later sign-in refresh normally', async () => {
    await endSession();
    post.mockResolvedValue({ data: { accessToken: 'fresh' } });

    expect(await refreshAccessToken()).toBe('fresh');
    expect(getAccessToken()).toBe('fresh');
  });
});

