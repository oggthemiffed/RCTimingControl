import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { AxiosError, type InternalAxiosRequestConfig } from 'axios';

import api from './api';
import { clearAccessToken, setAccessToken } from './auth';

const { refreshAccessToken } = vi.hoisted(() => ({ refreshAccessToken: vi.fn() }));
vi.mock('./auth', async (importOriginal) => ({
  ...(await importOriginal<typeof import('./auth')>()),
  refreshAccessToken,
}));

/** Answers each request with the next status in the list (200 once it runs out), recording the URLs. */
function answerWith(...statuses: number[]) {
  const urls: string[] = [];
  api.defaults.adapter = async (config: InternalAxiosRequestConfig) => {
    urls.push(String(config.url));
    const status = statuses.shift() ?? 200;
    const response = { status, statusText: '', data: {}, headers: {}, config };
    if (status >= 400) throw new AxiosError('failed', String(status), config, null, response);
    return response;
  };
  return urls;
}

describe('api 401 handling', () => {
  const originalLocation = window.location;

  beforeEach(() => {
    refreshAccessToken.mockReset();
    clearAccessToken();
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: { pathname: '/admin/events', search: '', href: '' },
    });
  });

  afterEach(() => {
    Object.defineProperty(window, 'location', { configurable: true, value: originalLocation });
  });

  it('shows a wrong password as a 401, without refreshing or leaving the page', async () => {
    answerWith(401);

    await expect(api.post('/api/v1/auth/login', {})).rejects.toMatchObject({ response: { status: 401 } });

    expect(refreshAccessToken).not.toHaveBeenCalled();
    expect(window.location.href).toBe('');
  });

  it('renews the token and retries once when an ordinary request is refused', async () => {
    const urls = answerWith(401, 200);
    refreshAccessToken.mockImplementation(async () => {
      setAccessToken('renewed');
      return 'renewed';
    });

    const response = await api.get('/api/v1/admin/events');

    expect(response.status).toBe(200);
    expect(urls).toEqual(['/api/v1/admin/events', '/api/v1/admin/events']);
    expect(refreshAccessToken).toHaveBeenCalledTimes(1);
  });

  it('sends the person to sign in again when the session cannot be renewed', async () => {
    answerWith(401);
    refreshAccessToken.mockRejectedValue(new Error('expired'));

    await expect(api.get('/api/v1/admin/events')).rejects.toBeTruthy();

    expect(window.location.href).toBe('/login?from=%2Fadmin%2Fevents');
  });

  it('does not retry a request twice', async () => {
    answerWith(401, 401);
    refreshAccessToken.mockResolvedValue('renewed');

    await expect(api.get('/api/v1/admin/events')).rejects.toMatchObject({ response: { status: 401 } });

    expect(refreshAccessToken).toHaveBeenCalledTimes(1);
  });
});
