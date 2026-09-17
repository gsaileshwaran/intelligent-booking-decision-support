import React, { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import type { User, LoginRequest, RegisterRequest } from '../types/auth';
import { authApi } from '../api/client';

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isManager: boolean;
  isAdmin: boolean;
  isLoading: boolean;
  selectedCityId: number | null;
  setSelectedCityId: (cityId: number | null) => void;
  login: (credentials: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('pvk_token'));
  const [user, setUser] = useState<User | null>(() => {
    const saved = localStorage.getItem('pvk_user');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch {
        return null;
      }
    }
    return null;
  });
  const [selectedCityId, setSelectedCityIdState] = useState<number | null>(() => {
    const saved = localStorage.getItem('pvk_selected_city');
    return saved ? parseInt(saved, 10) : 1; // Default to cityId 1 (e.g. Bangalore)
  });
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const setSelectedCityId = (cityId: number | null) => {
    setSelectedCityIdState(cityId);
    if (cityId) {
      localStorage.setItem('pvk_selected_city', cityId.toString());
    } else {
      localStorage.removeItem('pvk_selected_city');
    }
  };

  const refreshUser = async () => {
    if (!token) {
      setUser(null);
      setIsLoading(false);
      return;
    }
    try {
      const me = await authApi.getCurrentUser();
      setUser(me);
      localStorage.setItem('pvk_user', JSON.stringify(me));
    } catch {
      // Token might be expired or invalid
      logout();
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (token) {
      refreshUser();
    } else {
      setIsLoading(false);
    }
  }, [token]);

  const login = async (credentials: LoginRequest) => {
    const res = await authApi.login(credentials);
    setToken(res.token);
    localStorage.setItem('pvk_token', res.token);
    const u: User = {
      userId: res.userId,
      email: res.email,
      firstName: res.firstName,
      lastName: res.lastName,
      role: res.role,
      roles: [res.role],
      permissions: res.permissions || [],
    };
    setUser(u);
    localStorage.setItem('pvk_user', JSON.stringify(u));
  };

  const register = async (data: RegisterRequest) => {
    const res = await authApi.register(data);
    setToken(res.token);
    localStorage.setItem('pvk_token', res.token);
    const u: User = {
      userId: res.userId,
      email: res.email,
      firstName: res.firstName,
      lastName: res.lastName,
      role: res.role,
      roles: [res.role],
      permissions: res.permissions || [],
    };
    setUser(u);
    localStorage.setItem('pvk_user', JSON.stringify(u));
  };

  const logout = async () => {
    try {
      await authApi.logout();
    } finally {
      setToken(null);
      setUser(null);
      localStorage.removeItem('pvk_token');
      localStorage.removeItem('pvk_user');
    }
  };

  const roles = user?.roles || (user?.role ? [user.role] : []);
  const isAdmin = roles.some((r) => r === 'ROLE_SUPER_ADMIN' || r === 'SUPER_ADMIN');
  const isManager = roles.some((r) => r === 'ROLE_THEATRE_MANAGER' || r === 'THEATRE_MANAGER') || isAdmin;

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token && !!user,
        isManager,
        isAdmin,
        isLoading,
        selectedCityId,
        setSelectedCityId,
        login,
        register,
        logout,
        refreshUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
