import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { createMemoryRouter, Outlet, RouterProvider } from 'react-router-dom';
import axios, { AxiosError, AxiosHeaders } from 'axios';

import LoginPage from './LoginPage';
import { AuthProvider } from '@/providers/AuthProvider';
import api from '@/lib/api';

vi.mock('@/lib/api', () => ({ default: { post: vi.fn() } }));
vi.mock('sonner', () => ({ toast: { error: vi.fn() } }));

function loginResponse(roles: string[]) {
  return {
    data: { accessToken: 'token', id: '1', email: 'someone@example.com', firstName: 'Some', lastName: 'One', roles },
  };
}

function renderLogin() {
  const router = createMemoryRouter(
    [{
      // AuthProvider needs the router context, so it wraps the routes through an outlet
      element: <AuthProvider><Outlet /></AuthProvider>,
      children: [
        { path: '/login', element: <LoginPage /> },
        { path: '/admin', element: <h1>Admin home</h1> },
      ],
    }],
    { initialEntries: ['/login'] },
  );
  render(<RouterProvider router={router} />);
  return router;
}


async function signIn() {
  fireEvent.change(await screen.findByLabelText('Email'), { target: { value: 'someone@example.com' } });
  fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'secret' } });
  fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));
}

describe('LoginPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    // No refresh cookie: the session restore on load fails
    vi.spyOn(axios, 'post').mockRejectedValue(new Error('no session'));
  });

  it('takes an official to the admin panel', async () => {
    vi.mocked(api.post).mockResolvedValue(loginResponse(['RACE_DIRECTOR']));
    renderLogin();

    await signIn();

    expect(await screen.findByRole('heading', { name: 'Admin home' })).toBeInTheDocument();
  });

  it('turns away an account with no official role', async () => {
    vi.mocked(api.post).mockResolvedValue(loginResponse(['RACER']));
    const router = renderLogin();

    await signIn();

    expect(await screen.findByText('This sign-in is for race officials only.')).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/login');
  });

  it('shows the officials-only message when the server refuses the account', async () => {
    vi.mocked(api.post).mockRejectedValue(
      new AxiosError('Forbidden', 'ERR_BAD_REQUEST', undefined, null, {
        status: 403, statusText: 'Forbidden', data: {}, headers: {}, config: { headers: new AxiosHeaders() },
      }),
    );
    renderLogin();

    await signIn();

    expect(await screen.findByText('This sign-in is for race officials only.')).toBeInTheDocument();
  });

  it('tells a disabled official to ask an admin', async () => {
    vi.mocked(api.post).mockRejectedValue(
      new AxiosError('Forbidden', 'ERR_BAD_REQUEST', undefined, null, {
        status: 403, statusText: 'Forbidden', data: { reason: 'disabled' }, headers: {}, config: { headers: new AxiosHeaders() },
      }),
    );
    renderLogin();

    await signIn();

    expect(await screen.findByText('This account has been disabled. Ask a club admin to enable it.')).toBeInTheDocument();
  });

  it('offers no self-registration or password reset links', async () => {
    renderLogin();

    expect(await screen.findByRole('button', { name: 'Sign in' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /register/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /forgot/i })).not.toBeInTheDocument();
  });
});
