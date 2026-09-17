import React, { type ReactNode } from 'react';
import { useAuth } from '../../context/AuthContext';

interface ProtectedRouteProps {
  children: ReactNode;
  requiredRole?: 'MANAGER' | 'ADMIN';
  onNavigate: (view: string) => void;
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  children,
  requiredRole,
  onNavigate,
}) => {
  const { isAuthenticated, isManager, isAdmin, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div style={{ textAlign: 'center', padding: '80px 20px', color: 'var(--text-secondary)' }}>
        <p>Checking authentication privileges...</p>
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <div className="card" style={{ maxWidth: '480px', margin: '60px auto', textAlign: 'center' }}>
        <h3 style={{ marginBottom: '12px' }}>Authentication Required</h3>
        <p style={{ marginBottom: '24px' }}>Please sign in to access this portal or administrative tool.</p>
        <button className="btn btn-primary" onClick={() => onNavigate('login')}>
          Sign In Now
        </button>
      </div>
    );
  }

  if (requiredRole === 'MANAGER' && !isManager) {
    return (
      <div className="card" style={{ maxWidth: '480px', margin: '60px auto', textAlign: 'center' }}>
        <h3 style={{ marginBottom: '12px', color: '#EF4444' }}>Access Denied</h3>
        <p style={{ marginBottom: '24px' }}>
          This area requires <strong>Theatre Manager</strong> or <strong>Super Admin</strong> credentials.
        </p>
        <button className="btn btn-secondary" onClick={() => onNavigate('home')}>
          Return to Home
        </button>
      </div>
    );
  }

  if (requiredRole === 'ADMIN' && !isAdmin) {
    return (
      <div className="card" style={{ maxWidth: '480px', margin: '60px auto', textAlign: 'center' }}>
        <h3 style={{ marginBottom: '12px', color: '#EF4444' }}>Super Admin Restricted</h3>
        <p style={{ marginBottom: '24px' }}>
          This view is strictly restricted to <strong>Super Admin</strong> platform operators.
        </p>
        <button className="btn btn-secondary" onClick={() => onNavigate('home')}>
          Return to Home
        </button>
      </div>
    );
  }

  return <>{children}</>;
};
