import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import AdminPanelLayout from '../AdminPanelLayout';

// Mock useAuth to provide a basic admin user
vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({
    user: {
      id: '1',
      email: 'admin@example.com',
      firstName: 'Admin',
      lastName: 'User',
      roles: ['ADMIN'],
    },
    logout: vi.fn(),
    isLoading: false,
    login: vi.fn(),
    accessToken: 'mock-token',
    setAuthFromToken: vi.fn(),
  }),
}));

describe('AdminPanelLayout (Wave 0 stub — enabled in Plan 04)', () => {
  it('renders Setup Wizard nav entry linking to /setup (SC-5)', () => {
    render(
      <MemoryRouter initialEntries={['/admin']}>
        <AdminPanelLayout />
      </MemoryRouter>,
    );

    const link = screen.getByRole('link', { name: /Setup Wizard/i });
    expect(link).toBeTruthy();
    expect(link.getAttribute('href')).toMatch(/\/setup$/);
  });
});
