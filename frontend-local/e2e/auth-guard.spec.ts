import { test, expect } from '@playwright/test';
import { seed } from './helpers';

/**
 * AE4 (docs/plans/2026-08-06-001-feat-offline-race-day-resilience-split-plan.md): an
 * unauthenticated device on the venue's local network attempting a race-control write action is
 * rejected — only an authenticated official session can perform one.
 */
test.describe('unauthenticated write actions are rejected (AE4)', () => {
  test('a race-control transition request with no session token is rejected', async ({ request }) => {
    const { raceAId } = await seed(request);

    // No Authorization header at all — exactly what an unauthenticated device on the venue LAN
    // would send. LocalSecurityConfig's default-deny `anyRequest().authenticated()` rule must
    // reject this before RaceControlController's own logic ever runs.
    const response = await request.post(`/api/v1/race-control/races/${raceAId}/transition`, {
      data: { target: 'GRID' },
    });

    expect(response.status()).toBe(401);
  });

  test('a marshal-adjustment request with no session token is rejected', async ({ request }) => {
    const { raceAId } = await seed(request);

    const response = await request.post(`/api/v1/race-control/races/${raceAId}/marshal-adjustment`, {
      data: { cachedEntryId: 1, lapDelta: 1 },
    });

    expect(response.status()).toBe(401);
  });

  test('a request with a garbage bearer token is rejected the same way', async ({ request }) => {
    const { raceAId } = await seed(request);

    const response = await request.post(`/api/v1/race-control/races/${raceAId}/transition`, {
      headers: { Authorization: 'Bearer not-a-real-session-token' },
      data: { target: 'GRID' },
    });

    expect(response.status()).toBe(401);
  });

  test('visiting the app with no stored session shows the login screen, not race control', async ({
    page,
    request,
  }) => {
    await seed(request);
    await page.goto('/');
    await expect(page.getByRole('heading', { name: 'Local Race Day Login' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Race Control' })).not.toBeVisible();
  });
});
