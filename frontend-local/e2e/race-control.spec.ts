import { test, expect, type Page } from '@playwright/test';
import { seed, loginAsOfficial } from './helpers';

/**
 * AE1 (docs/plans/2026-08-06-001-feat-offline-race-day-resilience-split-plan.md): given the
 * venue has no internet connectivity, an official can run the full local race-control workflow
 * — start/stop, marshal adjustments, round-to-round grid propagation — and attendees see
 * updated now/next boards and results. This suite never talks to the cloud at all (the seed
 * fixture bypasses pre-cache/open entirely), which is a stronger proof of "zero connectivity"
 * than mocking a cloud response would be.
 *
 * One long, sequential test rather than several independent ones: each step's assertion is the
 * precondition for the next (you cannot marshal-adjust a PENDING race, or advance a RUNNING
 * one), so splitting this into separate `test()`s would just reintroduce the same ordering
 * dependency through shared module state instead of removing it.
 */
test('official runs a full race day flow fully offline (AE1)', async ({ page, request, context }) => {
  const { officialName, officialSecret } = await seed(request);

  await test.step('official logs in locally', async () => {
    await loginAsOfficial(page, officialName, officialSecret);
  });

  await test.step('official selects the first scheduled race', async () => {
    await page.getByRole('button', { name: /Round 1 · Heat 1/ }).click();
    await expect(page.getByRole('heading', { level: 2, name: /Round 1 · Heat 1/ })).toBeVisible();
    await expect(page.getByText('Status: PENDING')).toBeVisible();
  });

  await test.step('official calls the race to grid, then starts it', async () => {
    await page.getByRole('button', { name: 'Call to grid' }).click();
    await expect(page.getByText('Status: GRID')).toBeVisible();

    await page.getByRole('button', { name: 'Start race' }).click();
    await expect(page.getByText('Status: RUNNING')).toBeVisible();
  });

  await test.step('spectator board reflects the running race', async () => {
    const boardPage = await context.newPage();
    await boardPage.goto('/boards/now-next');
    await expect(boardPage.getByRole('heading', { name: /Round 1 · Heat 1/ })).toBeVisible();
    await expect(boardPage.getByText('Status: RUNNING')).toBeVisible();
    await boardPage.close();
  });

  await test.step('marshal adjustments record a finishing order with no decoder hardware', async () => {
    // Carol finishes 1st (3 laps), Alice 2nd (2 laps), Bob 3rd (1 lap) — distinct lap counts so
    // the finishing order advance-round propagates is unambiguous.
    await clickMarshalLap(page, 'Carol Racer', 3);
    await clickMarshalLap(page, 'Alice Racer', 2);
    await clickMarshalLap(page, 'Bob Racer', 1);

    // Live timing is sourced from the STOMP broadcast a marshal adjustment triggers, not the
    // REST call's own response — wait for all three to arrive before trusting the order.
    // Two parent hops from the heading reaches LiveTimingTable's own wrapping div (the one
    // that also contains the table) rather than every ancestor div up to the page root.
    const liveTimingSection = page.getByRole('heading', { name: 'Live timing' }).locator('../..');
    await expect(liveTimingSection.getByRole('row', { name: /Carol Racer/ })).toBeVisible({ timeout: 10_000 });
    await expect(liveTimingSection.getByRole('row', { name: /Alice Racer/ })).toBeVisible();
    await expect(liveTimingSection.getByRole('row', { name: /Bob Racer/ })).toBeVisible();

    const liveOrder = await liveTimingSection.innerText();
    assertAppearsInOrder(liveOrder, ['Carol Racer', 'Alice Racer', 'Bob Racer']);
  });

  await test.step('official stops the race', async () => {
    await page.getByRole('button', { name: 'Stop' }).click();
    await expect(page.getByText('Status: STOPPED')).toBeVisible();
  });

  await test.step('official advances the finishing order into round 2', async () => {
    await page.getByLabel('Next race').selectOption({ label: 'Round 2 · Heat 1 — Stock Buggy' });
    await page.getByRole('button', { name: 'Advance to next round', exact: true }).click();
    await expect(page.getByText('Advanced — next race grid set.')).toBeVisible();

    const advanceSection = page.locator('section', { hasText: 'Advance to next round' });
    const order = await advanceSection.innerText();
    assertAppearsInOrder(order, ['Carol Racer', 'Alice Racer', 'Bob Racer']);
  });

  await test.step('spectator board shows the next scheduled race once round 1 is stopped', async () => {
    const boardPage = await context.newPage();
    await boardPage.goto('/boards/now-next');
    await expect(boardPage.getByText('Status: STOPPED')).toBeVisible();
    await expect(boardPage.getByText(/Round 2 · Heat 1 — Stock Buggy/)).toBeVisible();
    await boardPage.close();
  });
});

async function clickMarshalLap(page: Page, driverName: string, times: number) {
  const row = page.getByRole('row', { name: new RegExp(driverName) }).first();
  const addLapButton = row.getByRole('button', { name: 'Add marshal lap' });
  for (let i = 0; i < times; i++) {
    await addLapButton.click();
    await expect(addLapButton).toBeEnabled();
  }
}

function assertAppearsInOrder(text: string, namesInOrder: string[]) {
  let cursor = -1;
  for (const name of namesInOrder) {
    const idx = text.indexOf(name, cursor + 1);
    expect(idx, `expected "${name}" to appear after position ${cursor} in:\n${text}`).toBeGreaterThan(cursor);
    cursor = idx;
  }
}

