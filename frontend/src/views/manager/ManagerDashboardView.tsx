import React, { useState, useEffect } from 'react';
import { Sliders, Tv, Calendar, ShieldCheck, ShieldAlert, MapPin, Plus } from 'lucide-react';
import { managerApi, theatresApi } from '../../api/client';
import type { Theatre, Screen } from '../../types/theatre';
import type { Show } from '../../types/show';
import { useAuth } from '../../context/AuthContext';

interface ManagerDashboardViewProps {
  onNavigate: (view: string, param?: any) => void;
}

export const ManagerDashboardView: React.FC<ManagerDashboardViewProps> = ({ onNavigate }) => {
  const { user, isAdmin } = useAuth();
  const [theatres, setTheatres] = useState<Theatre[]>([]);
  const [selectedTheatreId, setSelectedTheatreId] = useState<number | null>(null);
  const [screens, setScreens] = useState<Screen[]>([]);
  const [shows, setShows] = useState<Show[]>([]);
  const [loading, setLoading] = useState(true);
  const [accessError, setAccessError] = useState<string | null>(null);

  // Load accessible theatres based on backend authorization
  useEffect(() => {
    theatresApi.getTheatres()
      .then(async (data) => {
        if (isAdmin) {
          // Super Admin has platform-wide authority
          setTheatres(data);
          if (data.length > 0) {
            setSelectedTheatreId(data[0].theatreId);
          }
          return;
        }

        // For Theatre Manager: dynamically verify authorized multiplexes against backend security
        const authorized: Theatre[] = [];
        for (const t of data) {
          try {
            await managerApi.getScreens(t.theatreId);
            authorized.push(t);
          } catch {
            // 403 Forbidden: not in this manager's assigned scope
          }
        }
        setTheatres(authorized);
        if (authorized.length > 0) {
          setSelectedTheatreId(authorized[0].theatreId);
        } else if (data.length > 0) {
          setSelectedTheatreId(data[0].theatreId);
        }
      })
      .catch((err) => console.error('Failed to load theatres for manager', err));
  }, [isAdmin]);

  // Load theatre metrics when selectedTheatreId changes
  useEffect(() => {
    if (!selectedTheatreId) return;
    const fetchTheatreData = async () => {
      try {
        setLoading(true);
        setAccessError(null);
        const [screensData, showsData] = await Promise.all([
          managerApi.getScreens(selectedTheatreId),
          managerApi.getShows(selectedTheatreId),
        ]);
        setScreens(screensData || []);
        setShows(showsData || []);
      } catch (err: any) {
        console.error('Failed to load manager dashboard data', err);
        setAccessError(err.message || 'Access denied for this theatre (403 Forbidden). Scope is strictly restricted to your assigned multiplex.');
        setScreens([]);
        setShows([]);
      } finally {
        setLoading(false);
      }
    };
    fetchTheatreData();
  }, [selectedTheatreId]);

  if (loading || !selectedTheatreId) {
    return (
      <div style={{ textAlign: 'center', padding: '80px 20px', color: 'var(--text-secondary)' }}>
        Loading manager operational dashboard...
      </div>
    );
  }

  const currentTheatre = theatres.find((t) => t.theatreId === selectedTheatreId);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }} data-testid="manager-dashboard-view">
      {/* Access Error Banner if 403 Forbidden */}
      {accessError && (
        <div className="card" style={{ borderLeft: '4px solid #dc2626', background: 'rgba(220,38,38,0.08)', padding: '16px 20px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', color: '#dc2626', fontWeight: 600 }}>
            <ShieldAlert size={20} />
            <span>Security Boundary Violation (403 Forbidden)</span>
          </div>
          <p style={{ marginTop: '6px', fontSize: '0.9rem', color: 'var(--text-secondary)' }}>
            {accessError}
          </p>
        </div>
      )}

      {/* Portal Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <div className="badge badge-gold" style={{ marginBottom: '8px', gap: '6px' }}>
            <Sliders size={14} />
            <span>Theatre Operations Portal</span>
          </div>
          <h1 style={{ fontSize: '2.2rem', marginBottom: '6px' }}>
            Manager Dashboard {user ? `(${user.firstName} ${user.lastName})` : ''}
          </h1>
          <p>Supervise auditoriums, screen layouts, seating statuses, and scheduled screenings</p>
        </div>

        {/* Theatre Scope Switcher */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <MapPin size={18} color="var(--accent-crimson)" />
          {isAdmin || theatres.length > 1 ? (
            <select
              value={selectedTheatreId ?? ''}
              onChange={(e) => setSelectedTheatreId(parseInt(e.target.value, 10))}
              className="select"
              aria-label="Select Assigned Theatre"
              style={{ minWidth: '220px', fontWeight: 600 }}
            >
              {theatres.map((t) => (
                <option key={t.theatreId} value={t.theatreId}>
                  {t.theatreName}
                </option>
              ))}
            </select>
          ) : (
            <div style={{
              padding: '8px 16px',
              borderRadius: 'var(--radius-md)',
              background: 'var(--bg-elevated)',
              border: '1px solid var(--border-subtle)',
              fontWeight: 600,
              fontSize: '0.95rem',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
            }}>
              <span>{currentTheatre ? currentTheatre.theatreName : 'PVK INOX Luxe Chennai'}</span>
              <span className="badge badge-gold" style={{ fontSize: '0.68rem', padding: '2px 6px' }}>Assigned</span>
            </div>
          )}
        </div>
      </div>

      {/* Metrics Row */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
        gap: '20px',
      }}>
        <div className="card" style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <div style={{ background: 'rgba(245, 197, 24, 0.15)', color: 'var(--accent-gold)', padding: '14px', borderRadius: 'var(--radius-md)' }}>
            <Tv size={24} />
          </div>
          <div>
            <div style={{ fontSize: '1.8rem', fontWeight: 800 }}>{screens.length}</div>
            <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>Auditorium Screens</div>
          </div>
        </div>

        <div className="card" style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <div style={{ background: 'var(--accent-crimson-glow)', color: 'var(--accent-crimson)', padding: '14px', borderRadius: 'var(--radius-md)' }}>
            <Calendar size={24} />
          </div>
          <div>
            <div style={{ fontSize: '1.8rem', fontWeight: 800 }}>{shows.length}</div>
            <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>Scheduled Screenings</div>
          </div>
        </div>

        <div className="card" style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <div style={{ background: 'rgba(16, 185, 129, 0.15)', color: '#10B981', padding: '14px', borderRadius: 'var(--radius-md)' }}>
            <ShieldCheck size={24} />
          </div>
          <div>
            <div style={{ fontSize: '1.8rem', fontWeight: 800 }}>Active</div>
            <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>Operating Status</div>
          </div>
        </div>
      </div>

      {/* Quick Access Operational Cards */}
      <section>
        <h2 style={{ marginBottom: '16px', fontSize: '1.4rem' }}>Operational Management Tools</h2>
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
          gap: '20px',
        }}>
          <div
            className="card card-interactive"
            onClick={() => onNavigate('manager-screens', selectedTheatreId)}
            style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <Tv size={20} color="var(--accent-gold)" />
              <h3 style={{ fontSize: '1.2rem' }}>Screen Management</h3>
            </div>
            <p style={{ fontSize: '0.9rem' }}>
              Configure screen inventory, auditorium numbers, audio capabilities, and screen types (IMAX, 4DX, Standard).
            </p>
            <button className="btn btn-sm btn-outline" style={{ marginTop: 'auto', alignSelf: 'flex-start' }}>
              Manage Screens &rarr;
            </button>
          </div>

          <div
            className="card card-interactive"
            onClick={() => onNavigate('manager-seats', selectedTheatreId)}
            style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <ShieldCheck size={20} color="#10B981" />
              <h3 style={{ fontSize: '1.2rem' }}>Seat Management &amp; Blocking</h3>
            </div>
            <p style={{ fontSize: '0.9rem' }}>
              Inspect screen seat configurations and execute administrative seat blocking or status overrides.
            </p>
            <button className="btn btn-sm btn-outline" style={{ marginTop: 'auto', alignSelf: 'flex-start' }}>
              Manage Seats &rarr;
            </button>
          </div>

          <div
            className="card card-interactive"
            onClick={() => onNavigate('manager-shows', selectedTheatreId)}
            style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <Calendar size={20} color="var(--accent-crimson)" />
              <h3 style={{ fontSize: '1.2rem' }}>Show Scheduler</h3>
            </div>
            <p style={{ fontSize: '0.9rem' }}>
              Schedule new screenings with automated BR-004 conflict overlap validation and manage existing showtimes.
            </p>
            <button className="btn btn-sm btn-outline" style={{ marginTop: 'auto', alignSelf: 'flex-start' }}>
              Manage Shows &rarr;
            </button>
          </div>
        </div>
      </section>

      {/* Shows Overview Table */}
      <section>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <div>
            <h2 style={{ fontSize: '1.4rem' }}>Scheduled Screenings</h2>
            <p>Active and scheduled shows at {currentTheatre?.theatreName || 'this multiplex'}</p>
          </div>
          <button
            onClick={() => onNavigate('manager-shows', selectedTheatreId)}
            className="btn btn-sm btn-primary"
            style={{ gap: '6px' }}
          >
            <Plus size={14} />
            Schedule Show
          </button>
        </div>

        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Show ID</th>
                <th>Movie Title</th>
                <th>Auditorium</th>
                <th>Start Time</th>
                <th>End Time</th>
                <th>Status</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {shows.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '32px', color: 'var(--text-muted)' }}>
                    No shows currently scheduled for this theatre.
                  </td>
                </tr>
              ) : (
                shows.map((s) => (
                  <tr key={s.showId}>
                    <td><code>#{s.showId}</code></td>
                    <td style={{ fontWeight: 600 }}>{s.movieTitle}</td>
                    <td>{s.screenName}</td>
                    <td>{new Date(s.startAt).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' })}</td>
                    <td>{new Date(s.endAt).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' })}</td>
                    <td>
                      <span className={`badge ${s.showStatus === 'SCHEDULED' ? 'badge-emerald' : 'badge-slate'}`}>
                        {s.showStatus}
                      </span>
                    </td>
                    <td>
                      <button
                        onClick={() => onNavigate('show-seats', s.showId)}
                        className="btn btn-sm btn-secondary"
                        style={{ padding: '4px 10px', fontSize: '0.8rem' }}
                      >
                        Inspect Seats
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
};
