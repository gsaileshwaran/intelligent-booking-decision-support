import React, { useState, useEffect, useRef } from 'react';
import { Search, Film, MapPin, User as UserIcon, Shield, Sliders, LogOut, Menu, X } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { theatresApi } from '../../api/client';
import type { City } from '../../types/theatre';

interface HeaderProps {
  onNavigate: (view: string, param?: any) => void;
  currentView: string;
}

export const Header: React.FC<HeaderProps> = ({ onNavigate, currentView }) => {
  const { user, isAuthenticated, isManager, isAdmin, logout, selectedCityId, setSelectedCityId } = useAuth();
  const [cities, setCities] = useState<City[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [showUserMenu, setShowUserMenu] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const drawerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    theatresApi.getCities()
      .then((data) => setCities(data))
      .catch((err) => console.error('Failed to load cities', err));
  }, []);

  // Close mobile menu on outside click
  useEffect(() => {
    const handleClick = (e: MouseEvent) => {
      if (drawerRef.current && !drawerRef.current.contains(e.target as Node)) {
        setMobileMenuOpen(false);
      }
    };
    if (mobileMenuOpen) document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, [mobileMenuOpen]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      onNavigate('search', searchQuery.trim());
      setMobileMenuOpen(false);
    }
  };

  const navTo = (view: string, param?: any) => {
    onNavigate(view, param);
    setMobileMenuOpen(false);
    setShowUserMenu(false);
  };

  return (
    <header style={{
      height: 'var(--header-height)',
      background: 'rgba(10, 13, 20, 0.92)',
      backdropFilter: 'blur(16px)',
      WebkitBackdropFilter: 'blur(16px)',
      borderBottom: '1px solid var(--border-subtle)',
      position: 'sticky',
      top: 0,
      zIndex: 100,
    }}>
      <div className="app-container" style={{
        height: '100%',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: '24px',
      }}>
        {/* Brand Logo */}
        <button 
          onClick={() => navTo('home')} 
          className="brand-btn"
          aria-label="PVK Cinemas Home"
        >
          <div style={{
            background: 'var(--accent-crimson)',
            color: '#fff',
            borderRadius: '8px',
            padding: '6px 10px',
            fontWeight: 900,
            fontSize: '1.2rem',
            letterSpacing: '1px',
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            boxShadow: '0 2px 10px var(--accent-crimson-glow)',
          }}>
            <Film size={20} aria-hidden="true" />
            PVK
          </div>
          <span style={{
            fontFamily: 'var(--font-display)',
            fontSize: '1.3rem',
            fontWeight: 800,
            letterSpacing: '-0.02em',
            color: '#ffffff',
          }}>
            CINEMAS
          </span>
        </button>

        {/* Desktop: Global Search Bar */}
        <form 
          onSubmit={handleSearchSubmit} 
          className="header-search-desktop"
          style={{ flex: 1, maxWidth: '460px', position: 'relative' }}
          role="search"
        >
          <Search size={18} style={{
            position: 'absolute',
            left: '14px',
            top: '50%',
            transform: 'translateY(-50%)',
            color: 'var(--text-muted)',
            pointerEvents: 'none',
          }} />
          <input
            type="search"
            aria-label="Search movies, theatres, and genres"
            placeholder="Search movies, multiplexes, genres, languages..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="input"
            style={{
              paddingLeft: '40px',
              paddingRight: '14px',
              background: 'var(--bg-elevated)',
              border: '1px solid var(--border-subtle)',
              height: '42px',
              borderRadius: 'var(--radius-full)',
              fontSize: '0.9rem',
            }}
          />
        </form>

        {/* Desktop: City Selector */}
        <div className="header-city-desktop" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <MapPin size={16} color="var(--accent-crimson)" />
          <select
            value={selectedCityId || ''}
            onChange={(e) => setSelectedCityId(e.target.value ? parseInt(e.target.value, 10) : null)}
            aria-label="Select City"
            className="select"
            style={{
              padding: '6px 12px',
              width: 'auto',
              background: 'var(--bg-elevated)',
              border: '1px solid var(--border-subtle)',
              fontSize: '0.88rem',
              color: 'var(--text-primary)',
              borderRadius: 'var(--radius-md)',
            }}
          >
            <option value="">All Cities</option>
            {cities.map((city) => (
              <option key={city.cityId} value={city.cityId}>
                {city.cityName}
              </option>
            ))}
          </select>
        </div>

        {/* Desktop: Main Navigation Links */}
        <nav className="header-nav-desktop" style={{ display: 'flex', alignItems: 'center', gap: '16px' }} aria-label="Main Navigation">
          <button
            onClick={() => navTo('movies')}
            className={`btn btn-sm ${currentView === 'movies' ? 'btn-secondary' : 'btn-outline'}`}
            style={{ border: 'none' }}
            aria-current={currentView === 'movies' ? 'page' : undefined}
          >
            Movies
          </button>
          <button
            onClick={() => navTo('theatres')}
            className={`btn btn-sm ${currentView === 'theatres' ? 'btn-secondary' : 'btn-outline'}`}
            style={{ border: 'none' }}
            aria-current={currentView === 'theatres' ? 'page' : undefined}
          >
            Theatres
          </button>

          {isManager && (
            <button
              onClick={() => navTo('manager-dashboard')}
              className={`btn btn-sm ${currentView.startsWith('manager') ? 'btn-primary' : 'btn-secondary'}`}
              style={{ gap: '6px' }}
            >
              <Sliders size={15} />
              Manager
            </button>
          )}

          {isAdmin && (
            <button
              onClick={() => navTo('admin-dashboard')}
              className={`btn btn-sm ${currentView.startsWith('admin') ? 'btn-primary' : 'btn-secondary'}`}
              style={{ gap: '6px' }}
            >
              <Shield size={15} />
              Admin
            </button>
          )}

          {/* Auth Menu */}
          {isAuthenticated && user ? (
            <div style={{ position: 'relative' }}>
              <button
                onClick={() => setShowUserMenu(!showUserMenu)}
                className="btn btn-secondary btn-sm"
                style={{ borderRadius: 'var(--radius-full)', padding: '6px 14px', gap: '8px' }}
                aria-expanded={showUserMenu}
                aria-haspopup="true"
              >
                <UserIcon size={16} color="var(--accent-gold)" />
                <span>{user.firstName}</span>
              </button>

              {showUserMenu && (
                <div style={{
                  position: 'absolute',
                  right: 0,
                  top: '110%',
                  background: 'var(--bg-elevated)',
                  border: '1px solid var(--border-medium)',
                  borderRadius: 'var(--radius-md)',
                  boxShadow: 'var(--shadow-lg)',
                  width: '200px',
                  padding: '8px 0',
                  zIndex: 1000,
                }}>
                  <div style={{ padding: '8px 16px', borderBottom: '1px solid var(--border-subtle)' }}>
                    <div style={{ fontWeight: 600, color: 'var(--text-primary)', fontSize: '0.9rem' }}>
                      {user.firstName} {user.lastName}
                    </div>
                    <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>{user.email}</div>
                  </div>
                  <button
                    onClick={() => { setShowUserMenu(false); navTo('profile'); }}
                    style={{
                      width: '100%',
                      textAlign: 'left',
                      padding: '10px 16px',
                      background: 'none',
                      border: 'none',
                      color: 'var(--text-primary)',
                      cursor: 'pointer',
                      fontSize: '0.88rem',
                    }}
                  >
                    User Profile
                  </button>
                  <button
                    onClick={() => { setShowUserMenu(false); logout(); navTo('home'); }}
                    style={{
                      width: '100%',
                      textAlign: 'left',
                      padding: '10px 16px',
                      background: 'none',
                      border: 'none',
                      color: '#ef4444',
                      cursor: 'pointer',
                      fontSize: '0.88rem',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                    }}
                  >
                    <LogOut size={14} />
                    Sign Out
                  </button>
                </div>
              )}
            </div>
          ) : (
            <button
              onClick={() => navTo('login')}
              className="btn btn-primary btn-sm"
              style={{ borderRadius: 'var(--radius-full)', padding: '8px 18px' }}
            >
              Sign In
            </button>
          )}
        </nav>

        {/* Mobile: Hamburger Toggle Button */}
        <button
          className="hamburger-btn"
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          aria-label={mobileMenuOpen ? 'Close menu' : 'Open menu'}
          aria-expanded={mobileMenuOpen}
          style={{
            display: 'none',
            background: 'none',
            border: '1px solid var(--border-subtle)',
            borderRadius: 'var(--radius-md)',
            color: 'var(--text-primary)',
            padding: '8px',
            cursor: 'pointer',
          }}
        >
          {mobileMenuOpen ? <X size={22} /> : <Menu size={22} />}
        </button>
      </div>

      {/* Mobile Drawer Overlay */}
      {mobileMenuOpen && (
        <div style={{
          position: 'fixed',
          inset: 0,
          background: 'rgba(0,0,0,0.6)',
          zIndex: 200,
          backdropFilter: 'blur(4px)',
        }} onClick={() => setMobileMenuOpen(false)} aria-hidden="true" />
      )}

      {/* Mobile Drawer */}
      <div
        ref={drawerRef}
        aria-label="Mobile Navigation"
        style={{
          position: 'fixed',
          top: 0,
          right: 0,
          bottom: 0,
          width: '280px',
          background: 'var(--bg-elevated)',
          borderLeft: '1px solid var(--border-medium)',
          zIndex: 201,
          padding: '24px 20px',
          display: 'flex',
          flexDirection: 'column',
          gap: '16px',
          overflowY: 'auto',
          transform: mobileMenuOpen ? 'translateX(0)' : 'translateX(100%)',
          transition: 'transform 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
        }}
      >
        {/* Drawer header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
          <span style={{ fontWeight: 700, fontSize: '1rem', color: 'var(--accent-crimson)' }}>Menu</span>
          <button
            onClick={() => setMobileMenuOpen(false)}
            style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}
            aria-label="Close menu"
          >
            <X size={20} />
          </button>
        </div>

        {/* Mobile Search */}
        <form onSubmit={handleSearchSubmit} role="search" style={{ position: 'relative' }}>
          <Search size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)', pointerEvents: 'none' }} />
          <input
            type="text"
            inputMode="search"
            aria-label="Mobile search"
            placeholder="Search movies, theatres..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="input"
            style={{ paddingLeft: '36px', width: '100%', boxSizing: 'border-box' }}
          />
        </form>

        {/* Mobile City Selector */}
        <div>
          <label style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '6px', display: 'block' }}>Select City</label>
          <select
            value={selectedCityId || ''}
            onChange={(e) => setSelectedCityId(e.target.value ? parseInt(e.target.value, 10) : null)}
            className="select"
            style={{ width: '100%' }}
          >
            <option value="">All Cities</option>
            {cities.map((city) => (
              <option key={city.cityId} value={city.cityId}>{city.cityName}</option>
            ))}
          </select>
        </div>

        {/* Divider */}
        <div style={{ height: '1px', background: 'var(--border-subtle)' }} />

        {/* Mobile Nav Links */}
        <button onClick={() => navTo('movies')} className={`btn ${currentView === 'movies' ? 'btn-primary' : 'btn-outline'}`} style={{ width: '100%', justifyContent: 'flex-start' }}>
          Movies
        </button>
        <button onClick={() => navTo('theatres')} className={`btn ${currentView === 'theatres' ? 'btn-primary' : 'btn-outline'}`} style={{ width: '100%', justifyContent: 'flex-start' }}>
          Theatres
        </button>

        {isManager && (
          <button onClick={() => navTo('manager-dashboard')} className="btn btn-secondary" style={{ width: '100%', justifyContent: 'flex-start', gap: '8px' }}>
            <Sliders size={16} /> Manager Portal
          </button>
        )}
        {isAdmin && (
          <button onClick={() => navTo('admin-dashboard')} className="btn btn-secondary" style={{ width: '100%', justifyContent: 'flex-start', gap: '8px' }}>
            <Shield size={16} /> Admin Portal
          </button>
        )}

        <div style={{ height: '1px', background: 'var(--border-subtle)' }} />

        {/* Mobile Auth */}
        {isAuthenticated && user ? (
          <>
            <div style={{ padding: '10px 12px', background: 'var(--bg-card)', borderRadius: 'var(--radius-md)' }}>
              <div style={{ fontWeight: 600 }}>{user.firstName} {user.lastName}</div>
              <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>{user.email}</div>
            </div>
            <button onClick={() => navTo('profile')} className="btn btn-outline" style={{ width: '100%', justifyContent: 'flex-start', gap: '8px' }}>
              <UserIcon size={16} /> My Profile
            </button>
            <button onClick={() => { logout(); navTo('home'); }} className="btn btn-outline" style={{ width: '100%', justifyContent: 'flex-start', gap: '8px', color: '#ef4444', borderColor: '#ef4444' }}>
              <LogOut size={16} /> Sign Out
            </button>
          </>
        ) : (
          <button onClick={() => navTo('login')} className="btn btn-primary" style={{ width: '100%' }}>
            Sign In
          </button>
        )}
      </div>

      <style>{`
        @media (max-width: 768px) {
          .header-search-desktop,
          .header-city-desktop,
          .header-nav-desktop {
            display: none !important;
          }
          .hamburger-btn {
            display: flex !important;
          }
        }
      `}</style>
    </header>
  );
};
