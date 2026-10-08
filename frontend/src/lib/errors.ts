import { isAxiosError } from 'axios';

/**
 * The server's reason for refusing a request (the `detail` of its problem response, or an older `message`),
 * or `fallback` when there is none, such as a network failure. Axios's own message ("Request failed with
 * status code 409") never reaches the user.
 */
export function getApiErrorMessage(err: unknown, fallback: string): string {
  if (!isAxiosError(err)) return fallback;
  const data = err.response?.data as { detail?: unknown; message?: unknown } | undefined;
  if (typeof data?.detail === 'string' && data.detail) return data.detail;
  if (typeof data?.message === 'string' && data.message) return data.message;
  return fallback;
}

/** The HTTP status the server answered with, or undefined when there was no answer, such as a network failure. */
export function getApiErrorStatus(err: unknown): number | undefined {
  return isAxiosError(err) ? err.response?.status : undefined;
}
