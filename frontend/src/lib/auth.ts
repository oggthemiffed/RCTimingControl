import axios from 'axios';

// The access token lives only in memory. The HttpOnly refresh cookie is what keeps an official signed
// in across page loads, so a reload gets a new access token from /auth/refresh (see AuthProvider).
let accessToken: string | null = null;

// The same base the api instance uses, so a refresh goes to the server the rest of the app talks to
const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL || '';

export function getAccessToken(): string | null {
  return accessToken;
}

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

export function clearAccessToken(): void {
  accessToken = null;
}

// Bumped by endSession. A refresh started before then must not store its token: it would sign the page
// back in after the person signed out.
let sessionGeneration = 0;

// Refreshes in flight share one request: the refresh token rotates, so a second concurrent call would
// present a token the first has already used up and sign the official out.
let refreshing: Promise<string> | null = null;

/**
 * Gets a new access token with the refresh cookie and stores it. Uses plain axios, not the api
 * instance, so its own 401 cannot trigger another refresh. Rejects when the cookie is missing, expired
 * or revoked.
 */
export function refreshAccessToken(): Promise<string> {
  const generation = sessionGeneration;
  refreshing ??= axios
    .post<{ accessToken: string }>(`${API_BASE_URL}/api/v1/auth/refresh`, {}, { withCredentials: true })
    .then(({ data }) => {
      if (generation !== sessionGeneration) throw new Error('Signed out while refreshing');
      setAccessToken(data.accessToken);
      return data.accessToken;
    })
    .finally(() => {
      refreshing = null;
    });
  return refreshing;
}

/**
 * Ends this page's session. Drops the access token at once and makes any refresh still in flight discard
 * its result. Resolves when no refresh is outstanding: that refresh may already have rotated the refresh
 * cookie, and the cookie must be final before the server is asked to revoke it, or the revoke would hit
 * the old token and the new one would keep the browser signed in.
 */
export async function endSession(): Promise<void> {
  sessionGeneration++;
  accessToken = null;
  try {
    await refreshing;
  } catch {
    // A refresh that failed (or was discarded) has nothing left to wait for
  }
}

/** Seconds since the epoch at which a JWT expires, or null when it cannot be read. */
function expiryOf(token: string): number | null {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const exp = (JSON.parse(atob(payload)) as { exp?: unknown }).exp;
    return typeof exp === 'number' ? exp : null;
  } catch {
    return null;
  }
}

/**
 * The access token to connect with: the current one, or a new one when it has expired or is about to.
 * Access tokens last 15 minutes, and a WebSocket that reconnects with an expired one is refused, so a
 * long-open page must renew it before each connect. Returns null for an anonymous visitor (nothing to
 * renew) and when the session can no longer be refreshed, which leaves only the public topics.
 */
export async function currentAccessToken(): Promise<string | null> {
  const token = getAccessToken();
  if (!token) return null;
  const exp = expiryOf(token);
  if (exp === null || exp * 1000 - Date.now() > 30_000) return token;
  try {
    return await refreshAccessToken();
  } catch {
    return null;
  }
}

/** Thrown by login when the account holds no official role: only officials can sign in. */
export class NotAnOfficialError extends Error {
  constructor() {
    super('Only race officials can sign in.');
    this.name = 'NotAnOfficialError';
  }
}
