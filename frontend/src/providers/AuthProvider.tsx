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
  login: (email: string, password: string, redirectTo?: string) => Promise<void>;
  logout: () => void;
  isLoading: boolean;
  /** Used by AdminBootstrapGate to store JWT after bootstrap without navigating */
  setAuthFromToken: (token: string, authUser: AuthUser) => void;
}

interface AuthResponse {
  accessToken: string;
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: AuthUser['roles'];
}

function authResponseToUser(data: AuthResponse): AuthUser {
  return { id: data.id, email: data.email, firstName: data.firstName, lastName: data.lastName, roles: data.roles };
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
      .post<AuthResponse>('/api/v1/auth/refresh', {}, { withCredentials: true })
      .then(({ data }) => {
        setAccessToken(data.accessToken);
        setAccessTokenState(data.accessToken);
        setUser(authResponseToUser(data));
      })
      .catch(() => {
        // No valid refresh cookie — user remains unauthenticated
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, []);

  const login = async (email: string, password: string, redirectTo?: string): Promise<void> => {
    const { data } = await api.post<AuthResponse>('/api/v1/auth/login', { email, password });
    const authUser = authResponseToUser(data);
    setAccessToken(data.accessToken);
    setAccessTokenState(data.accessToken);
    setUser(authUser);

    if (redirectTo) {
      navigate(redirectTo);
    } else {
      const staffRoles: AuthUser['roles'][number][] = ['ADMIN', 'RACE_DIRECTOR', 'REFEREE'];
      const isStaff = authUser.roles.some((r) => staffRoles.includes(r));
      navigate(isStaff ? '/admin' : '/racer');
    }
  };

  const logout = () => {
    clearAccessToken();
    setAccessTokenState(null);
    setUser(null);
    navigate('/login');
  };

  const setAuthFromToken = (token: string, authUser: AuthUser) => {
    setAccessToken(token);
    setAccessTokenState(token);
    setUser(authUser);
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
      value={{ user, accessToken: accessTokenState, login, logout, isLoading, setAuthFromToken }}
    >
      {children}
    </AuthContext.Provider>
  );
}
