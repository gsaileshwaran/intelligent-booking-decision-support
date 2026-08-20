import React, { createContext, useContext, useState, useEffect } from 'react';
import { authService } from '../services/authService';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const storedUser = authService.getCurrentUser();
    if (storedUser) {
      setUser(storedUser);
    }
    setLoading(false);
  }, []);

  const login = async (email, password) => {
    const data = await authService.login(email, password);
    if (data.success && data.data) {
      setUser(data.data);
    }
    return data;
  };

  const register = async (name, email, password, role) => {
    const data = await authService.register(name, email, password, role);
    if (data.success && data.data) {
      setUser(data.data);
    }
    return data;
  };

  const updateUser = (updatedFields) => {
    if (!user) return;
    const newUserData = { ...user, ...updatedFields };
    localStorage.setItem('user', JSON.stringify(newUserData));
    setUser(newUserData);
  };

  const logout = () => {
    authService.logout();
    setUser(null);
  };

  const isCustomer = user?.role === 'ROLE_CUSTOMER';
  const isProvider = user?.role === 'ROLE_SERVICE_PROVIDER' || user?.role === 'ROLE_ADMIN';
  const isAdmin = user?.role === 'ROLE_ADMIN';

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        login,
        register,
        updateUser,
        logout,
        isAuthenticated: !!user,
        isCustomer,
        isProvider,
        isAdmin,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
