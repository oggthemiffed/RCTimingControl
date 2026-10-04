let accessToken: string | null = null;

export function getAccessToken(): string | null {
  return accessToken;
}

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

export function clearAccessToken(): void {
  accessToken = null;
}

/** Thrown by login when the account holds no official role: only officials can sign in. */
export class NotAnOfficialError extends Error {
  constructor() {
    super('Only race officials can sign in.');
    this.name = 'NotAnOfficialError';
  }
}
