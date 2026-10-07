import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { useContext } from 'react';
import axios from 'axios';

import { AuthContext, AuthProvider } from './AuthProvider';
import { getAccessToken, clearAccessToken } from '@/lib/auth';
import api from '@/lib/api';

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

describe('AuthProvider sign-out', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    clearAccessToken();
  });

  it('revokes the refresh token on the server, then clears the session here', async () => {
    const revoke = vi.spyOn(api, 'delete').mockResolvedValue({ status: 204 });
    vi.spyOn(axios, 'post').mockResolvedValue({
      data: { accessToken: 'restored-token', id: '1', email: 'someone@example.com', firstName: 'Some', lastName: 'One', roles: ['REFEREE'] },
    });
    function SignOutButton() {
      const auth = useContext(AuthContext);
      return <button onClick={() => auth?.logout()}>Sign out</button>;
    }
    const router = createMemoryRouter(
      [
        { path: '/', element: <AuthProvider><WhoIsSignedIn /><SignOutButton /></AuthProvider> },
        { path: '/login', element: <p>Login page</p> },
      ],
      { initialEntries: ['/'] },
    );
    render(<RouterProvider router={router} />);
    await screen.findByText('Signed in as someone@example.com');

    fireEvent.click(screen.getByText('Sign out'));

    expect(revoke).toHaveBeenCalledWith('/api/v1/auth/refresh');
    await waitFor(() => expect(screen.getByText('Login page')).toBeInTheDocument());
    expect(getAccessToken()).toBeNull();
  });

  it('still signs out here when the server cannot be reached', async () => {
    vi.spyOn(api, 'delete').mockRejectedValue(new Error('offline'));
    vi.spyOn(axios, 'post').mockResolvedValue({
      data: { accessToken: 'restored-token', id: '1', email: 'someone@example.com', firstName: 'Some', lastName: 'One', roles: ['REFEREE'] },
    });
    function SignOutButton() {
      const auth = useContext(AuthContext);
      return <button onClick={() => auth?.logout()}>Sign out</button>;
    }
    const router = createMemoryRouter(
      [
        { path: '/', element: <AuthProvider><SignOutButton /></AuthProvider> },
        { path: '/login', element: <p>Login page</p> },
      ],
      { initialEntries: ['/'] },
    );
    render(<RouterProvider router={router} />);
    fireEvent.click(await screen.findByText('Sign out'));

    await waitFor(() => expect(screen.getByText('Login page')).toBeInTheDocument());
    expect(getAccessToken()).toBeNull();
  });
});
