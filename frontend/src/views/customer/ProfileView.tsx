import React, { useState, useEffect } from 'react';
import { Mail, Phone, MapPin, Check, LogOut, Ticket, Calendar, Tv, ArrowRight } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { usersApi, theatresApi, bookingApi } from '../../api/client';
import type { City } from '../../types/theatre';
import type { BookingResponse } from '../../types/booking';
import { useToast } from '../../context/ToastContext';

interface ProfileViewProps {
  onNavigate: (view: string, param?: any) => void;
}

export const ProfileView: React.FC<ProfileViewProps> = ({ onNavigate }) => {
  const { user, logout, refreshUser } = useAuth();
  const { showToast } = useToast();
  const [activeTab, setActiveTab] = useState<'profile' | 'bookings'>('profile');
  const [cities, setCities] = useState<City[]>([]);
  const [firstName, setFirstName] = useState(user?.firstName || '');
  const [lastName, setLastName] = useState(user?.lastName || '');
  const [phone, setPhone] = useState(user?.phone || '');
  const [preferredCityId, setPreferredCityId] = useState<number | undefined>(undefined);
  const [saving, setSaving] = useState(false);

  // Bookings state
  const [bookings, setBookings] = useState<BookingResponse[]>([]);
  const [loadingBookings, setLoadingBookings] = useState(false);

  useEffect(() => {
    theatresApi.getCities()
      .then((data) => setCities(data))
      .catch((err) => console.error('Failed to load cities', err));

    usersApi.getProfile()
      .then((data) => {
        setFirstName(data.firstName || '');
        setLastName(data.lastName || '');
        setPhone(data.phone || '');
        setPreferredCityId(data.preferredCityId);
      })
      .catch((err) => console.error('Failed to load profile details', err));

    // Load customer's bookings
    setLoadingBookings(true);
    bookingApi.getMyBookings()
      .then((data) => setBookings(data || []))
      .catch((err) => console.warn('Could not load user bookings', err))
      .finally(() => setLoadingBookings(false));
  }, []);

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSaving(true);
      await usersApi.updateProfile({
        firstName,
        lastName,
        phone,
      });
      await refreshUser();
      showToast('Profile updated successfully!', 'success');
    } catch (err: any) {
      console.error('Failed to update profile', err);
      showToast(err.message || 'Failed to update profile', 'error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div style={{ maxWidth: '780px', margin: '20px auto', display: 'flex', flexDirection: 'column', gap: '28px' }} data-testid="profile-view">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1 style={{ marginBottom: '6px' }}>My Account</h1>
          <p>Manage personal details, viewing preferences, and cinema booking history</p>
        </div>

        {/* Tab Switcher */}
        <div style={{ display: 'flex', background: 'var(--bg-elevated)', borderRadius: 'var(--radius-md)', padding: '4px', border: '1px solid var(--border-subtle)' }}>
          <button
            type="button"
            onClick={() => setActiveTab('profile')}
            className={`btn btn-sm ${activeTab === 'profile' ? 'btn-primary' : 'btn-outline'}`}
            style={{ border: 'none' }}
          >
            Profile Details
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('bookings')}
            className={`btn btn-sm ${activeTab === 'bookings' ? 'btn-primary' : 'btn-outline'}`}
            style={{ border: 'none', gap: '6px' }}
          >
            <Ticket size={14} />
            My Bookings ({bookings.length})
          </button>
        </div>
      </div>

      {activeTab === 'profile' ? (
        <div className="card" style={{ padding: '32px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '28px', paddingBottom: '20px', borderBottom: '1px solid var(--border-subtle)' }}>
            <div style={{
              background: 'var(--accent-crimson)',
              color: '#ffffff',
              borderRadius: '50%',
              width: '56px',
              height: '56px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '1.4rem',
              fontWeight: 800,
            }}>
              {user?.firstName?.[0] || 'U'}
            </div>
            <div>
              <h3 style={{ fontSize: '1.25rem', marginBottom: '4px' }}>
                {user?.firstName} {user?.lastName}
              </h3>
              <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
                {(user?.roles || [user?.role || 'CUSTOMER']).map((r) => (
                  <span key={r} className="badge badge-gold">
                    {r.replace('ROLE_', '')}
                  </span>
                ))}
              </div>
            </div>
          </div>

          <form onSubmit={handleUpdate} style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
            <div className="form-row">
              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label" htmlFor="prof-first-name">First Name</label>
                <input
                  id="prof-first-name"
                  type="text"
                  required
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                  className="input"
                />
              </div>

              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label" htmlFor="prof-last-name">Last Name</label>
                <input
                  id="prof-last-name"
                  type="text"
                  required
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                  className="input"
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="prof-email">Email Address (Read-Only)</label>
              <div style={{ position: 'relative' }}>
                <Mail size={16} aria-hidden="true" style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
                <input
                  id="prof-email"
                  type="email"
                  disabled
                  value={user?.email || ''}
                  className="input"
                  style={{ paddingLeft: '38px', opacity: 0.6, cursor: 'not-allowed' }}
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="prof-phone">Phone Number</label>
              <div style={{ position: 'relative' }}>
                <Phone size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
                <input
                  id="prof-phone"
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="+91 9876543210"
                  className="input"
                  style={{ paddingLeft: '38px' }}
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="prof-preferred-city">Preferred Cinema City</label>
              <div style={{ position: 'relative' }}>
                <MapPin size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
                <select
                  id="prof-preferred-city"
                  value={preferredCityId || ''}
                  onChange={(e) => setPreferredCityId(e.target.value ? parseInt(e.target.value, 10) : undefined)}
                  className="select"
                  style={{ paddingLeft: '38px' }}
                >
                  <option value="">No City Selected</option>
                  {cities.map((city) => (
                    <option key={city.cityId} value={city.cityId}>
                      {city.cityName} ({city.state})
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '12px' }}>
              <button
                type="button"
                onClick={() => { logout(); onNavigate('home'); }}
                className="btn btn-danger btn-sm"
                style={{ gap: '6px' }}
              >
                <LogOut size={14} />
                Sign Out
              </button>

              <button
                type="submit"
                disabled={saving}
                className="btn btn-primary"
                style={{ gap: '6px' }}
              >
                <Check size={16} />
                {saving ? 'Saving...' : 'Save Preferences'}
              </button>
            </div>
          </form>
        </div>
      ) : (
        /* My Bookings Tab */
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {loadingBookings ? (
            <div style={{ textAlign: 'center', padding: '40px', color: 'var(--text-secondary)' }}>
              Loading your bookings...
            </div>
          ) : bookings.length === 0 ? (
            <div className="card" style={{ textAlign: 'center', padding: '48px 20px' }}>
              <Ticket size={40} color="var(--text-muted)" style={{ margin: '0 auto 12px auto' }} />
              <h3>No Bookings Found</h3>
              <p style={{ marginTop: '6px', color: 'var(--text-secondary)' }}>
                You haven't reserved any cinema tickets yet. Explore movies and choose your seats!
              </p>
              <button
                onClick={() => onNavigate('movies')}
                className="btn btn-primary btn-sm"
                style={{ marginTop: '20px' }}
              >
                Browse Movies
              </button>
            </div>
          ) : (
            bookings.map((b) => (
              <div
                key={b.bookingId}
                className="card"
                style={{
                  padding: '24px',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  flexWrap: 'wrap',
                  gap: '20px',
                  borderLeft: '4px solid var(--accent-crimson)',
                }}
              >
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                    <span className="badge badge-emerald">{b.bookingStatus}</span>
                    <span style={{ fontFamily: 'monospace', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                      {b.bookingReference}
                    </span>
                  </div>
                  <h3 style={{ fontSize: '1.4rem', margin: '4px 0 8px 0' }}>{b.movieTitle}</h3>
                  <div style={{ display: 'flex', gap: '14px', flexWrap: 'wrap', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                      <MapPin size={13} color="var(--accent-crimson)" />
                      <span>{b.theatreName} ({b.cityName})</span>
                    </div>
                    <span>&bull;</span>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                      <Tv size={13} color="var(--accent-gold)" />
                      <span>{b.screenName}</span>
                    </div>
                    <span>&bull;</span>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                      <Calendar size={13} color="var(--accent-crimson)" />
                      <span>{b.showDate} {b.showTime}</span>
                    </div>
                  </div>
                  <div style={{ marginTop: '10px', display: 'flex', gap: '6px', alignItems: 'center' }}>
                    <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Seats:</span>
                    {b.seats.map((s) => (
                      <span key={s.seatId} className="badge badge-crimson" style={{ fontWeight: 700 }}>
                        {s.rowLabel}{s.seatNumber}
                      </span>
                    ))}
                  </div>
                </div>

                <div style={{ textAlign: 'right', display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '10px' }}>
                  <div style={{ fontSize: '1.3rem', fontWeight: 900, color: 'var(--accent-gold)' }}>
                    ₹{b.totalAmount.toFixed(2)}
                  </div>
                  <button
                    type="button"
                    onClick={() => onNavigate('booking-confirmation', b.bookingReference)}
                    className="btn btn-sm btn-primary"
                    style={{ gap: '6px' }}
                  >
                    View E-Ticket
                    <ArrowRight size={14} />
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
};
