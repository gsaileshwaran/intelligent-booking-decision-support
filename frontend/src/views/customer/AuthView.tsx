import React, { useState } from 'react';
import { Film, Lock, Mail, AlertCircle } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';

interface AuthViewProps {
  onNavigate: (view: string) => void;
}

export const AuthView: React.FC<AuthViewProps> = ({ onNavigate }) => {
  const { login, register } = useAuth();
  const { showToast } = useToast();
  const [isLoginTab, setIsLoginTab] = useState(true);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [phone, setPhone] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleLoginSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    if (!email || !password) {
      setErrorMessage('Please provide both email and password.');
      return;
    }
    try {
      setLoading(true);
      await login({ email, passwordHash: password });
      showToast('Successfully signed in!', 'success');
      onNavigate('home');
    } catch (err: any) {
      console.error('Login failed', err);
      setErrorMessage(err.message || 'Invalid credentials. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleRegisterSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    if (!email || !password || !firstName || !lastName) {
      setErrorMessage('Please complete all required fields.');
      return;
    }
    try {
      setLoading(true);
      await register({
        email,
        passwordHash: password,
        firstName,
        lastName,
        phone: phone || undefined,
      });
      showToast('Registration successful! Welcome to PVK Cinemas.', 'success');
      onNavigate('home');
    } catch (err: any) {
      console.error('Registration failed', err);
      setErrorMessage(err.message || 'Registration failed. Please check your details.');
    } finally {
      setLoading(false);
    }
  };

  const fillTestCredentials = async (role: 'ADMIN' | 'MANAGER' | 'CUSTOMER') => {
    let email: string;
    let password: string;
    if (role === 'ADMIN') {
      email = 'admin@pvkcinemas.com';
      password = 'Password123!';
    } else if (role === 'MANAGER') {
      email = 'manager@pvkcinemas.com';
      password = 'Password123!';
    } else {
      email = 'customer@pvkcinemas.com';
      password = 'Password123!';
    }
    setEmail(email);
    setPassword(password);
    setErrorMessage(null);
    setIsLoginTab(true);
    // Auto-submit login with demo credentials
    try {
      setLoading(true);
      await login({ email, passwordHash: password });
      showToast(`Signed in as ${role.charAt(0) + role.slice(1).toLowerCase()}!`, 'success');
      onNavigate('home');
    } catch (err: any) {
      console.error('Demo quick-login failed', err);
      setErrorMessage(err.message || 'Login failed. Run the demo seed script first.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: '460px', margin: '40px auto' }} data-testid="auth-view">
      {/* Brand Header */}
      <div style={{ textAlign: 'center', marginBottom: '28px' }}>
        <div style={{
          background: 'var(--accent-crimson)',
          color: '#fff',
          borderRadius: '10px',
          width: '52px',
          height: '52px',
          margin: '0 auto 16px auto',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: 'var(--shadow-glow)',
        }}>
          <Film size={28} />
        </div>
        <h2 style={{ fontSize: '1.8rem', marginBottom: '6px' }}>PVK Cinemas Account</h2>
        <p style={{ fontSize: '0.92rem' }}>Sign in or create a customer profile to access tailored preferences</p>
      </div>

      <div className="card" style={{ padding: '32px' }}>
        {/* Tab switcher */}
        <div className="tabs" role="tablist" aria-label="Authentication modes" style={{ marginBottom: '24px' }}>
          <button
            role="tab"
            aria-selected={isLoginTab}
            onClick={() => { setIsLoginTab(true); setErrorMessage(null); }}
            className={`tab-btn ${isLoginTab ? 'active' : ''}`}
            style={{ flex: 1, textAlign: 'center' }}
          >
            Sign In
          </button>
          <button
            role="tab"
            aria-selected={!isLoginTab}
            onClick={() => { setIsLoginTab(false); setErrorMessage(null); }}
            className={`tab-btn ${!isLoginTab ? 'active' : ''}`}
            style={{ flex: 1, textAlign: 'center' }}
          >
            Create Account
          </button>
        </div>

        {errorMessage && (
          <div
            role="alert"
            aria-live="polite"
            style={{
              background: 'rgba(239, 68, 68, 0.12)',
              border: '1px solid rgba(239, 68, 68, 0.3)',
              borderRadius: 'var(--radius-md)',
              padding: '12px 14px',
              color: '#f87171',
              fontSize: '0.88rem',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              marginBottom: '20px',
            }}
          >
            <AlertCircle size={16} />
            <span>{errorMessage}</span>
          </div>
        )}

        {isLoginTab ? (
          /* Login Form */
          <form onSubmit={handleLoginSubmit}>
            <div className="form-group">
              <label className="form-label" htmlFor="login-email">Email Address</label>
              <div style={{ position: 'relative' }}>
                <Mail size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
                <input
                  id="login-email"
                  type="email"
                  required
                  placeholder="name@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="input"
                  style={{ paddingLeft: '38px' }}
                />
              </div>
            </div>

            <div className="form-group" style={{ marginBottom: '24px' }}>
              <label className="form-label" htmlFor="login-password">Password</label>
              <div style={{ position: 'relative' }}>
                <Lock size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
                <input
                  id="login-password"
                  type="password"
                  required
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="input"
                  style={{ paddingLeft: '38px' }}
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="btn btn-primary"
              style={{ width: '100%', padding: '12px', fontSize: '1rem' }}
            >
              {loading ? 'Authenticating...' : 'Sign In'}
            </button>
          </form>
        ) : (
          /* Registration Form */
          <form onSubmit={handleRegisterSubmit}>
            <div className="form-row" style={{ marginBottom: '16px' }}>
              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label" htmlFor="reg-first-name">First Name</label>
                <input
                  id="reg-first-name"
                  type="text"
                  required
                  placeholder="John"
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                  className="input"
                />
              </div>
              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label" htmlFor="reg-last-name">Last Name</label>
                <input
                  id="reg-last-name"
                  type="text"
                  required
                  placeholder="Doe"
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                  className="input"
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="reg-email">Email Address</label>
              <input
                id="reg-email"
                type="email"
                required
                placeholder="john.doe@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="input"
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="reg-phone">Phone Number (Optional)</label>
              <input
                id="reg-phone"
                type="tel"
                placeholder="+91 9876543210"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                className="input"
              />
            </div>

            <div className="form-group" style={{ marginBottom: '24px' }}>
              <label className="form-label" htmlFor="reg-password">Password</label>
              <input
                id="reg-password"
                type="password"
                required
                placeholder="At least 8 characters"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="input"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="btn btn-primary"
              style={{ width: '100%', padding: '12px', fontSize: '1rem' }}
            >
              {loading ? 'Creating Account...' : 'Register as Customer'}
            </button>
          </form>
        )}

        {/* Development Seed Accounts Helper */}
        <div style={{
          marginTop: '28px',
          paddingTop: '20px',
          borderTop: '1px solid var(--border-subtle)',
          textAlign: 'center',
        }}>
          <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>TEST QUICK-LOGIN CREDENTIALS</span>
          <div style={{ display: 'flex', gap: '8px', justifyContent: 'center', marginTop: '10px' }}>
            <button
              type="button"
              onClick={() => fillTestCredentials('ADMIN')}
              className="btn btn-sm btn-outline"
              style={{ fontSize: '0.75rem', padding: '4px 10px' }}
            >
              Admin
            </button>
            <button
              type="button"
              onClick={() => fillTestCredentials('MANAGER')}
              className="btn btn-sm btn-outline"
              style={{ fontSize: '0.75rem', padding: '4px 10px' }}
            >
              Manager
            </button>
            <button
              type="button"
              onClick={() => fillTestCredentials('CUSTOMER')}
              className="btn btn-sm btn-outline"
              style={{ fontSize: '0.75rem', padding: '4px 10px' }}
            >
              Customer
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
