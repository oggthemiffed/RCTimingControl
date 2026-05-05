import { describe, it, expect } from 'vitest';

describe.skip('SetupGuard (Wave 0 stub — enabled in Plan 04)', () => {
  it('redirects to /setup when setupComplete is false and pathname is not /setup', () => {
    expect.fail('Not implemented');
  });
  it('does NOT redirect when pathname starts with /setup (Pitfall 1: infinite redirect)', () => {
    expect.fail('Not implemented');
  });
  it('renders children when setupComplete is true', () => {
    expect.fail('Not implemented');
  });
  it('shows loading spinner while status query is loading', () => {
    expect.fail('Not implemented');
  });
});
