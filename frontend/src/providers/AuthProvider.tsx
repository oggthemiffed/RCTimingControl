import React, { createContext, useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import axios from 'axios';
import api from '@/lib/api';
import { setAccessToken, clearAccessToken } from '@/lib/auth';

export interface AuthUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: Array<'RACER' | 'ADMIN' | 'RACE_DIRECTOR' | 'REFEREE'>;
}

export interface AuthContextValue {
  user: AuthUser | null;
  accessToken: string | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  isLoading: boolean;
}

// eslint-disable-next-line react-refresh/only-export-components
export const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [accessTokenState, setAccessTokenState] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    // Attempt silent session restore via HttpOnly refresh cookie
    axios
      .post<{ accessToken: string; user: AuthUser }>(
        '/api/v1/auth/refresh',
        {},
        { withCredentials: true }
      )
      .then(({ data }) => {
        setAccessToken(data.accessToken);
        setAccessTokenState(data.accessToken);
        setUser(data.user);
      })
      .catch(() => {
        // No valid refresh cookie — user remains unauthenticated
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, []);

  const login = async (email: string, password: string): Promise<void> => {
    const { data } = await api.post<{ accessToken: string; user: AuthUser }>(
      '/api/v1/auth/login',
      { email, password }
    );
    setAccessToken(data.accessToken);
    setAccessTokenState(data.accessToken);
    setUser(data.user);

    const staffRoles: AuthUser['roles'][number][] = ['ADMIN', 'RACE_DIRECTOR', 'REFEREE'];
    const isStaff = data.user.roles.some((r) => staffRoles.includes(r));
    navigate(isStaff ? '/admin' : '/racer');
  };

  const logout = () => {
    clearAccessToken();
    setAccessTokenState(null);
    setUser(null);
    navigate('/login');
  };

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <AuthContext.Provider
      value={{ user, accessToken: accessTokenState, login, logout, isLoading }}
    >
      {children}
    </AuthContext.Provider>
  );
}
