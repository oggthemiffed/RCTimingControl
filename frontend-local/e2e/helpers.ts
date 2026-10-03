import type { APIRequestContext, Page } from '@playwright/test';
import { expect } from '@playwright/test';

export interface SeedResult {
  officialName: string;
  officialSecret: string;
  raceAId: number;
  raceBId: number;
}

/**
 * Seeds a fresh day-open + official + two-round race schedule via the e2e-profile-only
 * `/api/v1/test-support/seed` endpoint (see `E2eSeedController`). Bypasses the real cloud
 * pre-cache/open flow entirely — these tests prove the local race-day program works with zero
 * cloud connectivity, so faking that round-trip would be less faithful than skipping it.
 */
export async function seed(request: APIRequestContext): Promise<SeedResult> {
  const response = await request.post('/api/v1/test-support/seed');
  expect(response.ok(), `seed() failed: ${response.status()} ${await response.text()}`).toBeTruthy();
  const body = await response.json();
  return {
    officialName: body.officialName,
    officialSecret: body.officialSecret,
    raceAId: body.raceAId,
    raceBId: body.raceBId,
  };
}

/** Logs the given seeded official into the UI from a fresh (logged-out) LoginPage. */
export async function loginAsOfficial(page: Page, officialName: string, secret: string) {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Local Race Day Login' })).toBeVisible();
  await page.getByLabel('Official').selectOption({ label: officialName });
  await page.getByLabel('PIN').fill(secret);
  await page.getByRole('button', { name: 'Log in' }).click();
  await expect(page.getByRole('heading', { name: 'Race Control' })).toBeVisible();
}
