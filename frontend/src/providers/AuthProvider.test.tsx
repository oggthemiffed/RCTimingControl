import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { useContext } from 'react';
import axios from 'axios';

import { AuthContext, AuthProvider } from './AuthProvider';
import { getAccessToken, clearAccessToken } from '@/lib/auth';

function WhoIsSignedIn() {
  const auth = useContext(AuthContext);
  return <p>{auth?.user ? `Signed in as ${auth.user.email}` : 'Signed out'}</p>;
}

function renderWithRestoredSession(roles: string[]) {
  vi.spyOn(axios, 'post').mockResolvedValue({
    data: { accessToken: 'restored-token', id: '1', email: 'someone@example.com', firstName: 'Some', lastName: 'One', roles },
  });
  const router = createMemoryRouter(
    [{ path: '/', element: <AuthProvider><WhoIsSignedIn /></AuthProvider> }],
    { initialEntries: ['/'] },
  );
  render(<RouterProvider router={router} />);
}

describe('AuthProvider session restore', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    clearAccessToken();
  });

  it('restores an official from the refresh cookie', async () => {
    renderWithRestoredSession(['REFEREE']);

    expect(await screen.findByText('Signed in as someone@example.com')).toBeInTheDocument();
    expect(getAccessToken()).toBe('restored-token');
  });

  it('does not restore an account with no official role', async () => {
    renderWithRestoredSession(['RACER']);

    expect(await screen.findByText('Signed out')).toBeInTheDocument();
    expect(getAccessToken()).toBeNull();
  });
});
